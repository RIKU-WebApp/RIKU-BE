package RIKU.server.domains.run;

import RIKU.server.domains.run.comment.entity.Comment;
import RIKU.server.domains.run.comment.dto.response.ReadCommentsResponse;
import RIKU.server.domains.run.comment.repository.CommentRepository;
import RIKU.server.domains.run.comment.service.CommentQueryService;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.pacer.repository.PacerRepository;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.repository.*;
import RIKU.server.domains.run.post.service.*;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.global.util.DateTimeUtils;
import RIKU.server.infra.storage.S3Uploader;
import RIKU.server.security.AuthMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({PostService.class, CommentQueryService.class, FlashPostService.class, RegularPostService.class,
        TrainingPostService.class, EventPostService.class})
class PostQueryPerformanceTest {
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired ParticipantRepository participants;
    @Autowired CommentRepository comments;
    @Autowired PacerRepository pacers;
    @Autowired FlashPostRepository flash;
    @Autowired RegularPostRepository regular;
    @Autowired TrainingPostRepository training;
    @Autowired EventPostRepository events;
    @Autowired PostService postService;
    @Autowired CommentQueryService commentService;
    @Autowired FlashPostService flashService;
    @Autowired RegularPostService regularService;
    @Autowired TrainingPostService trainingService;
    @Autowired EventPostService eventService;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @MockBean S3Uploader s3;

    User user() { return users.save(User.create(UUID.randomUUID().toString(), "test", "회원", "college", "major", null)); }
    Post post(User u, PostType type, LocalDateTime date) {
        return posts.save(Post.create(u, "title", "location", date, "large body ".repeat(1000), null, type));
    }
    Statistics start() {
        em.flush(); em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear(); return stats;
    }
    LocalDateTime today() { return DateTimeUtils.startOfDayUtc(LocalDate.now(DateTimeUtils.getDefaultZone())); }

    @Test void historyIsLimitedInDatabaseAndCountsAreGrouped() {
        User u = user(); var today = today();
        for (int i = 1; i <= 100; i++) post(u, PostType.FLASH, today.minusDays(i)).updatePostStatus(PostStatus.CLOSED);
        for (int i = 1; i <= 30; i++) {
            var p = post(u, PostType.FLASH, today.plusDays(i));
            if (i == 1) { participants.save(Participant.create(p, u)); participants.save(Participant.create(p, user())); }
        }
        var stats = start();
        var response = postService.getPostsByRunType("flash");
        assertEquals(6, response.getPastRuns().size());
        assertEquals(30, response.getUpcomingRuns().size());
        assertEquals(2, response.getUpcomingRuns().get(0).getParticipants());
        assertEquals(0, response.getUpcomingRuns().get(1).getParticipants());
        assertEquals(today.minusDays(1), response.getPastRuns().get(0).getDate());
        assertEquals(4, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());
        System.out.println("OPTIMIZED post-list: 130 stored, 36 returned, SQL=4, entityLoads=0");
    }

    @Test void emptyListAvoidsEmptyInClause() {
        var stats = start(); var response = postService.getPostsByRunType("flash");
        assertTrue(response.getTodayRuns().isEmpty());
        assertTrue(response.getUpcomingRuns().isEmpty());
        assertTrue(response.getPastRuns().isEmpty());
        assertEquals(3, stats.getPrepareStatementCount());
    }

    @Test void kstBoundariesAndStatusOrderingArePreserved() {
        User u = user(); var today = today();
        var before = post(u, PostType.FLASH, today.minusSeconds(1)); before.updatePostStatus(PostStatus.CLOSED);
        var closedToday = post(u, PostType.FLASH, today); closedToday.updatePostStatus(PostStatus.CLOSED);
        var openToday = post(u, PostType.FLASH, today.plusHours(1));
        var tomorrow = post(u, PostType.FLASH, today.plusDays(1)); tomorrow.updatePostStatus(PostStatus.CANCELED);
        post(u, PostType.FLASH, today.minusDays(2)); // Old NOW is not in past CLOSED cards.
        post(u, PostType.FLASH, today.plusHours(2)).updateStatus(BaseStatus.INACTIVE);
        post(u, PostType.TRAINING, today.plusHours(3));
        start(); var response = postService.getPostsByRunType("flash");
        assertEquals(List.of(openToday.getId(), closedToday.getId()), response.getTodayRuns().stream().map(r -> r.getId()).toList());
        assertEquals(List.of(tomorrow.getId()), response.getUpcomingRuns().stream().map(r -> r.getId()).toList());
        assertEquals(List.of(before.getId()), response.getPastRuns().stream().map(r -> r.getId()).toList());
    }

    @Test void commentsUseOneQueryAndKeepDeletedParentsAndNestedReplies() {
        User u = user(); Post p = post(u, PostType.FLASH, today());
        var root = comments.save(Comment.create(u, p, "deleted parent", null)); root.updateStatus(BaseStatus.INACTIVE);
        var child = comments.save(Comment.create(user(), p, "child", root.getId()));
        comments.save(Comment.create(user(), p, "grandchild", child.getId()));
        Post another = post(u, PostType.FLASH, today());
        comments.save(Comment.create(u, another, "cross-post reply", root.getId()));
        var stats = start(); var response = commentService.getComments(p.getId());
        assertEquals(1, response.size());
        assertEquals(BaseStatus.INACTIVE, response.get(0).getCommentStatus());
        assertEquals(1, response.get(0).getReplies().size());
        assertEquals("grandchild", response.get(0).getReplies().get(0).getReplies().get(0).getContent());
        assertEquals(1, stats.getPrepareStatementCount());
    }

    @ParameterizedTest @EnumSource(PostType.class)
    void allDetailTypesHaveConstantQueries(PostType type) {
        User creator = user(); Post p = post(creator, type, today());
        switch (type) {
            case FLASH -> flash.save(FlashPost.create(p));
            case REGULAR -> regular.save(RegularPost.create(p));
            case TRAINING -> training.save(TrainingPost.create(p, "training"));
            case EVENT -> events.save(EventPost.create(p, "event"));
        }
        for (int i = 0; i < 10; i++) participants.save(Participant.createWithGroup(p, user(), "A"));
        if (type == PostType.REGULAR || type == PostType.TRAINING) pacers.save(Pacer.create(creator, p, "A", "6:00", "5"));
        for (int i = 0; i < 5; i++) {
            var root = comments.save(Comment.create(creator, p, "root", null));
            comments.save(Comment.create(creator, p, "reply", root.getId()));
        }
        var stats = start(); var auth = AuthMember.builder().id(creator.getId()).build();
        Object response = switch (type) {
            case FLASH -> flashService.getPostDetail(p.getId(), auth);
            case REGULAR -> regularService.getPostDetail(p.getId(), auth);
            case TRAINING -> trainingService.getPostDetail(p.getId(), auth);
            case EVENT -> eventService.getPostDetail(p.getId(), auth);
        };
        assertEquals(10, ReflectionTestUtils.getField(response, "participantsNum"));
        @SuppressWarnings("unchecked") var resultComments = (List<ReadCommentsResponse>) ReflectionTestUtils.getField(response, "comments");
        assertEquals(5, resultComments.size());
        assertTrue(resultComments.stream().allMatch(c -> c.getReplies().size() == 1));
        int expected = switch (type) { case FLASH -> 4; case REGULAR, EVENT -> 5; case TRAINING -> 6; };
        assertEquals(expected, stats.getPrepareStatementCount());
        System.out.printf("OPTIMIZED detail %s: participants=10 comments=10 SQL=%d%n", type, stats.getPrepareStatementCount());
    }
}
