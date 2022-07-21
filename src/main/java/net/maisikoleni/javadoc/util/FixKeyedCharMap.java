package net.maisikoleni.javadoc.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public class FixKeyedCharMap<V> implements CharMap<V> {

	private static final int BITS_16 = 16;
	private static final int BITS_16_MASK = (1 << BITS_16) - 1;

	private static final int LINEAR_SEARCH_THRESHOLD = 8;

	private final char[] keys;
	private final V[] values;

	private FixKeyedCharMap(char[] keys, V[] values) {
		this.keys = keys;
		this.values = values;
	}

	@Override
	public int size() {
		return keys.length;
	}

	private int find(char c) {
		if (size() <= LINEAR_SEARCH_THRESHOLD) {
			for (int i = 0; i < keys.length; i++) {
				if (c == keys[i])
					return i;
			}
			return -1;
		}
		return Arrays.binarySearch(keys, c);
	}

	@Override
	public boolean containsKey(char c) {
		return find(c) >= 0;
	}

	@Override
	public V get(char c) {
		int index = find(c);
		if (index < 0)
			return null;
		return values[index];
	}

	@Override
	public V put(char c, V value) {
		int index = find(c);
		if (index < 0)
			throw new UnsupportedOperationException("insertion of new keys not allowed");
		var old = values[index];
		values[index] = Objects.requireNonNull(value);
		return old;
	}

	@Override
	public void forEach(CharEntryConsumer<V> entryConsumer) {
		for (int i = 0; i < keys.length; i++) {
			entryConsumer.accept(keys[i], values[i]);
		}
	}

	@Override
	public void forEachParallel(CharEntryConsumer<V> entryConsumer) {
		IntStream.range(0, keys.length).parallel().forEach(i -> entryConsumer.accept(keys[i], values[i]));
	}

	@Override
	public <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
			MatchOperationWithContext<V, C> operation, C context) {
		var result = false;
		for (int i = 0; i < values.length; i++) {
			long stateAfterKey = matcher.step(keys[i], state);
			if (matcher.isOk(stateAfterKey))
				result |= operation.apply(values[i], matcher, stateAfterKey, context);
		}
		return result;
	}

	@Override
	public int hashCodeParallel() {
		return IntStream.range(0, keys.length).parallel().map(i -> values[i].hashCode() * 31 + keys[i]).sum();
	}

	public static <V> CharMap<V> copyOf(CharMap<V> cm) {
		int size = cm.size();
		if (size == 0)
			return CharMap.EMPTY_MAP;
		assert size < 1 << BITS_16 : "Cannot create fix keyed map with 2^16 keys or more";
		int[] indexedKeys = new int[size];
		V[] unsortedValues = newValueArray(size);
		Int index = new Int();
		cm.forEach((c, v) -> {
			int i = index.value;
			indexedKeys[i] = (c << BITS_16) | i;
			unsortedValues[i] = Objects.requireNonNull(v);
			index.value++;
		});
		Arrays.sort(indexedKeys);
		char[] keys = new char[size];
		V[] values = newValueArray(size);
		for (int i = 0; i < size; i++) {
			int indexedKey = indexedKeys[i];
			keys[i] = (char) (indexedKey >>> BITS_16);
			values[i] = unsortedValues[indexedKey & BITS_16_MASK];
		}
		return new FixKeyedCharMap<>(keys, values);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < keys.length; i++) {
			hash += values[i].hashCode() * 31 + keys[i];
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof FixKeyedCharMap<?> fkcm)
			return Arrays.equals(keys, fkcm.keys) && Arrays.equals(values, fkcm.values);
		if (!(obj instanceof CharMap<?> cm))
			return false;
		if (size() != cm.size())
			return false;
		for (int i = 0; i < keys.length; i++) {
			if (!values[i].equals(cm.get(keys[i])))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return IntStream.range(0, keys.length).mapToObj(i -> keys[i] + "=" + values[i])
				.collect(Collectors.joining(", ", "{", "}"));
	}

	@SuppressWarnings("unchecked")
	private static <V> V[] newValueArray(int size) {
		return (V[]) new Object[size];
	}

	private static final class Int {
		private int value;
	}
}