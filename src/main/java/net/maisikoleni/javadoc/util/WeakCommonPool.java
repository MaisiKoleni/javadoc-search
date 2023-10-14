package net.maisikoleni.javadoc.util;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is for resetting a custom common pool forcefully and have it GC'ed
 * (especially for Quarkus reloads)
 */
public final class WeakCommonPool {

	private static final Logger LOG = LoggerFactory.getLogger(WeakCommonPool.class);

	private static final Cleaner FORK_JOIN_POOL_CLEANER = Cleaner.create();
	private static WeakReference<WeakCommonPool> forkJoinPoolReference = new WeakReference<>(null);

	private final ForkJoinPool forkJoinPool;

	private WeakCommonPool(ForkJoinPool forkJoinPool) {
		this.forkJoinPool = Objects.requireNonNull(forkJoinPool);
		// forcefully shutdown the pool if it is not used
		FORK_JOIN_POOL_CLEANER.register(this, forkJoinPool::shutdownNow);
		LOG.debug("New WeakCommonPool created.");
	}

	public ForkJoinPool forkJoinPool() {
		return forkJoinPool;
	}

	@SuppressWarnings("resource")
	public static synchronized WeakCommonPool get() {
		var cachedPool = forkJoinPoolReference.get();
		if (cachedPool != null)
			return cachedPool;
		var newPool = createNewFlexibleForkJoinPool();
		var newWeakPool = new WeakCommonPool(newPool);
		forkJoinPoolReference = new WeakReference<>(newWeakPool);
		return newWeakPool;
	}

	private static ForkJoinPool createNewFlexibleForkJoinPool() {
		return new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism(),
				ForkJoinPool.defaultForkJoinWorkerThreadFactory,
				(t, e) -> LOG.warn("WeakCommonPool execution failed with uncaught exception", e), false, 16, 256, 4,
				null, 1, TimeUnit.MINUTES);
	}
}
