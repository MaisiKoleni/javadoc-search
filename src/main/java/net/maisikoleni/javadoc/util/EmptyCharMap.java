package net.maisikoleni.javadoc.util;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

final class EmptyCharMap<T> implements CharMap<T> {

	@SuppressWarnings("rawtypes")
	static final EmptyCharMap INSTANCE = new EmptyCharMap<>();

	private EmptyCharMap() {
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean containsKey(char c) {
		return false;
	}

	@Override
	public T get(char c) {
		return null;
	}

	@Override
	public T put(char c, Object value) {
		throw new UnsupportedOperationException("Cannot put into EMPTY_MAP");
	}

	@Override
	public void forEach(CharEntryConsumer<T> entryConsumer) {
		// do nothing
	}

	@Override
	public void forEachParallel(CharEntryConsumer<T> entryConsumer) {
		// do nothing
	}

	@Override
	public <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
			MatchOperationWithContext<T, C> operation, C context) {
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
		return obj instanceof CharMap<?> cm && cm.size() == 0;
	}
}