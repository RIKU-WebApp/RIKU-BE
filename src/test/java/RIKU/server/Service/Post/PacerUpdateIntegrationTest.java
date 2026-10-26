package RIKU.server.Service.Post;

import RIKU.server.Dto.Post.Request.UpdatePacerRequest;
import RIKU.server.Dto.Post.Request.UpdatePostRequest;
import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.Board.Pacer;
import RIKU.server.Entity.Board.Post.*;
import RIKU.server.Entity.Participant.Participant;
import RIKU.server.Entity.Participant.ParticipantStatus;
import RIKU.server.Entity.User.User;
import RIKU.server.Repository.*;
import RIKU.server.Security.AuthMember;
import RIKU.server.Service.ParticipantService;
import RIKU.server.Service.S3Uploader;
import RIKU.server.Util.Exception.Domain.PostException;
import RIKU.server.Util.Exception.Domain.UserException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({PostService.class, ParticipantService.class})
class PacerUpdateIntegrationTest {
    @Autowired PostService service;
    @Autowired ParticipantService attendance;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired PacerRepository pacers;
    @Autowired ParticipantRepository participants;
    @Autowired RegularPostRepository regulars;
    @Autowired TrainingPostRepository trainings;
    @Autowired UserPointRepository points;
    @MockBean S3Uploader s3;
    private int sequence;

    @ParameterizedTest
    @EnumSource(value = PostType.class, names = {"REGULAR", "TRAINING"})
    void unchangedPacersKeepAttendanceIdentityAndEarnPointsWhenClosed(PostType type) {
        User creator = user();
        Post post = post(creator, type);
        Pacer pacer = pacers.save(Pacer.create(creator, post, "A", "6:00", "5"));
        Participant participant = participants.save(Participant.createWithGroup(post, creator, "A"));
        attendance.createAttendanceCode(type.name(), post.getId(), auth(creator));
        em.flush(); em.clear();
        Participant before = participants.findById(participant.getId()).orElseThrow();
        LocalDateTime createdAt = before.getCreatedAt();

        update(post, creator, request(creator, "B"));
        em.flush(); em.clear();
        Participant after = participants.findById(participant.getId()).orElseThrow();
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
        assertThat(after.getParticipantStatus()).isEqualTo(ParticipantStatus.ATTENDED);
        assertThat(after.getGroup()).isEqualTo("B");
        assertThat(pacers.findByPost(post)).singleElement().satisfies(p -> {
            assertThat(p.getId()).isEqualTo(pacer.getId());
            assertThat(p.getGroup()).isEqualTo("B");
        });
        attendance.closeRun(type.name(), post.getId(), auth(creator));
        em.flush(); em.clear();
        assertThat(points.findAll()).singleElement().satisfies(p ->
                assertThat(p.getPoint()).isEqualTo(type == PostType.REGULAR ? 10 : 8));
    }

    @Test
    void replacementPreservesRemovedAndPromotedParticipants() {
        User creator = user(), removed = user(), promoted = user();
        Post post = post(creator, PostType.REGULAR);
        pacers.save(Pacer.create(creator, post, "A", "6", "5"));
        pacers.save(Pacer.create(removed, post, "B", "7", "5"));
        participants.save(Participant.createWithGroup(post, creator, "A"));
        Participant old = participants.save(Participant.createWithGroup(post, removed, "B"));
        old.attend();
        Participant next = participants.save(Participant.createWithGroup(post, promoted, "A"));
        next.attend();
        em.flush(); em.clear();
        update(post, creator, request(creator, "A"), request(promoted, "C"));
        em.flush(); em.clear();
        assertThat(participants.findByPost(post)).hasSize(3);
        assertThat(participants.findById(old.getId()).orElseThrow().getParticipantStatus())
                .isEqualTo(ParticipantStatus.ATTENDED);
        Participant after = participants.findById(next.getId()).orElseThrow();
        assertThat(after.getParticipantStatus()).isEqualTo(ParticipantStatus.ATTENDED);
        assertThat(after.getGroup()).isEqualTo("C");
        assertThat(pacers.findByPost(post)).extracting(p -> p.getUser().getId())
                .containsExactlyInAnyOrder(creator.getId(), promoted.getId());
    }

