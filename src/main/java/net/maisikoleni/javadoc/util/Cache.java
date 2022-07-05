package net.maisikoleni.javadoc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
}