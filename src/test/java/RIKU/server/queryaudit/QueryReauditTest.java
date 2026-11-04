package RIKU.server.queryaudit;

import RIKU.server.domains.admin.service.AdminService;
import RIKU.server.domains.calendar.service.CalendarService;
import RIKU.server.domains.point.entity.*;
import RIKU.server.domains.point.ranking.RankingChanged;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.run.home.service.HomeService;
import RIKU.server.domains.run.participant.dto.request.ManualAttendParticipantRequest;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.service.ParticipantService;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.season.SeasonProvider;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.user.service.UserService;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.global.util.DateTimeUtils;
import RIKU.server.infra.storage.S3Uploader;
import RIKU.server.security.AuthMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Current-cost probes: real repositories and flushes, excluding HTTP authentication and async ranking refresh. */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({RIKU.server.domains.run.recruitment.RunWaitlist.class, AdminService.class, ParticipantService.class, UserService.class, HomeService.class,
        CalendarService.class})
class QueryReauditTest {
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired ParticipantRepository participants;
    @Autowired UserPointRepository points;
    @Autowired AdminService admin;
    @Autowired ParticipantService attendance;
    @Autowired UserService profiles;
    @Autowired HomeService home;
    @Autowired CalendarService calendar;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @MockBean SeasonProvider season;
    @MockBean PasswordEncoder encoder;
    @MockBean S3Uploader s3;

    @BeforeEach void season() { when(season.seasonStartUtc()).thenReturn(LocalDateTime.of(2000, 1, 1, 0, 0)); }
    User user() { return users.save(User.create(UUID.randomUUID().toString(), "test", "회원", "대학", "학과", null)); }
    AuthMember auth(User user) { return AuthMember.of(user.getId(), user.getStudentId(), "", AuthorityUtils.createAuthorityList("ROLE_ADMIN"), BaseStatus.ACTIVE); }
    Post post(User user, PostType type) { return posts.save(Post.create(user, "title", "loc", LocalDateTime.now().plusDays(1), "body".repeat(1000), null, type)); }
    Statistics start() {
        em.flush(); em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear(); return stats;
    }
    void result(String name, Statistics stats) {
        em.flush();
        System.out.printf("QUERY_REAUDIT %s sql=%d entities=%d collections=%d inserted=%d updated=%d deleted=%d%n",
                name, stats.getPrepareStatementCount(), stats.getEntityLoadCount(), stats.getCollectionFetchCount(),
                stats.getEntityInsertCount(), stats.getEntityUpdateCount(), stats.getEntityDeleteCount());
    }
    List<ManualAttendParticipantRequest> members(Post post, int count, boolean attended, boolean withPoints) {
        var requests = new ArrayList<ManualAttendParticipantRequest>();
        for (int i = 0; i < count; i++) {
            var member = user(); var participant = Participant.createWithGroup(post, member, "A");
            if (attended) participant.attend(); participants.save(participant);
            if (withPoints) points.save(UserPoint.createWithPost(member, 10, "regular", PointType.ADD_REGULAR_JOIN, post));
            var request = new ManualAttendParticipantRequest();
            ReflectionTestUtils.setField(request, "userId", member.getId());
            ReflectionTestUtils.setField(request, "isAttend", !attended); requests.add(request);
        }
        return requests;
    }

    @Test void administratorListUsesGroupedAggregates() {
        User actor = user(); for (int i = 1; i < 50; i++) user();
        var stats = start(); assertEquals(50, admin.getUsers(auth(actor)).size());
        result("admin_members_50", stats); assertEquals(3, stats.getPrepareStatementCount());
    }

    @Test void manualAttendanceBatchesReadsAndRetainsIndividualUpdates() {
        var actor = user(); var post = post(actor, PostType.REGULAR); var requests = members(post, 20, false, false);
        var stats = start(); attendance.manualAttendRun("regular", post.getId(), auth(actor), requests);
        result("manual_attendance_20", stats); assertEquals(22, stats.getPrepareStatementCount());
    }

    @Test void closingRunStillUsesIndividualIdentityInserts() {
        var actor = user(); var post = post(actor, PostType.REGULAR); members(post, 20, true, false);
        var stats = start(); attendance.closeRun("regular", post.getId(), auth(actor));
        result("close_run_20", stats); assertEquals(23, stats.getPrepareStatementCount());
    }

    @Test void absenceCorrectionReusesDeletedRowsWithoutAnExistenceQuery() {
        var actor = user(); var post = post(actor, PostType.REGULAR); post.updatePostStatus(PostStatus.CLOSED);
        var requests = members(post, 20, true, true);
        var stats = start(); attendance.fixAttendRun("regular", post.getId(), auth(actor), requests);
        result("absence_correction_20", stats); assertEquals(43, stats.getPrepareStatementCount());
    }

    @Test void derivedPointDeleteReturnsOriginalCoordinatesWithoutExtraSelect() {
        var actor = user(); var post = post(actor, PostType.REGULAR); members(post, 20, true, true);
        var stats = start(); var removed = points.deleteByPost(post);
        assertEquals(20, removed.size());
        assertFalse(RankingChanged.points(removed).points().isEmpty());
        result("derived_point_delete_20", stats); assertEquals(21, stats.getPrepareStatementCount());
    }

    @Test void homeAndCalendarRemainConstantQueryCountsButLoadPostBodies() {
        var actor = user(); var day = LocalDate.now(DateTimeUtils.getDefaultZone()).plusDays(1);
        for (var type : PostType.values()) {
            posts.save(Post.create(actor, "title", "loc", DateTimeUtils.startOfDayUtc(day).plusHours(1), "body".repeat(1000), null, type));
        }
        var stats = start(); home.getHome(auth(actor)); result("home_4_posts", stats); assertEquals(5, stats.getPrepareStatementCount());
        stats = start(); calendar.getDailySchedule(day); result("daily_4_posts", stats); assertEquals(1, stats.getPrepareStatementCount());
        stats = start(); calendar.getMonthlySchedule(auth(actor), day); result("monthly_4_posts", stats); assertEquals(2, stats.getPrepareStatementCount());
        stats = start(); profiles.getProfile(auth(actor), day); result("profile", stats); assertEquals(4, stats.getPrepareStatementCount());
    }
}
