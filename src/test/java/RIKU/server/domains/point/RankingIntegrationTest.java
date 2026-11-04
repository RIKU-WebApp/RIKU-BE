package RIKU.server.domains.point;

import RIKU.server.domains.admin.dto.request.UpdateUsersRequest;
import RIKU.server.domains.admin.service.AdminService;
import RIKU.server.domains.point.dto.request.ReadUserEventRankingRequest;
import RIKU.server.domains.point.entity.*;
import RIKU.server.domains.point.ranking.*;
import RIKU.server.domains.point.repository.UserPointRepository;
import RIKU.server.domains.point.service.UserPointService;
import RIKU.server.domains.run.participant.dto.request.ManualAttendParticipantRequest;
import RIKU.server.domains.run.participant.entity.Participant;
import RIKU.server.domains.run.participant.repository.ParticipantRepository;
import RIKU.server.domains.run.participant.service.ParticipantService;
import RIKU.server.domains.run.post.entity.*;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.domains.run.post.service.PostService;
import RIKU.server.domains.season.SeasonProvider;
import RIKU.server.domains.user.dto.request.*;
import RIKU.server.domains.user.entity.User;
import RIKU.server.domains.user.repository.UserRepository;
import RIKU.server.domains.user.service.UserService;
import RIKU.server.global.entity.BaseStatus;
import RIKU.server.global.util.DateTimeUtils;
import RIKU.server.infra.storage.S3Uploader;
import RIKU.server.security.AuthMember;
import com.amazonaws.services.s3.AmazonS3;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class RankingIntegrationTest {
    @Autowired UserRepository users;
    @Autowired UserPointRepository points;
    @Autowired PostRepository posts;
    @Autowired ParticipantRepository participants;
    @Autowired UserPointService service;
    @Autowired UserService userService;
    @Autowired AdminService adminService;
    @Autowired ParticipantService participantService;
    @Autowired PostService postService;
    @Autowired RankingCache cache;
    @Autowired RankingSnapshotLoader loader;
    @Autowired SeasonProvider season;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    @MockBean AmazonS3 amazonS3;
    @MockBean S3Uploader s3;
    @MockBean(name = "rankingRefreshExecutor") ThreadPoolTaskExecutor refreshExecutor;
    private final Queue<Runnable> refreshes = new ArrayDeque<>();

    @BeforeEach void reset() {
        doAnswer(inv -> { refreshes.add(inv.getArgument(0)); return null; }).when(refreshExecutor).execute(any(Runnable.class));
        drain();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (String table : List.of("user_point", "participant", "comment", "pacer", "attachment",
                    "flash_post", "regular_post", "training_post", "event_post", "post", "users")) {
                jdbc.update("delete from " + table);
            }
        });
        jdbc.execute("alter table users alter column user_id restart with 1000");
        var entries = (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(cache, "entries");
        assertNotNull(entries); entries.invalidateAll(); entries.cleanUp();
    }

    @AfterEach void drain() {
        int executed = 0;
        while (!refreshes.isEmpty()) {
            assertTrue(++executed < 100, "refresh must settle");
            refreshes.remove().run();
        }
    }

    User user(String name) {
        User u;
        do { u = users.save(User.create(UUID.randomUUID().toString(), "test", name, "college", "major", null)); }
        while (Set.of(10L, 29L, 6L, 79L).contains(u.getId()));
        return u;
    }
    AuthMember auth(User u) { return AuthMember.of(u.getId(), u.getStudentId(), "", AuthorityUtils.createAuthorityList("ROLE_ADMIN"), BaseStatus.ACTIVE); }
    RankingQuery seasonQuery() { return RankingQuery.season(season.seasonStartUtc()); }
    Statistics start() { var s = emf.unwrap(SessionFactory.class).getStatistics(); s.clear(); return s; }
    void point(User u, int value, PointType type, LocalDateTime createdAt) {
        var p = points.save(UserPoint.create(u, value, "test", type));
        jdbc.update("update user_point set created_at = ? where point_id = ?", createdAt, p.getId());
    }
    ReadUserEventRankingRequest event(LocalDate from, LocalDate to, List<PointType> types) {
        var request = new ReadUserEventRankingRequest();
        ReflectionTestUtils.setField(request, "startDate", from);
        ReflectionTestUtils.setField(request, "endDate", to);
        ReflectionTestUtils.setField(request, "pointTypes", types); return request;
    }

    @Test void fiftyMemberRankingUsesThreeQueriesColdAndOneWarm() {
        User first = user("첫번째"); User second = user("두번째");
        for (int i = 2; i < 50; i++) user("회원" + i);
        var stats = start();
        assertEquals(20, service.getUserPointRanking(auth(first)).getTop20().size());
        assertEquals(3, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());
        start();
        assertEquals(second.getId(), service.getUserPointRanking(auth(second)).getUserPoints().getUserId());
        assertEquals(1, stats.getPrepareStatementCount());
        System.out.println("OPTIMIZED ranking: users=50 cold SQL=3, warm SQL=1 (member existence only)");
    }

    @Test void eventFiltersAndKstExclusiveBoundaryAreCachedByNormalizedTypes() {
        User u = user("이벤트"); LocalDate day = LocalDate.of(2026, 6, 1);
        var start = DateTimeUtils.startOfDayUtc(day); var end = DateTimeUtils.startOfDayUtc(day.plusDays(1));
        point(u, 100, PointType.ADD_ATTENDANCE, start.minusSeconds(1));
        point(u, 1, PointType.ADD_ATTENDANCE, start);
        point(u, 5, PointType.ADD_FLASH_JOIN, end.minusNanos(500_000_000));
        point(u, 100, PointType.ADD_ATTENDANCE, end);
        point(u, -3, PointType.REMOVE, start.plusHours(1));
        var request = event(day, day, List.of(PointType.ADD_ATTENDANCE, PointType.ADD_FLASH_JOIN));
        var stats = start();
        assertEquals(6, service.getUserEventPointRanking(request, auth(u)).getUserPoints().getTotalPoints());
        assertEquals(3, stats.getPrepareStatementCount());
        start();
        var reordered = event(day, day, List.of(PointType.ADD_FLASH_JOIN, PointType.ADD_ATTENDANCE, PointType.ADD_ATTENDANCE));
        assertEquals(6, service.getUserEventPointRanking(reordered, auth(u)).getUserPoints().getTotalPoints());
        assertEquals(1, stats.getPrepareStatementCount());
        assertEquals(-3, service.getUserEventPointRanking(event(day, day, List.of(PointType.REMOVE)), auth(u)).getUserPoints().getTotalPoints());
        System.out.println("OPTIMIZED event ranking: cold SQL=3, warm SQL=1, point entity loads=0");
    }

    @Test void dailyAttendanceOnlyRefreshesAfterCommitAndWarmsExistingEvents() {
        User u = user("출석"); LocalDate day = LocalDate.now(DateTimeUtils.getDefaultZone());
        var query = new RankingQuery(DateTimeUtils.startOfDayUtc(day), DateTimeUtils.startOfDayUtc(day.plusDays(1)), Set.of(PointType.ADD_ATTENDANCE));
        var before = cache.get(query); drain();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userService.attendProfile(auth(u));
            assertSame(before, cache.get(query), "uncommitted point must not invalidate");
            assertTrue(refreshes.isEmpty());
        });
        assertEquals(1, refreshes.size());
        drain(); start();
        assertEquals(1, cache.get(query).forUser(u.getId()).getUserPoints().getTotalPoints());
        assertEquals(0, emf.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount());
    }

    @Test void twentyMemberEventUsesGroupedProjectionWithoutLoadingPointEntities() {
        List<User> members = new ArrayList<>();
        LocalDate day = LocalDate.of(2026, 6, 1);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (int i = 0; i < 20; i++) {
                User member = user("회원" + i); members.add(member);
                for (int j = 0; j < 5; j++) {
                    point(member, 1, j < 3 ? PointType.ADD_ATTENDANCE : PointType.REMOVE,
                            DateTimeUtils.startOfDayUtc(day).plusHours(1));
                }
            }
        });
        var request = event(day, day, List.of(PointType.ADD_ATTENDANCE));
        var stats = start();
        var result = service.getUserEventPointRanking(request, auth(members.get(0)));
        assertEquals(20, result.getTop20().size());
        assertTrue(result.getTop20().stream().allMatch(row -> row.getTotalPoints() == 3));
        assertEquals(3, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());
        start(); service.getUserEventPointRanking(request, auth(members.get(1)));
        assertEquals(1, stats.getPrepareStatementCount());
        System.out.println("OPTIMIZED event ranking: users=20 points=100 cold SQL=3 warm SQL=1 entityLoads=0");
    }

    @Test void rolledBackAttendanceKeepsPreviousSnapshot() {
        User u = user("롤백"); var before = cache.get(seasonQuery()); drain();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userService.attendProfile(auth(u)); status.setRollbackOnly();
        });
        assertTrue(refreshes.isEmpty());
        assertSame(before, cache.get(seasonQuery()));
        assertEquals(0, points.count());
    }

    @Test void closeCorrectionAndPostDeletionRefreshPointTotals() {
        User creator = user("작성자"); User attendee = user("참가자");
        Post post = posts.save(Post.create(creator, "regular", "loc", LocalDateTime.now(), "body", null, PostType.REGULAR));
        var participant = Participant.createWithGroup(post, attendee, "A"); participant.attend(); participants.save(participant);
        assertEquals(0, cache.get(seasonQuery()).forUser(attendee.getId()).getUserPoints().getTotalPoints()); drain();
        participantService.closeRun("regular", post.getId(), auth(creator)); drain();
        assertEquals(10, cache.get(seasonQuery()).forUser(attendee.getId()).getUserPoints().getTotalPoints());
        var correction = new ManualAttendParticipantRequest();
        ReflectionTestUtils.setField(correction, "userId", attendee.getId());
        ReflectionTestUtils.setField(correction, "isAttend", false);
        participantService.fixAttendRun("regular", post.getId(), auth(creator), List.of(correction)); drain();
        assertEquals(0, cache.get(seasonQuery()).forUser(attendee.getId()).getUserPoints().getTotalPoints());
        ReflectionTestUtils.setField(correction, "isAttend", true);
        participantService.fixAttendRun("regular", post.getId(), auth(creator), List.of(correction)); drain();
        assertEquals(10, cache.get(seasonQuery()).forUser(attendee.getId()).getUserPoints().getTotalPoints());
        postService.deletePost(auth(creator), "regular", post.getId()); drain();
        assertEquals(0, cache.get(seasonQuery()).forUser(attendee.getId()).getUserPoints().getTotalPoints());
    }

    @Test void signupInactiveStatusAndAvatarChangesRefreshMembership() {
        User admin = user("관리자"); cache.get(seasonQuery()); drain();
        var signup = new SignUpUserRequest();
        for (var entry : Map.of("studentId", "new-member", "password", "Test1234!", "name", "신입", "college", "대학", "major", "학과").entrySet()) {
            ReflectionTestUtils.setField(signup, entry.getKey(), entry.getValue());
        }
        Long id = userService.signUp(signup); drain();
        assertNotNull(cache.get(seasonQuery()).forUser(id).getUserPoints());
        User member = users.findById(id).orElseThrow();
        member.updateProfile(null, member.getPassword(), "https://example.com/profile.png"); users.save(member);
        cache.onRankingChanged(RankingChanged.members()); drain();
        assertNotNull(cache.get(seasonQuery()).forUser(id).getUserPoints().getUserProfileImg());
        var profile = new UpdateProfileRequest(); profile.setUserProfileImg(new MockMultipartFile("userProfileImg", new byte[0]));
        userService.updateProfile(auth(member), profile); drain();
        assertNull(cache.get(seasonQuery()).forUser(id).getUserPoints().getUserProfileImg());
        var change = new UpdateUsersRequest();
        ReflectionTestUtils.setField(change, "studentId", member.getStudentId());
        ReflectionTestUtils.setField(change, "userRole", "INACTIVE");
        adminService.updateUsers(auth(admin), List.of(change)); drain();
        assertEquals(-1, cache.get(seasonQuery()).forUser(id).getUserRanking());
    }

    @Test void nonRankingProfileAndAdminChangesDoNotRebuildSnapshots() {
        User member = user("정보변경");
        var before = cache.get(seasonQuery()); drain();
        var profile = new UpdateProfileRequest();
        profile.setPhone("01012345678"); profile.setPassword("Changed1234!");
        userService.updateProfile(auth(member), profile);
        var change = new UpdateUsersRequest();
        ReflectionTestUtils.setField(change, "studentId", member.getStudentId());
        ReflectionTestUtils.setField(change, "userRole", "MEMBER");
        ReflectionTestUtils.setField(change, "isPacer", true);
        adminService.updateUsers(auth(member), List.of(change));
        assertTrue(refreshes.isEmpty());
        assertSame(before, cache.get(seasonQuery()));
        assertEquals("01012345678", users.findById(member.getId()).orElseThrow().getPhone());
    }

    @Test void closingCorrectingAndDeletingWithoutPointChangesDoesNotRebuild() {
        User creator = user("무득점");
        Post post = posts.save(Post.create(creator, "regular", "loc", LocalDateTime.now(), "body", null, PostType.REGULAR));
        participants.save(Participant.createWithGroup(post, creator, "A"));
        var before = cache.get(seasonQuery()); drain();
        participantService.closeRun("regular", post.getId(), auth(creator));
        var correction = new ManualAttendParticipantRequest();
        ReflectionTestUtils.setField(correction, "userId", creator.getId());
        ReflectionTestUtils.setField(correction, "isAttend", false);
        participantService.fixAttendRun("regular", post.getId(), auth(creator), List.of(correction));
        postService.deletePost(auth(creator), "regular", post.getId());
        assertTrue(refreshes.isEmpty());
        assertSame(before, cache.get(seasonQuery()));
    }

    @Test void backgroundRefreshCostIsTwoQueriesForEachAffectedCondition() {
        User member = user("갱신비용");
        var queue = new ArrayDeque<Runnable>();
        var isolatedCache = new RankingCache(loader, queue::add);
        var day = DateTimeUtils.startOfDayUtc(LocalDate.now(DateTimeUtils.getDefaultZone()));
        var queries = List.of(seasonQuery(),
                new RankingQuery(day, day.plusDays(1), Set.of(PointType.ADD_ATTENDANCE)),
                new RankingQuery(day.minusDays(10), day.minusDays(9), Set.of(PointType.ADD_FLASH_JOIN)));
        queries.forEach(isolatedCache::get);
        var untouched = isolatedCache.get(queries.get(2));
        point(member, 1, PointType.ADD_ATTENDANCE, day.plusHours(1));
        var stats = start();
        var change = new RankingChanged(false, Set.of(new RankingChanged.PointChange(day.plusHours(1), PointType.ADD_ATTENDANCE)));
        for (int i = 0; i < 100; i++) isolatedCache.onRankingChanged(change);
        assertEquals(1, queue.size());
        assertEquals(0, stats.getPrepareStatementCount(), "committed write thread does not aggregate");
        queue.remove().run();
        assertEquals(4, stats.getPrepareStatementCount());
        assertSame(untouched, isolatedCache.get(queries.get(2)));
        assertEquals(0, stats.getEntityLoadCount());
        start();
        for (var query : queries) isolatedCache.get(query).forUser(member.getId());
        assertEquals(0, stats.getPrepareStatementCount());
        System.out.println("OPTIMIZED selective refresh: active conditions=3 affected=2, 100 queued events, background SQL=4 (was 6), subsequent cache SQL=0");
    }

    RankingQuery eventQuery(LocalDate day, PointType type) {
        var start = DateTimeUtils.startOfDayUtc(day);
        return new RankingQuery(start, start.plusDays(1), Set.of(type));
    }

    @Test void dailyAttendanceDoesNotRebuildPastOrOtherPointTypeEvents() {
        User member = user("선별출석"); var today = LocalDate.now(DateTimeUtils.getDefaultZone());
        var affected = eventQuery(today, PointType.ADD_ATTENDANCE);
        var wrongType = eventQuery(today, PointType.ADD_FLASH_JOIN);
        var past = eventQuery(today.minusDays(1), PointType.ADD_ATTENDANCE);
        cache.get(seasonQuery()); cache.get(affected);
        var typeBefore = cache.get(wrongType); var pastBefore = cache.get(past);
        userService.attendProfile(auth(member));
        var stats = start(); drain();
        assertEquals(4, stats.getPrepareStatementCount());
        assertEquals(1, cache.get(affected).forUser(member.getId()).getUserPoints().getTotalPoints());
        assertSame(typeBefore, cache.get(wrongType)); assertSame(pastBefore, cache.get(past));
    }

    @Test void deletingPostUsesEveryDeletedPointsOriginalDateAndType() {
        User creator = user("과거삭제"); User attendee = user("과거참여");
        var today = LocalDate.now(DateTimeUtils.getDefaultZone()); var oldDay = today.minusDays(20);
        Post post = posts.save(Post.create(creator, "old", "loc", oldDay.atStartOfDay(), "body", null, PostType.FLASH));
        var first = points.save(UserPoint.createWithPost(attendee, 5, "old join", PointType.ADD_FLASH_JOIN, post));
        var second = points.save(UserPoint.createWithPost(creator, 7, "old create", PointType.ADD_FLASH_CREATE, post));
        jdbc.update("update user_point set created_at = ? where point_id = ?", DateTimeUtils.startOfDayUtc(oldDay), first.getId());
        jdbc.update("update user_point set created_at = ? where point_id = ?", DateTimeUtils.startOfDayUtc(oldDay.plusDays(1)), second.getId());
        var join = eventQuery(oldDay, PointType.ADD_FLASH_JOIN);
        var create = eventQuery(oldDay.plusDays(1), PointType.ADD_FLASH_CREATE);
        var wrongType = eventQuery(oldDay, PointType.ADD_ATTENDANCE);
        var current = eventQuery(today, PointType.ADD_FLASH_JOIN);
        var futureSeason = RankingQuery.season(DateTimeUtils.startOfDayUtc(today));
        assertEquals(5, cache.get(join).forUser(attendee.getId()).getUserPoints().getTotalPoints());
        assertEquals(7, cache.get(create).forUser(creator.getId()).getUserPoints().getTotalPoints());
        var typeBefore = cache.get(wrongType); var currentBefore = cache.get(current); var futureBefore = cache.get(futureSeason);
        postService.deletePost(auth(creator), "flash", post.getId());
        var stats = start(); drain();
        assertEquals(4, stats.getPrepareStatementCount());
        assertEquals(0, cache.get(join).forUser(attendee.getId()).getUserPoints().getTotalPoints());
        assertEquals(0, cache.get(create).forUser(creator.getId()).getUserPoints().getTotalPoints());
        assertSame(typeBefore, cache.get(wrongType)); assertSame(currentBefore, cache.get(current));
        assertSame(futureBefore, cache.get(futureSeason));
    }

    @Test void historicalCorrectionInvalidatesDeletionDateAndNewAwardDateSeparately() {
        User member = user("과거정정");
        var today = LocalDate.now(DateTimeUtils.getDefaultZone()); var oldDay = today.minusDays(20);
        Post post = Post.create(member, "old", "loc", oldDay.atStartOfDay(), "body", null, PostType.REGULAR);
        post.updatePostStatus(PostStatus.CLOSED); post = posts.save(post);
        var participant = Participant.createWithGroup(post, member, "A"); participant.attend(); participants.save(participant);
        var original = points.save(UserPoint.createWithPost(member, 10, "old", PointType.ADD_REGULAR_JOIN, post));
        jdbc.update("update user_point set created_at = ? where point_id = ?", DateTimeUtils.startOfDayUtc(oldDay), original.getId());
        var past = eventQuery(oldDay, PointType.ADD_REGULAR_JOIN); var current = eventQuery(today, PointType.ADD_REGULAR_JOIN);
        var wrongType = eventQuery(oldDay, PointType.ADD_ATTENDANCE);
        var allTime = RankingQuery.season(DateTimeUtils.startOfDayUtc(oldDay));
        assertEquals(10, cache.get(past).forUser(member.getId()).getUserPoints().getTotalPoints());
        var todayBefore = cache.get(current); var wrongBefore = cache.get(wrongType); cache.get(allTime);
        var correction = new ManualAttendParticipantRequest();
        ReflectionTestUtils.setField(correction, "userId", member.getId());
        ReflectionTestUtils.setField(correction, "isAttend", false);
        participantService.fixAttendRun("regular", post.getId(), auth(member), List.of(correction));
        var stats = start(); drain();
        assertEquals(4, stats.getPrepareStatementCount());
        var pastAfterDeletion = cache.get(past);
        assertEquals(0, pastAfterDeletion.forUser(member.getId()).getUserPoints().getTotalPoints());
        assertSame(todayBefore, cache.get(current)); assertSame(wrongBefore, cache.get(wrongType));
        ReflectionTestUtils.setField(correction, "isAttend", true);
        participantService.fixAttendRun("regular", post.getId(), auth(member), List.of(correction));
        start(); drain(); assertEquals(4, stats.getPrepareStatementCount());
        assertSame(pastAfterDeletion, cache.get(past));
        assertEquals(10, cache.get(current).forUser(member.getId()).getUserPoints().getTotalPoints());
        assertEquals(10, cache.get(allTime).forUser(member.getId()).getUserPoints().getTotalPoints());
    }

    @Test void rolledBackHistoricalPointDeletionKeepsSnapshots() {
        User member = user("삭제롤백"); var day = LocalDate.now(DateTimeUtils.getDefaultZone()).minusDays(20);
        Post post = posts.save(Post.create(member, "rollback", "loc", day.atStartOfDay(), "body", null, PostType.FLASH));
        var original = points.save(UserPoint.createWithPost(member, 5, "old", PointType.ADD_FLASH_JOIN, post));
        jdbc.update("update user_point set created_at = ? where point_id = ?", DateTimeUtils.startOfDayUtc(day), original.getId());
        var query = eventQuery(day, PointType.ADD_FLASH_JOIN); var before = cache.get(query);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            postService.deletePost(auth(member), "flash", post.getId()); status.setRollbackOnly();
        });
        assertTrue(refreshes.isEmpty()); assertSame(before, cache.get(query)); assertEquals(1, points.count());
    }

    @Test void closingFlashRefreshesBothJoinAndCreatorPointTypes() {
        var creator = user("번개개설"); var member = user("번개참여");
        var today = LocalDate.now(DateTimeUtils.getDefaultZone());
        var post = posts.save(Post.create(creator, "flash", "loc", LocalDateTime.now(), "body", null, PostType.FLASH));
        var participant = Participant.create(post, member); participant.attend(); participants.save(participant);
        var join = eventQuery(today, PointType.ADD_FLASH_JOIN);
        var create = eventQuery(today, PointType.ADD_FLASH_CREATE);
        var other = eventQuery(today, PointType.ADD_ATTENDANCE);
        cache.get(join); cache.get(create); var untouched = cache.get(other);
        participantService.closeRun("flash", post.getId(), auth(creator));
        var stats = start(); drain(); assertEquals(4, stats.getPrepareStatementCount());
        assertEquals(5, cache.get(join).forUser(member.getId()).getUserPoints().getTotalPoints());
        assertEquals(7, cache.get(create).forUser(creator.getId()).getUserPoints().getTotalPoints());
        assertSame(untouched, cache.get(other));
    }

    @Test void zeroPointUsersAndKoreanTieOrderingAreRetained() {
        User english = user("Alice"); User koreanB = user("나"); User koreanA = user("가"); User zero = user("제로"); User negative = user("음수");
        var time = season.seasonStartUtc().plusDays(1);
        for (User u : List.of(english, koreanB, koreanA)) point(u, 5, PointType.ADD_ATTENDANCE, time);
        point(negative, -1, PointType.REMOVE, time);
        User inactive = user("비활성"); inactive.updateStatus(BaseStatus.INACTIVE); users.save(inactive);
        point(inactive, 100, PointType.ADD_ATTENDANCE, time);
        jdbc.update("insert into users(user_id, student_id, name, status) values (10, 'excluded', '제외회원', 'ACTIVE')");
        point(users.findById(10L).orElseThrow(), 100, PointType.ADD_ATTENDANCE, time);
        var result = service.getUserPointRanking(auth(zero));
        assertEquals(List.of(koreanA.getId(), koreanB.getId(), english.getId()), result.getTop20().stream().limit(3).map(r -> r.getUserId()).toList());
        assertEquals(4, result.getUserRanking());
        assertEquals(0, result.getUserPoints().getTotalPoints());
        assertEquals(5, service.getUserPointRanking(auth(negative)).getUserRanking());
        var excluded = service.getUserPointRanking(AuthMember.builder().id(10L).build());
        assertEquals(-1, excluded.getUserRanking());
        assertNull(excluded.getUserPoints());
        assertEquals(5, excluded.getTop20().size());
    }

    @Test void activityKeepsNullPostPointsAndDoesNotFetchEachTitle() {
        User u = user("활동");
        for (int i = 0; i < 10; i++) {
            Post post = posts.save(Post.create(u, "title" + i, "loc", LocalDateTime.now(), "body", null, PostType.FLASH));
            points.save(UserPoint.createWithPost(u, 5, "run", PointType.ADD_FLASH_JOIN, post));
        }
        points.save(UserPoint.create(u, 1, "attendance", PointType.ADD_ATTENDANCE));
        cache.get(seasonQuery()); drain(); var stats = start();
        var result = service.getParticipations(auth(u));
        assertEquals(11, result.getPoints().size()); assertEquals(51, result.getTotalPoint());
        assertEquals(4, stats.getPrepareStatementCount());
        System.out.println("OPTIMIZED activity: points=11 distinctPosts=10 SQL=4 (ranking warm)");
    }
}
