package net.maisikoleni.javadoc.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;

public class SimpleTrie<T> extends AbstractTrie<T, SimpleTrie<T>.Node, SimpleTrie<T>> {

	public SimpleTrie() {
		super(new TypeFactory<>());
	}

	public SimpleTrie(AbstractTypeFactory<T> factory) {
		super(factory);
	}

	@Override
	protected Node newNode() {
		return new Node();
	}

	static class TypeFactory<T> extends AbstractTypeFactory<T> {

		@Override
		protected Set<T> newValueSet() {
			return new HashSet<>(2, 0.75f);
		}

		@Override
		protected <N> CharMap<N> newTransitionMap() {
			return new CharObjectHashMap<>(2, 0.5f);
		}

		@Override
		protected <R> Cache<R> newCache() {
			return Cache.newDefault();
		}
	}

	class Node extends AbstractTrie.AbstractNode<T, Node, SimpleTrie<T>> {

		@Override
		protected SimpleTrie<T> trie() {
			return SimpleTrie.this;
		}

		@Override
		protected void cacheSelf(Cache<Node> nodeCache) {
			nodeCache.put(this);
		}

		@Override
		protected void forEachTransition(CharEntryConsumer<SimpleTrie<T>.Node> action) {
			transitions.forEach(action);
		}
	}

	@Override
	protected SimpleNodeMatch<T> findNode(CharSequence cs, boolean writeAccess) {
		var cNode = root;
		var indexInNode = 0;
		for (int indexInString = 0; indexInString < cs.length(); indexInString++) {
			char c = cs.charAt(indexInString);
			boolean nodeHasCharsLeft = cNode.charCount() > indexInNode;
			if (nodeHasCharsLeft && cNode.chars().charAt(indexInNode) == c) {
				indexInNode++;
			} else if (!nodeHasCharsLeft) {
				var newNode = cNode.transitions().get(c);
				if (newNode == null)
					return new SimpleNodeMatch<>(false, cNode, indexInNode, indexInString);
				cNode = newNode;
				indexInNode = 0;
			} else {
				return new SimpleNodeMatch<>(false, cNode, indexInNode, indexInString);
			}
		}
		return new SimpleNodeMatch<>(indexInNode == cNode.charCount(), cNode, indexInNode, cs.length());
	}

	record SimpleNodeMatch<T> (boolean success, SimpleTrie<T>.Node node, int indexInNode, int indexInString)
			implements NodeMatch<T, SimpleTrie<T>.Node, SimpleTrie<T>> {
		SimpleNodeMatch {
			Objects.requireNonNull(node);
		}
	}

}
