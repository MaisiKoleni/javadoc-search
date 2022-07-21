package net.maisikoleni.javadoc.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;

public final class SimpleTrie<T> extends AbstractTrie<T, SimpleTrie.Node<T>> {

	public SimpleTrie() {
		this(new TypeFactory<>());
	}

	public SimpleTrie(AbstractTypeFactory<T> factory) {
		super(new Node<>(), factory);
	}

	static class TypeFactory<T> extends AbstractTypeFactory<T> {

		@Override
		protected Set<T> newValueSet() {
			return new HashSet<>(2, 0.75f);
		}

		@Override
		protected <N> CharMap<N> newTransitionMap() {
			return new JdkCharHashMap<>(2, 0.5f);
		}

		@Override
		protected <R> Cache<R> newCache() {
			return Cache.newDefault();
		}
	}

	static final class Node<T> extends AbstractTrie.AbstractNode<T, Node<T>> {

		@Override
		protected Node<T> newNode() {
			return new Node<>();
		}

		@Override
		protected void cacheSelf(Cache<Node<T>> nodeCache) {
			nodeCache.put(this);
		}

		@Override
		protected void forEachTransition(CharEntryConsumer<Node<T>> action) {
			transitions.forEach(action);
		}
	}

	@Override
	protected SimpleNodeMatch<T> findNode(CharSequence key, boolean writeAccess) {
		var node = root;
		var indexInNode = 0;
		for (int indexInKey = 0; indexInKey < key.length(); indexInKey++) {
			char c = key.charAt(indexInKey);
			boolean nodeHasCharsLeft = node.charCount() > indexInNode;
			if (nodeHasCharsLeft && node.chars().charAt(indexInNode) == c) {
				indexInNode++;
			} else if (!nodeHasCharsLeft) {
				var newNode = node.transitions().get(c);
				if (newNode == null)
					return new SimpleNodeMatch<>(false, node, indexInNode, indexInKey);
				node = newNode;
				indexInNode = 0;
			} else {
				return new SimpleNodeMatch<>(false, node, indexInNode, indexInKey);
			}
		}
		return new SimpleNodeMatch<>(indexInNode == node.charCount(), node, indexInNode, key.length());
	}

	record SimpleNodeMatch<T> (boolean success, Node<T> node, int indexInNode, int indexInKey)
			implements NodeMatch<T, Node<T>> {
		SimpleNodeMatch {
			Objects.requireNonNull(node);
		}
	}

}
