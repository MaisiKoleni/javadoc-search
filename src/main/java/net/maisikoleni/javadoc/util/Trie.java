package net.maisikoleni.javadoc.util;

import java.util.Set;

public interface Trie<T> extends PatternLookup<T> {

	void compress(CommonCompressionCache compressionCache);

	class CommonCompressionCache {

		private final Cache<?> nodes;
		private final Cache<Set<?>> valueSets;
		private final Cache<CharSequence> charSequences;

		public CommonCompressionCache(CacheSupplier cacheSupplier) {
			nodes = cacheSupplier.createNew();
			valueSets = cacheSupplier.createNew();
			charSequences = cacheSupplier.createNew();
		}

		@SuppressWarnings({ "unchecked" })
		public <T> Cache<T> nodes() {
			return (Cache<T>) nodes;
		}

		@SuppressWarnings({ "unchecked" })
		public <T> Cache<Set<T>> valueSets() {
			return (Cache<Set<T>>) (Cache<?>) valueSets;
		}

		public Cache<CharSequence> charSequences() {
			return charSequences;
		}

		public void clear() {
			nodes.clear();
			valueSets.clear();
			charSequences.clear();
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
