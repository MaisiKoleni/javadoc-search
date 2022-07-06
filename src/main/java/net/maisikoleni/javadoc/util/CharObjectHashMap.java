/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified 2022 by Christian Femers:
 * - removed many unused methods and attributes to save space
 * - added specialized methods for matching and parallel computation
 * - include values in hash code calculation
 * - other small changes
 */

package net.maisikoleni.javadoc.util;

import static io.netty.util.internal.MathUtil.safeFindNextPositivePowerOfTwo;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import io.netty.util.collection.CharObjectMap;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

/**
 * A hash map implementation of {@link CharObjectMap} that uses open addressing
 * for keys. To minimize the memory footprint, this class uses open addressing
 * rather than chaining. Collisions are resolved using linear probing. Deletions
 * implement compaction, so cost of remove can approach O(N) for full maps,
 * which makes a small loadFactor recommended.
 *
 * @param <V> The value type stored in the map.
 */
public class CharObjectHashMap<V> implements CharMap<V> {

	/** Default initial capacity. Used if not specified in the constructor */
	public static final int DEFAULT_CAPACITY = 8;

	/** Default load factor. Used if not specified in the constructor */
	public static final float DEFAULT_LOAD_FACTOR = 0.5f;

	/** The maximum number of elements allowed without allocating more space. */
	private int maxSize;

	/** The load factor for the map. Used to calculate {@link #maxSize}. */
	private final float loadFactor;

	private char[] keys;
	private V[] values;
	private int size;
	private int mask;

