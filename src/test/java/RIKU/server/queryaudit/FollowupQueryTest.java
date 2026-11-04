package RIKU.server.queryaudit;

import RIKU.server.domains.admin.dto.request.AuthorizePacerRequest;
import RIKU.server.domains.admin.dto.request.UpdateUsersRequest;
import RIKU.server.domains.admin.service.AdminService;
import RIKU.server.domains.calendar.service.CalendarService;
import RIKU.server.domains.point.ranking.RankingChanged;
import RIKU.server.domains.run.pacer.entity.Pacer;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.entity.ParticipantStatus;
import RIKU.server.domains.run.participant.service.ParticipantService;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.season.SeasonProvider;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.entity.UserRole;
import RIKU.server.domains.user.exception.UserException;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.global.response.BaseResponseStatus;
import RIKU.server.security.AuthMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=" +
        "RIKU.server.queryaudit.FollowupQueryTest$LockObserver")
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({AdminService.class, ParticipantService.class, CalendarService.class})
class FollowupQueryTest {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired AdminService admin;
    @Autowired ParticipantService attendance;
    @Autowired CalendarService calendar;
    @Autowired ApplicationEvents events;
    @MockBean SeasonProvider seasons;
    @MockBean PasswordEncoder encoder;

    private User user() {
        User user = User.create(UUID.randomUUID().toString(), "pw", "member", "college", "major", null);
        em.persist(user);
        return user;
    }

    private AuthMember auth(User user) {
        return AuthMember.of(user.getId(), user.getStudentId(), "", AuthorityUtils.createAuthorityList("ROLE_ADMIN"), BaseStatus.ACTIVE);
    }

    private Post post(User owner, PostType type, LocalDateTime date) {
        var post = Post.create(owner, "run", "location", date, "body".repeat(100), null, type);
        em.persist(post);
        return post;
    }

    private Post openRun(User owner, PostType type) {
        Post post = post(owner, type, LocalDateTime.now().minusMinutes(1));
        if (type == PostType.REGULAR) em.persist(RegularPost.create(post));
        else em.persist(TrainingPost.create(post, "training"));
        return post;
    }

    private Participant participant(Post post, User user) {
        var participant = Participant.createWithGroup(post, user, "A");
        em.persist(participant);
        return participant;
    }

    private void pacer(Post post, User user) {
        em.persist(Pacer.create(user, post, "A", "6:00", "5"));
    }

    private UpdateUsersRequest update(String studentId, String role, Boolean isPacer) {
        var request = new UpdateUsersRequest();
        ReflectionTestUtils.setField(request, "studentId", studentId);
        ReflectionTestUtils.setField(request, "userRole", role);
        ReflectionTestUtils.setField(request, "isPacer", isPacer);
        return request;
    }

    private AuthorizePacerRequest authorize(String studentId, boolean isPacer) {
        var request = new AuthorizePacerRequest();
        ReflectionTestUtils.setField(request, "studentId", studentId);
        ReflectionTestUtils.setField(request, "isPacer", isPacer);
        return request;
    }

