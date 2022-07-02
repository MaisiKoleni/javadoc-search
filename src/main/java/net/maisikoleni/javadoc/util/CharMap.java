package net.maisikoleni.javadoc.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
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

		@Deprecated
		@Override
		public Set<Entry<Character, Object>> entrySet() {
			return Collections.EMPTY_SET;
		}
	};

	int size();

	boolean containsKey(char c);

	V get(char c);

	V put(char c, V value);

	@Deprecated()
	Set<Entry<Character, V>> entrySet();

	default void forEach(CharEntryConsumer<V> entryConsumer) {
		entrySet().forEach(e -> entryConsumer.accept(e.getKey(), e.getValue()));
	}

	default void forEachParallel(CharEntryConsumer<V> entryConsumer) {
		entrySet().stream().parallel().forEach(e -> entryConsumer.accept(e.getKey(), e.getValue()));
	}

	default <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
			MatchOperationWithContext<V, C> operation, C context) {
		var result = false;
		for (var transition : entrySet()) {
			long stateAfterKey = matcher.step(transition.getKey(), state);
			if (matcher.isOk(stateAfterKey))
				result |= operation.apply(matcher, stateAfterKey, transition.getValue(), context);
		}
		return result;
	}

	default int hashCodeParallel() {
		var hash = new AtomicInteger(size());
		forEachParallel((c, node) -> hash.addAndGet(node.hashCode() * 31 + c));
		return hash.get();
	}

	@FunctionalInterface
	interface CharEntryConsumer<V> {

		void accept(char c, V v);
	}

	@FunctionalInterface
	interface MatchOperationWithContext<V, C> {

		boolean apply(GradingLongStepMatcher matcher, long state, V value, C context);
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
	}

	class NettyCharObjectHashMap<V> extends io.netty.util.collection.CharObjectHashMap<V> implements CharMap<V> {

		@Override
		public int hashCode() {
			// This is overridden to include the value again, like HashMap
			int hash = 0;
			for (PrimitiveEntry<V> entry : entries())
				hash += Objects.hashCode(entry.value()) * 31 ^ entry.key();
			return hash;
		}
	}

	static <V> CharMap<V> newEmpty() {
		return new CharObjectHashMap<>();
	}
}