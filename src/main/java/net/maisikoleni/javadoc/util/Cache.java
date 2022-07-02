package net.maisikoleni.javadoc.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface Cache<T> {

	public int size();

	public boolean isEmpty();

	public boolean contains(T value);

	public T put(T value);

	public T getCached(T value);

	public T getOrCache(T value);

	public T remove(T value);

	public void clear();

	public static <T> Cache<T> newDefault() {
		return new DelegateMapCache<>(new HashMap<>());
	}

	public static <T> Cache<T> newConcurrent() {
		return new DelegateMapCache<>(new ConcurrentHashMap<>());
	}

	public static <T> Cache<T> newConcurrentWeak() {
		return new ConcurrentWeakCache<>();
	}

	public static <T> Cache<T> newMapBased(Map<T, T> map) {
		return new DelegateMapCache<>(map);
	}

	public static final class DelegateMapCache<T> implements Cache<T> {

		private final Map<T, T> map;

		private DelegateMapCache(Map<T, T> map) {
			this.map = Objects.requireNonNull(map);
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public boolean contains(T value) {
			return map.containsKey(value);
		}

		@Override
		public T getCached(T value) {
			return map.get(value);
		}

		@Override
		public T put(T value) {
			return map.put(value, value);
		}

		@Override
		public T getOrCache(T value) {
			var inCache = map.putIfAbsent(value, value);
			return inCache == null ? value : inCache;
		}

		@Override
		public T remove(T value) {
			return map.remove(value);
		}

		@Override
		public void clear() {
			map.clear();
		}
	}

	public static final class ConcurrentWeakCache<T> implements Cache<T> {

		private final WeakHashMap<T, WeakReference<T>> map = new WeakHashMap<>();
		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

		@Override
		public int size() {
			lock.readLock().lock();
			try {
				return map.size();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public boolean isEmpty() {
			lock.readLock().lock();
			try {
				return map.isEmpty();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public boolean contains(T value) {
			lock.readLock().lock();
			try {
				return map.containsKey(value);
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public T getCached(T value) {
			lock.readLock().lock();
			try {
				return tryGet(map.get(value));
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public T put(T value) {
			lock.writeLock().lock();
			try {
				return tryGet(map.put(value, new WeakReference<>(value)));
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public T getOrCache(T value) {
			var cachedValue = getCached(value);
			if (cachedValue != null)
				return cachedValue;
			lock.writeLock().lock();
			try {
				return tryGet(map.computeIfAbsent(value, WeakReference::new));
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public T remove(T value) {
			lock.writeLock().lock();
			try {
				return tryGet(map.remove(value));
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public void clear() {
			lock.writeLock().lock();
			try {
				map.clear();
			} finally {
				lock.writeLock().unlock();
			}
		}

		private static <T> T tryGet(WeakReference<T> ref) {
			return ref == null ? null : ref.get();
		}
	}
}