    @Test
    void newPacerStartsPendingAndRepeatedEditDoesNotDuplicateParticipation() {
        User creator = user(), old = user(), added = user();
        Post post = post(creator, PostType.REGULAR);
        pacers.save(Pacer.create(creator, post, "A", "6", "5"));
        pacers.save(Pacer.create(old, post, "B", "7", "5"));
        participants.save(Participant.createWithGroup(post, creator, "A"));
        participants.save(Participant.createWithGroup(post, old, "B"));
        update(post, creator, request(creator, "A"), request(added, "B"));
        update(post, creator, request(creator, "A"), request(added, "C"));
        em.flush(); em.clear();
        assertThat(participants.findByPost(post)).hasSize(3);
        Participant result = participants.findByPostIdAndUserId(post.getId(), added.getId()).orElseThrow();
        assertThat(result.getParticipantStatus()).isEqualTo(ParticipantStatus.PENDING);
        assertThat(result.getGroup()).isEqualTo("C");
    }

    @Test
    void duplicatePacerIsRejectedBeforeAnyImageOrParticipantChanges() {
        User creator = user(), other = user();
        Post post = post(creator, PostType.REGULAR);
        pacers.save(Pacer.create(creator, post, "A", "6", "5"));
        pacers.save(Pacer.create(other, post, "B", "7", "5"));
        Participant attended = participants.save(Participant.createWithGroup(post, creator, "A"));
        attended.attend();
        UpdatePostRequest request = body(request(creator, "A"), request(creator, "B"));
        request.setPostImage(new MockMultipartFile("postImage", "x.png", "image/png", new byte[]{1}));
        assertThatThrownBy(() -> service.updatePost(auth(creator), "regular", post.getId(), request))
                .isInstanceOf(PostException.class);
        assertThat(participants.findById(attended.getId()).orElseThrow().getParticipantStatus())
                .isEqualTo(ParticipantStatus.ATTENDED);
        assertThat(pacers.findByPost(post)).hasSize(2);
        verifyNoInteractions(s3);
    }

    @Test
    void incompleteOrUnauthorizedPacerIsRejected() {
        User creator = user();
        Post post = post(creator, PostType.REGULAR);
        pacers.save(Pacer.create(creator, post, "A", "6", "5"));
        assertThatThrownBy(() -> update(post, creator, new UpdatePacerRequest())).isInstanceOf(PostException.class);
        assertThatThrownBy(() -> update(post, creator, (UpdatePacerRequest) null)).isInstanceOf(PostException.class);
        creator.updatePacer(false);
        assertThatThrownBy(() -> update(post, creator, request(creator, "A"))).isInstanceOf(UserException.class);
        verifyNoInteractions(s3);
    }

    private User user() {
        User u = users.save(User.create("test-" + ++sequence, "unused", "test", "test", "test", null));
        u.updatePacer(true);
        return u;
    }
    private Post post(User creator, PostType type) {
        Post p = posts.save(Post.create(creator, "run", "park", LocalDateTime.now(), "run", null, type));
        if (type == PostType.REGULAR) regulars.save(RegularPost.create(p));
        if (type == PostType.TRAINING) trainings.save(TrainingPost.create(p, "test"));
        return p;
    }
    private AuthMember auth(User user) {
        return AuthMember.of(user.getId(), "test", "unused", List.of(), BaseStatus.ACTIVE);
    }
    private UpdatePacerRequest request(User user, String group) {
        UpdatePacerRequest r = new UpdatePacerRequest();
        r.setPacerId(user.getId()); r.setGroup(group); r.setPace("6:00"); r.setDistance("5");
        return r;
    }
    private UpdatePostRequest body(UpdatePacerRequest... pacers) {
        UpdatePostRequest r = new UpdatePostRequest();
        r.setPacers(Arrays.asList(pacers));
        return r;
    }
    private void update(Post post, User creator, UpdatePacerRequest... pacers) {
        service.updatePost(auth(creator), post.getPostType().name(), post.getId(), body(pacers));
    }
}