	public CharObjectHashMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public CharObjectHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public CharObjectHashMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0.0f || loadFactor > 1.0f) {
			// Cannot exceed 1 because we can never store more than capacity elements;
			// using a bigger loadFactor would trigger rehashing before the desired load is
			// reached.
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
		}

		this.loadFactor = loadFactor;

		// Adjust the initial capacity if necessary.
		int capacity = safeFindNextPositivePowerOfTwo(initialCapacity);
		mask = capacity - 1;

		// Allocate the arrays.
		keys = new char[capacity];
		@SuppressWarnings({ "unchecked" })
		V[] temp = (V[]) new Object[capacity];
		values = temp;

		// Initialize the maximum size value.
		maxSize = calcMaxSize(capacity);
	}

	@Override
	public V get(char key) {
		int index = indexOf(key);
		return index == -1 ? null : values[index];
	}

	@Override
	public V put(char key, V value) {
		Objects.requireNonNull(value);
		int startIndex = hashIndex(key);
		int index = startIndex;

		for (;;) {
			if (values[index] == null) {
				// Found empty slot, use it.
				keys[index] = key;
				values[index] = value;
				growSize();
				return null;
			}
			if (keys[index] == key) {
				// Found existing entry with this key, just replace the value.
				V previousValue = values[index];
				values[index] = value;
				return previousValue;
			}

			// Conflict, keep probing ...
			if ((index = probeNext(index)) == startIndex) {
				// Can only happen if the map was full at MAX_ARRAY_SIZE and couldn't grow.
				throw new IllegalStateException("Unable to insert");
			}
		}
	}

	@Override
	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void clear() {
		Arrays.fill(keys, (char) 0);
		Arrays.fill(values, null);
		size = 0;
	}

	@Override
	public boolean containsKey(char key) {
		return indexOf(key) >= 0;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < values.length; i++) {
			V value = values[i];
			if (value != null)
				hash += value.hashCode() * 31 + keys[i];
		}
		return hash;
	}

	@Override
	public int hashCodeParallel() {
		return IntStream.range(0, values.length).parallel().map(i -> {
			V value = values[i];
			if (value == null)
				return 0;
			return value.hashCode() * 31 + keys[i];
		}).sum();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CharMap<?> other)) {
			return false;
		}
		if (size != other.size()) {
			return false;
		}
		for (int i = 0; i < values.length; ++i) {
			V value = values[i];
			if (value != null) {
				char key = keys[i];
				Object otherValue = other.get(key);
				if (!value.equals(otherValue)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void forEach(CharEntryConsumer<V> entryConsumer) {
		for (int i = 0; i < values.length; i++) {
			V value = values[i];
			if (value != null)
				entryConsumer.accept(keys[i], value);
		}
	}

	@Override
	public void forEachParallel(CharEntryConsumer<V> entryConsumer) {
		if (size < 2) {
			forEach(entryConsumer);
		} else {
			IntStream.range(0, values.length).parallel().forEach(i -> {
				var value = values[i];
				if (value != null)
					entryConsumer.accept(keys[i], value);
			});
		}
	}

	@Override
	public <C> boolean stepAllAndAdvanceWithContext(GradingLongStepMatcher matcher, long state,
			MatchOperationWithContext<V, C> operation, C context) {
		var result = false;
		for (int i = 0; i < values.length; i++) {
			V value = values[i];
			if (value != null) {
				var key = keys[i];
				long stateAfterKey = matcher.step(key, state);
				if (matcher.isOk(stateAfterKey))
					result |= operation.apply(value, matcher, stateAfterKey, context);
			}
		}
		return result;
	}

	/**
	 * Locates the index for the given key. This method probes using double hashing.
	 *
	 * @param key the key for an entry in the map.
	 * @return the index where the key was found, or {@code -1} if no entry is found
	 *         for that key.
	 */
	private int indexOf(char key) {
		int startIndex = hashIndex(key);
		int index = startIndex;

		for (;;) {
			if (values[index] == null) {
				// It's available, so no chance that this value exists anywhere in the map.
				return -1;
			}
			if (key == keys[index]) {
				return index;
			}

			// Conflict, keep probing ...
			if ((index = probeNext(index)) == startIndex) {
				return -1;
			}
		}
	}

	/**
	 * Returns the hashed index for the given key.
	 */
	private int hashIndex(char key) {
		// The array lengths are always a power of two, so we can use a bitmask to stay
		// inside the array bounds.
		return key & mask;
	}

	/**
	 * Get the next sequential index after {@code index} and wraps if necessary.
	 */
	private int probeNext(int index) {
		// The array lengths are always a power of two, so we can use a bitmask to stay
		// inside the array bounds.
		return (index + 1) & mask;
	}

	/**
	 * Grows the map size after an insertion. If necessary, performs a rehash of the
	 * map.
	 */
	private void growSize() {
		size++;

		if (size > maxSize) {
			if (keys.length == Integer.MAX_VALUE) {
				throw new IllegalStateException("Max capacity reached at size=" + size);
			}

			// Double the capacity.
			rehash(keys.length << 1);
		}
	}

	/**
	 * Calculates the maximum size allowed before rehashing.
	 */
	private int calcMaxSize(int capacity) {
		// Clip the upper bound so that there will always be at least one available
		// slot.
		int upperBound = capacity - 1;
		return Math.min(upperBound, (int) (capacity * loadFactor));
	}

	/**
	 * Rehashes the map for the given capacity.
	 *
	 * @param newCapacity the new capacity for the map.
	 */
	private void rehash(int newCapacity) {
		char[] oldKeys = keys;
		V[] oldVals = values;

		keys = new char[newCapacity];
		@SuppressWarnings({ "unchecked" })
		V[] temp = (V[]) new Object[newCapacity];
		values = temp;

		maxSize = calcMaxSize(newCapacity);
		mask = newCapacity - 1;

		// Insert to the new arrays.
		for (int i = 0; i < oldVals.length; ++i) {
			V oldVal = oldVals[i];
			if (oldVal != null) {
				// Inlined put(), but much simpler: we don't need to worry about
				// duplicated keys, growing/rehashing, or failing to insert.
				char oldKey = oldKeys[i];
				int index = hashIndex(oldKey);

				for (;;) {
					if (values[index] == null) {
						keys[index] = oldKey;
						values[index] = oldVal;
						break;
					}

					// Conflict, keep probing. Can wrap around, but never reaches startIndex again.
					index = probeNext(index);
				}
			}
		}
	}

	@Override
	public String toString() {
		if (isEmpty()) {
			return "{}";
		}
		StringBuilder sb = new StringBuilder(4 * size);
		sb.append('{');
		boolean first = true;
		for (int i = 0; i < values.length; ++i) {
			V value = values[i];
			if (value != null) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(keyToString(keys[i])).append('=').append(value == this ? "(this Map)" : value);
				first = false;
			}
		}
		return sb.append('}').toString();
	}

	/**
	 * Helper method called by {@link #toString()} in order to convert a single map
	 * key into a string. This is protected to allow subclasses to override the
	 * appearance of a given key.
	 */
	protected static String keyToString(char key) {
		return Character.toString(key);
	}
}
