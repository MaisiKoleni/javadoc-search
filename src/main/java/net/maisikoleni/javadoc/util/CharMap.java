package net.maisikoleni.javadoc.util;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

	class JdkCharHashMap<V> extends HashMap<Character, V> implements CharMap<V> {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean containsKey(char c) {
			return super.containsKey(c);
		}

		@Override
		public V get(char c) {
			return super.get(c);
		}

		@Override
		public V put(char c, V value) {
			return super.put(c, value);
		}

		@Override
		public void forEach(CharEntryConsumer<V> entryConsumer) {
			super.entrySet().forEach(e -> entryConsumer.accept(e.getKey(), e.getValue()));
		}

		@Override
		public void forEachParallel(CharEntryConsumer<V> entryConsumer) {
			super.entrySet().stream().parallel().forEach(e -> entryConsumer.accept(e.getKey(), e.getValue()));
		}

		@Override
		public <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
				MatchOperationWithContext<V, C> operation, C context) {
			var result = false;
			for (var transition : super.entrySet()) {
				long stateAfterKey = matcher.step(transition.getKey(), state);
				if (matcher.isOk(stateAfterKey))
					result |= operation.apply(transition.getValue(), matcher, stateAfterKey, context);
			}
			return result;
		}

		@Override
		public int hashCodeParallel() {
			var hash = new AtomicInteger(super.size());
			forEachParallel((c, node) -> hash.addAndGet(node.hashCode() * 31 + c));
			return hash.get();
		}
	}

	static <V> CharMap<V> newEmpty() {
		return new CharObjectHashMap<>();
	}
}