package net.maisikoleni.javadoc.util;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public interface CharMap<V> {

	@SuppressWarnings("rawtypes")
	CharMap EMPTY_MAP = new CharMap<>() {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean containsKey(char c) {
			return false;
		}

		@Override
		public Object get(char c) {
			return null;
		}

		@Override
		public Object put(char c, Object value) {
			throw new UnsupportedOperationException("Cannot put into EMPTY_MAP");
		}

		@Override
		public void forEach(CharEntryConsumer<Object> entryConsumer) {
			// do nothing
		}

		@Override
		public void forEachParallel(CharEntryConsumer<Object> entryConsumer) {
			// do nothing
		}

		@Override
		public <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
				MatchOperationWithContext<Object, C> operation, C context) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public int hashCodeParallel() {
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			return obj instanceof CharMap cm && cm.size() == 0;
		}
	};

	int size();

	boolean containsKey(char c);

	V get(char c);

	V put(char c, V value);

	void forEach(CharEntryConsumer<V> entryConsumer);

	void forEachParallel(CharEntryConsumer<V> entryConsumer);

	<C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
			MatchOperationWithContext<V, C> operation, C context);

	int hashCodeParallel();

	@FunctionalInterface
	interface CharEntryConsumer<V> {

		void accept(char c, V v);
	}

	@FunctionalInterface
	interface MatchOperationWithContext<V, C> {

		boolean apply(V value, GradingLongStepMatcher matcher, long state, C context);
	}
}