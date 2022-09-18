package net.maisikoleni.javadoc.util.trie;

import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.Cache;
import net.maisikoleni.javadoc.util.CharMap;
import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;
import net.maisikoleni.javadoc.util.FixKeyedCharMap;
import net.maisikoleni.javadoc.util.SingleElementSet;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public abstract class AbstractTrie<T, N extends AbstractTrie.AbstractNode<T, N>> implements Trie<T> {

	@SuppressWarnings("rawtypes")
	protected static final Set EMPTY_SET = Collections.EMPTY_SET; // $NOSONAR$ - raw type anyways
	@SuppressWarnings("rawtypes")
	protected static final CharMap EMPTY_MAP = CharMap.EMPTY_MAP;

	protected final AbstractTypeFactory<T> factory;
	protected final N root;
	protected boolean mutable = true;

	protected AbstractTrie(N root, AbstractTypeFactory<T> factory) {
		this.factory = Objects.requireNonNull(factory);
		this.root = Objects.requireNonNull(root);
	}

	protected abstract static class AbstractNode<T, N extends AbstractNode<T, N>> {

		protected CharSequence chars = "";
		protected Set<T> values = EMPTY_SET;
		protected CharMap<N> transitions = EMPTY_MAP;
		protected int hashCode;

		public final CharSequence chars() {
			return chars;
		}

		public final int charCount() {
			return chars.length();
		}

		public final Set<T> values() {
			return values;
		}

		public final Stream<T> valueStream() {
			return values.stream();
		}

		public final CharMap<N> transitions() {
			return transitions;
		}

		protected abstract N newNode();

		public void insert(CharSequence key, int indexInKey, int indexInNode, T value, AbstractTypeFactory<T> factory) {
			var isKeyEnd = key.length() == indexInKey;
			splitIfNecessary(indexInNode, factory);
			if (isKeyEnd)
				addValue(value, factory);
			else
				addTransitionToNewNode(key, indexInKey, value, factory);
		}

		private void splitIfNecessary(int indexInNode, AbstractTypeFactory<T> factory) {
			if (indexInNode == charCount())
				return;
			var node = newNode();
			// split strings and extract transition character
			var transitionChar = chars.charAt(indexInNode);
			node.chars = chars.subSequence(indexInNode + 1, chars.length());
			chars = chars.subSequence(0, indexInNode);
			// move transitions and values to the new node behind this one
			node.transitions = transitions;
			transitions = factory.newTransitionMap();
			node.values = values;
			values = EMPTY_SET;
			// create new transition
			transitions.put(transitionChar, node);
		}

		private void addTransitionToNewNode(CharSequence key, int indexInKey, T value, AbstractTypeFactory<T> factory) {
			// create a new node for the missing content
			var valueNode = newNode();
			var transitionChar = key.charAt(indexInKey);
			valueNode.chars = key.subSequence(indexInKey + 1, key.length());
			valueNode.values = SingleElementSet.of(value);
			// add node transition to this node (after potential split)
			if (transitions == CharMap.EMPTY_MAP)
				transitions = factory.newTransitionMap();
			transitions.put(transitionChar, valueNode);
		}

		private void addValue(T value, AbstractTypeFactory<T> factory) {
			// add value to this node (after potential split)
			if (values == EMPTY_SET) {
				values = SingleElementSet.of(value);
			} else {
				if (values.size() == 1)
					values = factory.newValueSet(values);
				values.add(value);
			}
		}

		public boolean search(GradingLongStepMatcher matcher, long startState,
				Consumer<GradedValueSet<T>> resultConsumer) {
			long result = matcher.stepThrough(chars, startState);
			if (!matcher.isOk(result))
				return false;
			boolean hasMatch = matcher.isMatch(result) && values != EMPTY_SET;
			if (hasMatch) {
				double grade = matcher.grade(result);
				resultConsumer.accept(new GradedValueSet<>(values, grade));
			}
			if (transitions == EMPTY_MAP)
				return hasMatch;
			boolean childrenMatched = transitions.stepAllAndAdvanceWithContext(matcher, result, N::search,
					resultConsumer);
			return hasMatch || childrenMatched;
		}

		protected void compress(CompressionCache cache) {
			cacheSelf(cache.nodes());
			chars = cache.keySegments().getOrCache(chars);
			values = cache.<T>valueSets().getOrCache(values);
			// template methods to allow for parallel computation
			forEachTransition((c, node) -> {
				var nodeInCache = cache.<N>nodes().getOrCache(node);
				if (node != nodeInCache)
					updateTransitionTarget(c, nodeInCache);
				else
					node.compress(cache);
			});
		}

		protected final void compressTransitions() {
			transitions = FixKeyedCharMap.copyOf(transitions);
			forEachTransition((c, node) -> node.compressTransitions());
		}

		protected abstract void cacheSelf(Cache<N> nodeCache);

		protected void forEachTransition(CharEntryConsumer<N> action) {
			transitions.forEach(action);
		}

		protected void updateTransitionTarget(char character, N newValue) {
			transitions.put(character, newValue);
		}

		protected void startRead() {
			// subclasses can override
		}

		protected void endRead() {
			// subclasses can override
		}

		protected void startWrite() {
			// subclasses can override
		}

		protected void endWrite() {
			// subclasses can override
		}

		@Override
		public final int hashCode() {
			if (hashCode == 0) {
				hashCode = calculateHashCode();
				if (hashCode == 0)
					hashCode = 1;
			}
			return hashCode;
		}

		protected int calculateHashCode() {
			return Objects.hash(chars, values, transitions);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj == this)
				return true;
			return obj instanceof AbstractTrie.AbstractNode<?, ?> other && hashCode() == other.hashCode()
					&& chars.equals(other.chars) && values.equals(other.values)
					&& transitions.equals(other.transitions);
		}

		@Override
		public final String toString() {
			startRead();
			try {
				var result = new StringBuilder();
				result.append(getClass().getSimpleName());
				result.append("[\"");
				result.append(chars);
				result.append("\"] values: ");
				result.append(values);
				result.append('\n');
				transitions.forEach((c, node) -> result
						.append(node.toString().indent(8).replaceFirst(" ".repeat(7), " '" + c + "' ->")));
				return result.toString();
			} finally {
				endRead();
			}
		}
	}

	protected interface NodeMatch<T, N extends AbstractTrie.AbstractNode<T, N>> {

		boolean success();

		N node();

		int indexInNode();

		int indexInKey();

		default <R> R switchOnSuccess(Function<? super N, ? extends R> onSuccess, Supplier<? extends R> onFailure) {
			if (success())
				return onSuccess.apply(node());
			return onFailure.get();
		}

		default void insert(CharSequence key, T value, AbstractTypeFactory<T> factory) {
			node().insert(key, indexInKey(), indexInNode(), value, factory);
		}
	}

	protected record GradedValueSet<T> (Set<T> values, double grade) implements Comparable<GradedValueSet<T>> {

		protected GradedValueSet {
			Objects.requireNonNull(values);
		}

		@Override
		public int compareTo(GradedValueSet<T> o) {
			// reversed, as a higher grade is better
			return Double.compare(o.grade, grade);
		}
	}

	protected abstract NodeMatch<T, N> findNode(CharSequence key, boolean writeAccess);

	protected abstract static class AbstractTypeFactory<T> {

		protected abstract Set<T> newValueSet();

		protected Set<T> newValueSet(Set<T> values) {
			Set<T> newSet = newValueSet();
			newSet.addAll(values);
			return newSet;
		}

		protected abstract <N> CharMap<N> newTransitionMap();

		protected abstract <R> Cache<R> newCache();
	}

	@Override
	public Stream<T> search(CharSequence key) {
		return findNode(key, false).switchOnSuccess(N::valueStream, () -> Stream.of());
	}

	@Override
	public Stream<T> search(GradingLongStepMatcher matcher) {
		var results = new PriorityQueue<GradedValueSet<T>>();
		root.search(matcher, matcher.getStartState(), results::add);
		return results.stream().map(GradedValueSet::values).flatMap(Set::stream).distinct();
	}

	@Override
	public void insert(CharSequence key, T value) {
		if (!mutable)
			throw new IllegalStateException("Trie is immutable");
		findNode(key, true).insert(key, value, factory);
	}

	public final void compress() {
		compress(new CompressionCache(factory::newCache));
	}

	@Override
	public final void compress(CompressionCache compressionCache) {
		mutable = false;
		root.compressTransitions();
		root.compress(compressionCache);
	}

	@Override
	public final int hashCode() {
		return root.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this)
			return true;
		return obj instanceof AbstractTrie<?, ?> other && hashCode() == other.hashCode() && root.equals(other.root);
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + root.toString();
	}
}
