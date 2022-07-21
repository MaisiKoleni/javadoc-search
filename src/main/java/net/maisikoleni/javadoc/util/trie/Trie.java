package net.maisikoleni.javadoc.util.trie;

import java.util.Set;

import net.maisikoleni.javadoc.util.Cache;

public interface Trie<T> extends PatternLookup<T> {

	void compress(CompressionCache compressionCache);

	class CompressionCache {

		private final Cache<?> nodes;
		private final Cache<Set<?>> valueSets;
		private final Cache<CharSequence> keySegments;

		public CompressionCache(CacheSupplier cacheSupplier) {
			nodes = cacheSupplier.createNew();
			valueSets = cacheSupplier.createNew();
			keySegments = cacheSupplier.createNew();
		}

		@SuppressWarnings({ "unchecked" })
		public <T> Cache<T> nodes() {
			return (Cache<T>) nodes;
		}

		@SuppressWarnings({ "unchecked" })
		public <T> Cache<Set<T>> valueSets() {
			return (Cache<Set<T>>) (Cache<?>) valueSets;
		}

		public Cache<CharSequence> keySegments() {
			return keySegments;
		}

		public void clear() {
			nodes.clear();
			valueSets.clear();
			keySegments.clear();
		}
	}

	@FunctionalInterface
	interface CacheSupplier {
		/**
		 * Creates a new cache for the type <code>T</code>.
		 */
		<T> Cache<T> createNew();
	}
}
