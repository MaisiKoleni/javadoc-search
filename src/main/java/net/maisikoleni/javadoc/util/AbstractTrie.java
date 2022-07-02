package net.maisikoleni.javadoc.util;

import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public abstract class AbstractTrie<T, N extends AbstractTrie.AbstractNode<T, N, C>, C extends AbstractTrie<T, N, C>>
		implements Trie<T> {

	@SuppressWarnings("rawtypes")
	protected static final Set EMPTY_SET = Collections.EMPTY_SET;
	@SuppressWarnings("rawtypes")
	protected static final CharMap EMPTY_MAP = CharMap.EMPTY_MAP;

	protected final AbstractTypeFactory<T> factory;
	protected final N root;
	protected boolean mutable = true;

	protected AbstractTrie(AbstractTypeFactory<T> factory) {
		this.factory = Objects.requireNonNull(factory);
		this.root = newNode();
	}

	protected abstract N newNode();

	protected static abstract class AbstractNode<T, N extends AbstractNode<T, N, C>, C extends AbstractTrie<T, N, C>> {

		protected CharSequence chars = "";
		protected Set<T> values = EMPTY_SET;
		protected CharMap<N> transitions = EMPTY_MAP;
		protected int hashCode;

		protected abstract C trie();

		public CharSequence chars() {
			return chars;
		}

		public int charCount() {
			return chars.length();
		}

		public Set<T> values() {
			return values;
		}

		public Stream<T> valueStream() {
			return values.stream();
		}

		public CharMap<N> transitions() {
			return transitions;
		}

		public void insert(CharSequence cs, int startIndex, int indexInNode, T value) {
			var isStringEnd = cs.length() == startIndex;
			// split if necessary
			split(indexInNode);
			if (isStringEnd) {
				// add value to this node (after potential split)
				if (values == EMPTY_SET) {
					values = Set.of(value);
				} else {
					if (values.size() == 1)
						values = trie().factory.newValueSet(values);
					values.add(value);
				}
			} else {
				// create a new node for the missing content
				var valueNode = trie().newNode();
				var transitionChar = cs.charAt(startIndex);
				valueNode.chars = cs.subSequence(startIndex + 1, cs.length());
				valueNode.values = Set.of(value);
				// add node transition to this node (after potential split)
				if (transitions == CharMap.EMPTY_MAP)
					transitions = CharMap.newEmpty();
				transitions.put(transitionChar, valueNode);
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
			boolean childrenMatched = transitions.stepAllAndAdvanceWithContext(matcher, result,
					(m, stateAfterTransition, node, context) -> node.search(m, stateAfterTransition, context),
					resultConsumer);
			return hasMatch || childrenMatched;
		}

		protected void split(int indexInNode) {
			if (indexInNode == charCount())
				return;
			var node = trie().newNode();
			// split strings and extract transition character
			var transitionChar = chars.charAt(indexInNode);
			node.chars = chars.subSequence(indexInNode + 1, chars.length());
			chars = chars.subSequence(0, indexInNode);
			// move transitions and values to the new node behind this one
			node.transitions = transitions;
			transitions = trie().factory.newTransitionMap();
			node.values = values;
			values = EMPTY_SET;
			// create new transition
			transitions.put(transitionChar, node);
		}

		protected void compress(CommonCompressionCache cache) {
			cacheSelf(cache.nodes());
			chars = cache.charSequences().getOrCache(chars);
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
		public int hashCode() {
			if (hashCode == 0) {
				hashCode = Objects.hash(chars, values, transitions);
				if (hashCode == 0)
					hashCode = 1;
			}
			return hashCode;
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj == this)
				return true;
			return obj instanceof AbstractTrie.AbstractNode<?, ?, ?> other && hashCode() == other.hashCode()
					&& chars.equals(other.chars) && values.equals(other.values)
					&& transitions.equals(other.transitions);
		}

		@Override
		@SuppressWarnings("deprecation")
		public String toString() {
			startRead();
			try {
				return getClass().getSimpleName() + "[\"" + chars + "\"] values: " + values + "\n"
						+ transitions
								.entrySet().stream().map(e -> e.getValue().toString().indent(8)
										.replaceFirst(" ".repeat(7), " '" + e.getKey() + "' ->"))
								.collect(Collectors.joining());
			} finally {
				endRead();
			}
		}
	}

	protected interface NodeMatch<T, N extends AbstractTrie.AbstractNode<T, N, C>, C extends AbstractTrie<T, N, C>> {

		boolean success();

		N node();

		int indexInNode();

		int indexInString();

		default <R> R switchOnSuccess(Function<? super N, ? extends R> onSuccess,
				BiFunction<? super N, ? super Integer, ? extends R> onFailure) {
			if (success())
				return onSuccess.apply(node());
			return onFailure.apply(node(), indexInString());
		}

		default void insert(CharSequence cs, T value) {
			node().insert(cs, indexInString(), indexInNode(), value);
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

	protected abstract NodeMatch<T, N, C> findNode(CharSequence cs, boolean writeAccess);

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
	public Stream<T> search(CharSequence cs) {
		return findNode(cs, false).switchOnSuccess(N::valueStream, (node, end) -> Stream.of());
	}

	@Override
	public Stream<T> search(GradingLongStepMatcher matcher) {
		var results = new PriorityQueue<GradedValueSet<T>>();
		root.search(matcher, matcher.getStartState(), results::add);
		return results.stream().map(GradedValueSet::values).flatMap(Set::stream).distinct();
	}

	@Override
	public void insert(CharSequence cs, T value) {
		if (!mutable)
			throw new IllegalStateException("Trie is immutable");
		findNode(cs, true).insert(cs, value);
	}

	public void compress() {
		compress(new CommonCompressionCache(factory::newCache));
	}

	@Override
	public void compress(CommonCompressionCache compressionCache) {
		mutable = false;
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
		return obj instanceof AbstractTrie<?, ?, ?> other && hashCode() == other.hashCode() && root.equals(other.root);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + root.toString();
	}
}
