package net.maisikoleni.javadoc.util.trie;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.Cache;
import net.maisikoleni.javadoc.util.CharMap;
import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;
import net.maisikoleni.javadoc.util.JdkCharHashMap;
import net.maisikoleni.javadoc.util.PrimitiveReentrantReadWriteLock;

public final class ConcurrentTrie<T> extends AbstractTrie<T, ConcurrentTrie.Node<T>> {

	public ConcurrentTrie() {
		this(new TypeFactory<>());
	}

	public ConcurrentTrie(AbstractTypeFactory<T> factory) {
		super(new Node<>(), factory);
	}

	static class TypeFactory<T> extends AbstractTypeFactory<T> {

		@Override
		protected Set<T> newValueSet() {
			return new HashSet<>();
		}

		@Override
		protected <N> CharMap<N> newTransitionMap() {
			return new JdkCharHashMap<>(2, 0.5f);
		}

		@Override
		protected <R> Cache<R> newCache() {
			return Cache.newConcurrent();
		}
	}

	static final class Node<T> extends AbstractTrie.AbstractNode<T, Node<T>> {

		private static final VarHandle LOCK_VAR_HANDLE;
		static {
			try {
				LOCK_VAR_HANDLE = MethodHandles.lookup().findVarHandle(Node.class, "lockState", int.class);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		private static final PrimitiveReentrantReadWriteLock<Node<?>> LOCK = new PrimitiveReentrantReadWriteLock<>(
				LOCK_VAR_HANDLE);

		@SuppressWarnings("unused") // used in VarHandle
		private int lockState;

		@Override
		protected Node<T> newNode() {
			return new Node<>();
		}

		@Override
		protected void startRead() {
			LOCK.startRead(this);
		}

		@Override
		protected void endRead() {
			LOCK.endRead(this);
		}

		@Override
		protected void startWrite() {
			LOCK.startWrite(this);
		}

		@Override
		protected void endWrite() {
			LOCK.endWrite(this);
		}

		@Override
		protected void cacheSelf(Cache<Node<T>> nodeCache) {
			nodeCache.put(this);
		}

		@Override
		protected void forEachTransition(CharEntryConsumer<ConcurrentTrie.Node<T>> action) {
			transitions.forEachParallel(action);
		}

		@Override
		protected void updateTransitionTarget(char character, ConcurrentTrie.Node<T> newValue) {
			synchronized (transitions) {
				transitions.put(character, newValue);
			}
		}

		@Override
		protected void compress(CompressionCache cache) {
			super.compress(cache);
			LOCK.setInactive(this, true);
		}

		@Override
		protected int calculateHashCode() {
			return Objects.hash(chars, values, transitions.hashCodeParallel());
		}
	}

	@Override
	protected LockedNodeMatch<T> findNode(CharSequence key, boolean writeAccess) {
		int length = key.length();
		// start with the root (and reading it)
		Node<T> node = root;
		node.startRead();
		boolean writeLockAcquired = false;
		int indexInNode = 0;
		int indexInKey = 0;
		int nodeStartInKey = 0;
		MAIN_LOOP: while (indexInKey <= length) {
			int nodeCharCount = node.charCount();
			CHECK: {
				if (indexInKey == length) {
					// acquire write lock and re-check node if necessary
					if (writeAccess && !writeLockAcquired)
						break CHECK;
					boolean isExactMatch = indexInNode == nodeCharCount;
					return new LockedNodeMatch<>(isExactMatch, node, indexInNode, length, writeAccess);
				}
				char c = key.charAt(indexInKey);
				boolean nodeHasCharsLeft = nodeCharCount > indexInNode;
				if (nodeHasCharsLeft && node.chars().charAt(indexInNode) == c) {
					indexInNode++;
				} else if (!nodeHasCharsLeft) {
					Node<T> newNode = node.transitions().get(c);
					if (newNode == null) {
						// acquire write lock and re-check node if necessary
						if (writeAccess && !writeLockAcquired)
							break CHECK;
						return new LockedNodeMatch<>(false, node, indexInNode, indexInKey, writeAccess);
					}
					// start reading new node
					newNode.startRead();
					// stop read/write to the old
					if (writeLockAcquired)
						node.endWrite();
					else
						node.endRead();
					// switch to node at the other end of the transition
					node = newNode;
					// node starts with the char after this transition
					nodeStartInKey = indexInKey + 1;
					// reset node-dependent states
					indexInNode = 0;
					writeLockAcquired = false;
				} else {
					// acquire write lock and re-check node if necessary
					if (writeAccess && !writeLockAcquired)
						break CHECK;
					return new LockedNodeMatch<>(false, node, indexInNode, indexInKey, writeAccess);
				}
				// normal for-loop order, continue with next index
				indexInKey++;
				continue MAIN_LOOP;
			}
			// we found the node but require write access
			node.endRead();
			// state of cNode can change here
			node.startWrite();
			// now we can write, but must check if we still got the correct node
			writeLockAcquired = true;
			// check if there was at least one split
			int newNodeCharCount = node.charCount();
			if (newNodeCharCount != nodeCharCount && newNodeCharCount < indexInNode) {
				// re-check new node end
				indexInKey = nodeStartInKey + newNodeCharCount;
				indexInNode = newNodeCharCount;
			} else {
				// just re-check last char
			}
		}
		throw new IllegalStateException(
				"unreachable code: key='%s',length=%d, writeLock=%s, indexInNode=%d, nodeStartInKey=%d, indexInKey=%d, node=%s"
						.formatted(key, length, writeLockAcquired, indexInNode, nodeStartInKey, indexInKey, node));
	}

	record LockedNodeMatch<T> (boolean success, Node<T> node, int indexInNode, int indexInKey, boolean write)
			implements NodeMatch<T, Node<T>>, AutoCloseable {

		LockedNodeMatch {
			Objects.requireNonNull(node);
		}

		@Override
		public void close() {
			if (write)
				node.endWrite();
			else
				node.endRead();
		}
	}

	@Override
	public Stream<T> search(CharSequence key) {
		try (var nodeMatch = findNode(key, false)) {
			return nodeMatch.switchOnSuccess(Node<T>::valueStream, () -> Stream.of());
		}
	}

	@Override
	public void insert(CharSequence key, T value) {
		if (!mutable)
			throw new IllegalStateException("Trie is immutable");
		try (var nodeMatch = findNode(key, true)) {
			nodeMatch.insert(key, value, factory);
		}
	}
}
