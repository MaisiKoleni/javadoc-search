package net.maisikoleni.javadoc.util;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public interface CharMap<V> {

	@SuppressWarnings("rawtypes")
	CharMap EMPTY_MAP = EmptyCharMap.INSTANCE;

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