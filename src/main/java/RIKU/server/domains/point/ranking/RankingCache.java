package RIKU.server.domains.point.ranking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Duration;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RankingCache {
    private static final long PREWARM_WINDOW = Duration.ofMinutes(5).toNanos();
    private final RankingSnapshotLoader loader;
    private final Executor refreshExecutor;
    private final Ticker ticker;
    private final Cache<RankingQuery, Entry> entries;
    private final Object registryLock = new Object();
    // Evicted entries may still be loading. Keep them visible to changes until their readers finish.
    private final Map<Entry, Integer> readers = new IdentityHashMap<>();
    private final AtomicLong changes = new AtomicLong();
    private final AtomicBoolean refreshQueued = new AtomicBoolean();

    @Autowired
    public RankingCache(RankingSnapshotLoader loader, @Qualifier("rankingRefreshExecutor") Executor refreshExecutor) {
        this(loader, refreshExecutor, Ticker.systemTicker());
    }

    RankingCache(RankingSnapshotLoader loader, Executor refreshExecutor, Ticker ticker) {
        this.loader = loader;
        this.refreshExecutor = refreshExecutor;
        this.ticker = ticker;
        entries = Caffeine.newBuilder().maximumSize(32).ticker(ticker).build();
    }

    public RankingSnapshot get(RankingQuery query) {
        Entry entry;
        synchronized (registryLock) {
            // Only metadata creation runs under this lock; never database work or waiting on a load.
            entry = entries.get(query, Entry::new);
            entry.lastRequested = ticker.read();
            readers.merge(entry, 1, Integer::sum);
        }
        try {
            return loadCurrent(entry);
        } finally {
            release(entry);
        }
    }

    private RankingSnapshot loadCurrent(Entry entry) {
        while (true) {
            var cached = entry.snapshot;
            if (cached != null && cached.generation() == entry.generation) return cached.value();
            synchronized (entry) {
                long generation = entry.generation;
                cached = entry.snapshot;
                if (cached != null && cached.generation() == generation) return cached.value();
                // Same-condition requests share a calculation. Invalidation never takes this monitor.
                var loaded = loader.load(entry.query);
                if (generation == entry.generation) {
                    entry.snapshot = new VersionedSnapshot(generation, loaded);
                    if (generation == entry.generation) return loaded;
                }
                // A relevant commit during loading must not restore an obsolete result.
            }
        }
    }

    private void release(Entry entry) {
        synchronized (registryLock) {
            readers.computeIfPresent(entry, (ignored, count) -> count == 1 ? null : count - 1);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRankingChanged(RankingChanged event) {
        boolean prewarm = false;
        synchronized (registryLock) {
            var observed = new HashSet<>(entries.asMap().values());
            observed.addAll(readers.keySet());
            long now = ticker.read();
            for (var entry : observed) {
                if (!event.affects(entry.query)) continue;
                entry.generation++;
                entry.snapshot = null;
                prewarm |= now - entry.lastRequested < PREWARM_WINDOW;
            }
            if (prewarm) changes.incrementAndGet();
        }
        if (prewarm) queueRefresh();
    }

    private void queueRefresh() {
        if (!refreshQueued.compareAndSet(false, true)) return;
        try {
            refreshExecutor.execute(this::refreshRecentQueries);
        } catch (RuntimeException e) {
            refreshQueued.set(false);
            log.warn("Ranking refresh could not be scheduled; next read will rebuild", e);
        }
    }

    private void refreshRecentQueries() {
        long observedChanges = changes.get();
        List<Entry> candidates;
        synchronized (registryLock) {
            candidates = List.copyOf(entries.asMap().values());
        }
        try {
            for (var entry : candidates) {
                synchronized (registryLock) {
                    if (entries.getIfPresent(entry.query) != entry || ticker.read() - entry.lastRequested >= PREWARM_WINDOW) continue;
                    readers.merge(entry, 1, Integer::sum);
                }
                try {
                    loadCurrent(entry);
                } catch (RuntimeException e) {
                    // A failed condition must not prevent other changed conditions from being warmed.
                    log.warn("Ranking refresh failed; next read will rebuild", e);
                } finally {
                    release(entry);
                }
            }
        } finally {
            refreshQueued.set(false);
            if (observedChanges != changes.get()) queueRefresh();
        }
    }

    private static final class Entry {
        private final RankingQuery query;
        private long lastRequested; // registryLock
        private volatile long generation;
        private volatile VersionedSnapshot snapshot;

        private Entry(RankingQuery query) { this.query = query; }
    }

    private record VersionedSnapshot(long generation, RankingSnapshot value) {}
}