    private Statistics start() {
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    private void counts(String scenario, Statistics stats, long sql, long updates) {
        System.out.printf("FOLLOWUP %s sql=%d updates=%d postLoads=%d%n", scenario, stats.getPrepareStatementCount(),
                stats.getEntityUpdateCount(), stats.getEntityStatistics(Post.class.getName()).getLoadCount());
        assertEquals(sql, stats.getPrepareStatementCount());
        assertEquals(updates, stats.getEntityUpdateCount());
        assertEquals(0, stats.getEntityInsertCount());
        assertEquals(0, stats.getEntityDeleteCount());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void updateTwentyDistinctStudents(boolean pacerOnly) {
        var actor = auth(user());
        var members = new ArrayList<User>();
        for (int i = 0; i < 20; i++) members.add(user());
        var stats = start();
        if (pacerOnly) {
            admin.authorizePacer(actor, members.stream().map(u -> authorize(u.getStudentId(), true)).toList());
        } else {
            admin.updateUsers(actor, members.stream().map(u -> update(u.getStudentId(), "INACTIVE", true)).toList());
        }
        em.flush();
        counts(pacerOnly ? "authorize_20" : "members_20", stats, 21, 20);
        assertEquals(pacerOnly ? 0 : 1, events.stream(RankingChanged.class).count());
        em.clear();
        for (User member : members) {
            var saved = em.find(User.class, member.getId());
            assertTrue(saved.getIsPacer());
            assertEquals(pacerOnly ? BaseStatus.ACTIVE : BaseStatus.INACTIVE, saved.getStatus());
        }
    }

    @Test
    void largeNoOpRequestBoundsTheNumberOfIdsPerQuery() {
        var actor = auth(user());
        var requests = new ArrayList<UpdateUsersRequest>();
        for (int i = 0; i < 501; i++) requests.add(update(user().getStudentId(), null, null));
        var stats = start();
        admin.updateUsers(actor, requests);
        em.flush();
        counts("members_501_no_changes", stats, 2, 0);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void duplicateStudentRevertedInOrderStillUpdatesAuditTimestamp(boolean pacerOnly) {
        var member = user();
        var actor = auth(member);
        var id = member.getStudentId();
        // Use an older stored timestamp so this assertion is independent of clock resolution.
        em.createQuery("update User u set u.updatedAt = :past where u.id = :id")
                .setParameter("past", LocalDateTime.of(2000, 1, 1, 0, 0)).setParameter("id", member.getId()).executeUpdate();
        var stats = start();
        if (pacerOnly) admin.authorizePacer(actor, List.of(authorize(id, true), authorize(id, false)));
        else admin.updateUsers(actor, List.of(update(id, "INACTIVE", true), update(id, "NEW_MEMBER", false)));
        em.flush();
        counts("duplicate_" + pacerOnly, stats, 4, 2);
        assertEquals(pacerOnly ? 0 : 1, events.stream(RankingChanged.class).count());
        em.clear();
        var saved = em.find(User.class, member.getId());
        assertEquals(BaseStatus.ACTIVE, saved.getStatus());
        assertEquals(UserRole.NEW_MEMBER, saved.getUserRole());
        assertFalse(saved.getIsPacer());
        assertTrue(saved.getUpdatedAt().getYear() > 2000);
    }

    private void commitFixture() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    @Test @DirtiesContext
    void invalidRoleAndMissingStudentPreserveFirstErrorAndRollback() {
        var member = user();
        var actor = auth(member);
        var valid = update(member.getStudentId(), "INACTIVE", true);
        var invalid = update(user().getStudentId(), "invalid-role", false);
        var missing = update("missing", null, true);
        commitFixture();
        assertThrows(IllegalArgumentException.class, () -> admin.updateUsers(actor, List.of(valid, invalid, missing)));
        assertEquals(BaseResponseStatus.USER_NOT_FOUND,
                assertThrows(UserException.class, () -> admin.updateUsers(actor, List.of(valid, missing, invalid))).getStatus());
        var saved = em.find(User.class, member.getId());
        assertFalse(saved.getIsPacer());
        assertEquals(BaseStatus.ACTIVE, saved.getStatus());
        assertEquals(0, events.stream(RankingChanged.class).count());
    }

    @Test
    void firstCodeForTwentyPacers() {
        var owner = user();
        var actor = auth(owner);
        var post = openRun(owner, PostType.REGULAR);
        List<Long> participantIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            var member = user();
            pacer(post, member);
            participantIds.add(participant(post, member).getId());
        }
        var stats = start();
        String code = attendance.createAttendanceCode("regular", post.getId(), actor);
        em.flush();
        counts("regular_20", stats, 25, 21);
        em.clear();
        assertTrue(code.matches("[0-9]{3}"));
        assertEquals(code, em.find(RegularPost.class, post.getId()).getAttendanceCode());
        participantIds.forEach(id -> assertEquals(ParticipantStatus.ATTENDED, em.find(Participant.class, id).getParticipantStatus()));
    }

    @ParameterizedTest @EnumSource(value = PostType.class, names = {"REGULAR", "TRAINING"})
    void attendanceIsLimitedToExistingPacerParticipantsOnThisPost(PostType type) {
        var owner = user();
        var actor = auth(owner);
        var post = openRun(owner, type);
        var other = openRun(owner, type);
        var member = user();
        pacer(post, member);
        var alreadyAttended = participant(post, member);
        alreadyAttended.attend();
        var missing = user();
        pacer(post, missing);
        var onOtherRun = participant(other, missing);
        var ordinary = participant(post, user());
        var eligible = user();
        pacer(post, eligible);
        var absent = participant(post, eligible);
        absent.absent();
        var stats = start();
        String code = attendance.createAttendanceCode(type.name(), post.getId(), actor);
        em.flush();
        assertEquals(2, stats.getEntityUpdateCount());
        stats = start();
        assertEquals(code, attendance.createAttendanceCode(type.name(), post.getId(), actor));
        em.flush();
        counts("existing_" + type, stats, 2, 0);
        assertEquals(ParticipantStatus.ATTENDED, em.find(Participant.class, absent.getId()).getParticipantStatus());
        assertEquals(ParticipantStatus.ATTENDED, em.find(Participant.class, alreadyAttended.getId()).getParticipantStatus());
        assertEquals(ParticipantStatus.PENDING, em.find(Participant.class, ordinary.getId()).getParticipantStatus());
        assertEquals(ParticipantStatus.PENDING, em.find(Participant.class, onOtherRun.getId()).getParticipantStatus());
    }

    @Test
    void duplicateParticipationRowsStillFail() {
        var owner = user();
        var actor = auth(owner);
        var post = openRun(owner, PostType.REGULAR);
        var member = user();
        pacer(post, member);
        participant(post, member);
        participant(post, member);
        start();
        assertThrows(IncorrectResultSizeDataAccessException.class,
                () -> attendance.createAttendanceCode("regular", post.getId(), actor));
    }

    public static class LockObserver implements StatementInspector {
        static final ThreadLocal<CountDownLatch> waiting = new ThreadLocal<>();

        @Override public String inspect(String sql) {
            var latch = waiting.get();
            if (latch != null && sql.contains("for update")) latch.countDown();
            return sql;
        }
    }

    @Test @DirtiesContext
    void secondCodeRequestReusesFirstCommitUnderThePostLock() throws Exception {
        var owner = user();
        var actor = auth(owner);
        var post = openRun(owner, PostType.REGULAR);
        var member = user();
        pacer(post, member);
        var participant = participant(post, member);
        commitFixture();
        TestTransaction.start();
        var stats = start();
        String first = attendance.createAttendanceCode("regular", post.getId(), actor);
        em.flush(); // First request has written the code and still owns the post lock.
        var executor = Executors.newSingleThreadExecutor();
        var secondLock = new CountDownLatch(1);
        try {
            var second = executor.submit(() -> {
                LockObserver.waiting.set(secondLock);
                try {
                    return attendance.createAttendanceCode("regular", post.getId(), actor);
                } finally {
                    LockObserver.waiting.remove();
                }
            });
            assertTrue(secondLock.await(5, SECONDS), "second transaction must issue SELECT FOR UPDATE");
            assertFalse(second.isDone());
            commitFixture();
            assertEquals(first, second.get(5, SECONDS));
            counts("concurrent_pair", stats, 8, 2);
            assertEquals(first, em.find(RegularPost.class, post.getId()).getAttendanceCode());
            assertEquals(ParticipantStatus.ATTENDED, em.find(Participant.class, participant.getId()).getParticipantStatus());
        } finally {
            if (TestTransaction.isActive()) TestTransaction.end();
            executor.shutdownNow();
        }
    }

    @Test
    void leapMonthUsesKoreanDatesAndExistingInclusiveEnd() {
        var owner = user();
        var actor = auth(owner);
        var firstUtc = LocalDateTime.of(2024, 1, 31, 15, 0);
        var nextMonthUtc = LocalDateTime.of(2024, 2, 29, 15, 0); // 2024-03-01 00:00 KST
        post(owner, PostType.REGULAR, firstUtc.minusSeconds(1));
        post(owner, PostType.REGULAR, firstUtc);
        post(owner, PostType.FLASH, firstUtc.plusMinutes(30));
        post(owner, PostType.TRAINING, firstUtc.plusDays(1));
        post(owner, PostType.EVENT, nextMonthUtc.minusSeconds(1));
        post(owner, PostType.REGULAR, nextMonthUtc.minusNanos(500_000_000)); // outside the existing 23:59:59 cutoff
        post(owner, PostType.REGULAR, nextMonthUtc);
        post(owner, PostType.FLASH, firstUtc.plusDays(2)).updateStatus(BaseStatus.INACTIVE);
        var stats = start();
        var schedule = calendar.getMonthlySchedule(actor, LocalDate.of(2024, 2, 12));
        counts("february", stats, 2, 0);
        assertEquals(0, stats.getEntityStatistics(Post.class.getName()).getLoadCount());
        assertEquals("NEW_MEMBER", schedule.getUserRole());
        assertThat(schedule.getSchedules()).extracting("date", "eventCount").containsExactly(
                tuple(LocalDate.of(2024, 2, 1), 2L), tuple(LocalDate.of(2024, 2, 2), 1L),
                tuple(LocalDate.of(2024, 2, 29), 1L));
        assertThat(calendar.getMonthlySchedule(actor, LocalDate.of(2024, 4, 1)).getSchedules()).isEmpty();
    }
}
