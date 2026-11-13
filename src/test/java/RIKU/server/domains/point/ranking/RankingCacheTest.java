package RIKU.server.domains.point.ranking;

import RIKU.server.domains.point.dto.response.ReadUserPointResponse;
import RIKU.server.domains.point.entity.PointType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingCacheTest {
    private final RankingQuery query = RankingQuery.season(LocalDateTime.of(2026, 1, 1, 0, 0));

    private RankingSnapshot snapshot(int points) {
        return new RankingSnapshot(List.of(ReadUserPointResponse.of(100L, null, "회원", points)));
    }

    @Test void hiddenAccountsOnlyAffectRankingTab() {
        for (long id : List.of(231L, 331L, 412L))
            assertTrue(new RankingMember(id, "동명이인", null).hiddenFromRankingTab());
        assertFalse(new RankingMember(1L, "동명이인", null).hiddenFromRankingTab());
        var snapshot = new RankingSnapshot(List.of(ReadUserPointResponse.of(1L, null, "가", 10),
                ReadUserPointResponse.of(2L, null, "나", 5)), Set.of(1L));
        assertNull(snapshot.forUser(1L).getUserPoints());
        assertEquals(List.of(2L), snapshot.forUser(1L).getTop20().stream()
                .map(ReadUserPointResponse::getUserId).toList());
        assertEquals(1, snapshot.forUser(2L).getUserRanking());
        assertEquals(2, snapshot.activityRank(2L));
    }

    @Test void cacheIsSharedButCurrentUserIsNot() {
        var loader = mock(RankingSnapshotLoader.class);
        var cached = new RankingSnapshot(List.of(ReadUserPointResponse.of(100L, null, "가", 10),
                ReadUserPointResponse.of(101L, null, "나", 5)));
        when(loader.load(query)).thenReturn(cached);
        var cache = new RankingCache(loader, runnable -> {});
        assertEquals(1, cache.get(query).forUser(100L).getUserRanking());
        assertEquals(2, cache.get(query).forUser(101L).getUserRanking());
        assertNull(cache.get(query).forUser(10L).getUserPoints());
        assertEquals(-1, cache.get(query).forUser(10L).getUserRanking());
        verify(loader, times(1)).load(query);
    }

    @Test void typeOrderDoesNotChangeCacheKey() {
        var start = query.start();
        var one = new RankingQuery(start, start.plusDays(1), Set.of(PointType.ADD_ATTENDANCE, PointType.ADD_FLASH_JOIN));
        var two = new RankingQuery(start, start.plusDays(1), Set.of(PointType.ADD_FLASH_JOIN, PointType.ADD_ATTENDANCE));
        assertEquals(one, two);
        assertNotEquals(one, new RankingQuery(start, start.plusDays(2), one.pointTypes()));
        assertNotEquals(one, new RankingQuery(start, one.endExclusive(), Set.of(PointType.ADD_ATTENDANCE)));
    }

    @Test void changesAreCoalescedAndRecentQueriesArePrecomputed() {
        var loader = mock(RankingSnapshotLoader.class);
        var initial = snapshot(0); var updated = snapshot(1);
        when(loader.load(query)).thenReturn(initial, updated);
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add);
        assertSame(initial, cache.get(query));
        for (int i = 0; i < 100; i++) cache.onRankingChanged(RankingChanged.members());
        assertEquals(1, queue.size());
        queue.remove().run();
        assertSame(updated, cache.get(query));
        verify(loader, times(2)).load(query);
    }

    @Test void unchangedSnapshotSurvivesThirtySecondsAndLongInactivity() {
        var loader = mock(RankingSnapshotLoader.class);
        var original = snapshot(0);
        when(loader.load(query)).thenReturn(original);
        var time = new AtomicLong();
        var cache = new RankingCache(loader, runnable -> fail("time must not schedule refresh"), time::get);
        assertSame(original, cache.get(query));
        time.set(Duration.ofSeconds(31).toNanos());
        assertSame(original, cache.get(query));
        time.set(Duration.ofDays(30).toNanos());
        assertSame(original, cache.get(query));
        verify(loader, times(1)).load(query);
    }

    @Test void inactiveQueriesAreNotPrecomputedButMustRefreshOnTheirNextRead() {
        var loader = mock(RankingSnapshotLoader.class);
        var initial = snapshot(0); var updated = snapshot(1);
        when(loader.load(query)).thenReturn(initial, updated);
        var time = new AtomicLong();
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add, time::get);
        assertSame(initial, cache.get(query));
        time.set(Duration.ofMinutes(6).toNanos());
        for (int i = 0; i < 100; i++) {
            cache.onRankingChanged(RankingChanged.members());
            assertTrue(queue.isEmpty());
        }
        verify(loader, times(1)).load(query);
        assertSame(updated, cache.get(query));
        verify(loader, times(2)).load(query);
    }

    @Test void readOnlyWorkloadAvoidsRepeatedThirtySecondRebuilds() {
        var time = new AtomicLong();
        var loader = mock(RankingSnapshotLoader.class);
        var result = snapshot(0);
        when(loader.load(query)).thenReturn(result);
        var cache = new RankingCache(loader, runnable -> fail("no writes"), time::get);
        Cache<RankingQuery, RankingSnapshot> previousTtlPolicy = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30)).ticker(time::get).build();
        var ttlRebuilds = new AtomicInteger();
        for (int second = 0; second < 120; second++) {
            time.set(Duration.ofSeconds(second).toNanos());
            assertSame(result, cache.get(query));
            previousTtlPolicy.get(query, ignored -> { ttlRebuilds.incrementAndGet(); return result; });
        }
        assertEquals(4, ttlRebuilds.get());
        verify(loader, times(1)).load(query);
        System.out.println("CACHE workload: 120 reads over 120 seconds, no writes; TTL30 rebuilds=4, change-only rebuilds=1");
    }

    @Test void separateChangesStillHaveARefreshCostWhileTheQueryIsActive() {
        var loader = mock(RankingSnapshotLoader.class);
        when(loader.load(query)).thenReturn(snapshot(0));
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add, () -> 0L);
        cache.get(query);
        for (int i = 0; i < 100; i++) {
            cache.onRankingChanged(RankingChanged.members());
            queue.remove().run();
        }
        verify(loader, times(101)).load(query);
        System.out.println("CACHE workload: 1 initial read, 100 separate active changes; rebuilds=101 (including initial)");
    }

    @Test void rejectedRefreshIsRetriedByRead() {
        var loader = mock(RankingSnapshotLoader.class);
        when(loader.load(query)).thenReturn(snapshot(0), snapshot(1));
        var cache = new RankingCache(loader, runnable -> { throw new RejectedExecutionException("test"); });
        cache.get(query);
        assertDoesNotThrow(() -> cache.onRankingChanged(RankingChanged.members()));
        assertEquals(1, cache.get(query).forUser(100L).getUserPoints().getTotalPoints());
        verify(loader, times(2)).load(query);
    }

    @Test void obsoleteGenerationsDoNotRetainSnapshotsAfterRefresh() {
        var loader = mock(RankingSnapshotLoader.class);
        when(loader.load(query)).thenAnswer(ignored -> snapshot(0));
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add, () -> 0L);
        cache.get(query);
        for (int i = 0; i < 20; i++) {
            cache.onRankingChanged(RankingChanged.members());
            queue.remove().run();
        }
        var retained = (Cache<?, ?>) ReflectionTestUtils.getField(cache, "entries");
        assertNotNull(retained);
        retained.cleanUp();
        assertEquals(1, retained.estimatedSize(), "only the current result should remain resident");
        cache.get(query);
        verify(loader, times(21)).load(query);
    }

    @Test void oldInFlightCalculationCannotRestoreStaleSnapshot() throws Exception {
        var loader = mock(RankingSnapshotLoader.class);
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var calls = new AtomicInteger();
        when(loader.load(query)).thenAnswer(inv -> {
            if (calls.incrementAndGet() == 1) {
                entered.countDown(); assertTrue(finish.await(5, TimeUnit.SECONDS)); return snapshot(0);
            }
            return snapshot(1);
        });
        var cache = new RankingCache(loader, runnable -> {});
        var executor = Executors.newSingleThreadExecutor();
        try {
            var read = executor.submit(() -> cache.get(query));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            cache.onRankingChanged(RankingChanged.members());
            finish.countDown();
            assertEquals(1, read.get(5, TimeUnit.SECONDS).forUser(100L).getUserPoints().getTotalPoints());
            assertEquals(1, cache.get(query).forUser(100L).getUserPoints().getTotalPoints());
            assertEquals(2, calls.get());
        } finally { finish.countDown(); executor.shutdownNow(); }
    }

    @Test void concurrentMissesShareOneCalculation() throws Exception {
        var loader = mock(RankingSnapshotLoader.class);
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        when(loader.load(query)).thenAnswer(inv -> {
            entered.countDown(); assertTrue(finish.await(5, TimeUnit.SECONDS)); return snapshot(1);
        });
        var cache = new RankingCache(loader, runnable -> {});
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> cache.get(query));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> cache.get(query));
            finish.countDown();
            assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            verify(loader, times(1)).load(query);
        } finally { finish.countDown(); executor.shutdownNow(); }
    }

    @Test void failedRefreshIsRetriedByRead() {
        var loader = mock(RankingSnapshotLoader.class);
        when(loader.load(query)).thenReturn(snapshot(0)).thenThrow(new IllegalStateException("test failure")).thenReturn(snapshot(1));
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add);
        cache.get(query); cache.onRankingChanged(RankingChanged.members());
        assertDoesNotThrow(() -> queue.remove().run());
        assertEquals(1, cache.get(query).forUser(100L).getUserPoints().getTotalPoints());
    }

    @Test void tiesZeroAndNegativePointsUseCompetitionRanking() {
        var snapshot = new RankingSnapshot(List.of(ReadUserPointResponse.of(1L, null, "a", 0),
                ReadUserPointResponse.of(2L, null, "b", 0), ReadUserPointResponse.of(3L, null, "c", -1)));
        assertEquals(1, snapshot.forUser(1L).getUserRanking());
        assertEquals(1, snapshot.forUser(2L).getUserRanking());
        assertEquals(3, snapshot.forUser(3L).getUserRanking());
    }

    private RankingChanged pointChange(LocalDateTime time, PointType type) {
        return new RankingChanged(false, Set.of(new RankingChanged.PointChange(time, type)));
    }

    @Test void pointChangesRefreshOnlyMatchingDatesAndTypesButMembershipRefreshesAll() {
        var loader = mock(RankingSnapshotLoader.class);
        when(loader.load(any())).thenAnswer(ignored -> snapshot(0));
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add);
        var day = query.start().plusDays(10);
        var attendance = new RankingQuery(day, day.plusDays(1), Set.of(PointType.ADD_ATTENDANCE));
        var otherType = new RankingQuery(day, day.plusDays(1), Set.of(PointType.ADD_FLASH_JOIN));
        var past = new RankingQuery(day.minusDays(1), day, attendance.pointTypes());
        var keys = List.of(query, attendance, otherType, past);
        var before = keys.stream().map(cache::get).toList();
        cache.onRankingChanged(pointChange(day, PointType.ADD_ATTENDANCE));
        queue.remove().run();
        for (int i = 0; i < keys.size(); i++) {
            if (i < 2) assertNotSame(before.get(i), cache.get(keys.get(i)));
            else assertSame(before.get(i), cache.get(keys.get(i)));
            verify(loader, times(i < 2 ? 2 : 1)).load(keys.get(i));
        }
        cache.onRankingChanged(RankingChanged.members());
        queue.remove().run();
        for (int i = 0; i < keys.size(); i++) verify(loader, times(i < 2 ? 3 : 2)).load(keys.get(i));
    }

    @Test void matchingUsesInclusiveStartExclusiveEndAndAllSeasonTypes() {
        var day = query.start();
        var event = new RankingQuery(day, day.plusDays(1), Set.of(PointType.ADD_ATTENDANCE));
        assertFalse(pointChange(day.minusNanos(1), PointType.ADD_ATTENDANCE).affects(event));
        assertTrue(pointChange(day, PointType.ADD_ATTENDANCE).affects(event));
        assertTrue(pointChange(event.endExclusive().minusNanos(1), PointType.ADD_ATTENDANCE).affects(event));
        assertFalse(pointChange(event.endExclusive(), PointType.ADD_ATTENDANCE).affects(event));
        assertFalse(pointChange(day, PointType.REMOVE).affects(event));
        assertTrue(pointChange(day, PointType.REMOVE).affects(query));
        assertFalse(pointChange(day.minusNanos(1), PointType.REMOVE).affects(query));
        assertFalse(pointChange(null, PointType.REMOVE).affects(query));
        assertTrue(pointChange(day, null).affects(query));
        assertFalse(pointChange(day, null).affects(event));
        assertFalse(pointChange(day, PointType.ADD_ATTENDANCE).affects(new RankingQuery(day, day.plusDays(1), Set.of())));
        assertFalse(new RankingChanged(false, Set.of()).affects(query));
    }

    @Test void irrelevantCommitDoesNotRetryAnInFlightCalculation() throws Exception {
        var loader = mock(RankingSnapshotLoader.class);
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var result = snapshot(0);
        when(loader.load(query)).thenAnswer(ignored -> {
            entered.countDown(); assertTrue(finish.await(5, TimeUnit.SECONDS)); return result;
        });
        var queue = new ArrayDeque<Runnable>();
        var cache = new RankingCache(loader, queue::add);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var read = executor.submit(() -> cache.get(query));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            cache.onRankingChanged(pointChange(query.start().minusDays(1), PointType.ADD_ATTENDANCE));
            assertTrue(queue.isEmpty());
            finish.countDown();
            assertSame(result, read.get(5, TimeUnit.SECONDS));
            verify(loader, times(1)).load(query);
        } finally { finish.countDown(); executor.shutdownNow(); }
    }

    @Test void evictedInFlightEntryStillObservesRelevantCommit() throws Exception {
        var loader = mock(RankingSnapshotLoader.class);
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var calls = new AtomicInteger();
        when(loader.load(query)).thenAnswer(ignored -> {
            if (calls.incrementAndGet() == 1) {
                entered.countDown(); assertTrue(finish.await(5, TimeUnit.SECONDS)); return snapshot(0);
            }
            return snapshot(1);
        });
        var cache = new RankingCache(loader, runnable -> {});
        var executor = Executors.newSingleThreadExecutor();
        try {
            var read = executor.submit(() -> cache.get(query));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var entries = (Cache<?, ?>) ReflectionTestUtils.getField(cache, "entries");
            assertNotNull(entries); entries.invalidateAll(); entries.cleanUp();
            cache.onRankingChanged(pointChange(query.start(), PointType.ADD_ATTENDANCE));
            finish.countDown();
            assertEquals(1, read.get(5, TimeUnit.SECONDS).forUser(100L).getUserPoints().getTotalPoints());
            assertEquals(2, calls.get());
            assertTrue(((java.util.Map<?, ?>) ReflectionTestUtils.getField(cache, "readers")).isEmpty());
        } finally { finish.countDown(); executor.shutdownNow(); }
    }
}
