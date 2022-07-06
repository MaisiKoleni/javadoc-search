package net.maisikoleni.javadoc.util;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public final class JdkCharHashMap<V> extends HashMap<Character, V> implements CharMap<V> {

	private static final long serialVersionUID = 1L;

	public JdkCharHashMap() {
	}

	public JdkCharHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

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