package net.maisikoleni.javadoc.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.CharMap.CharEntryConsumer;

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
				LOCK_VAR_HANDLE = MethodHandles.lookup().findVarHandle(Node.class, "intLock", int.class);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		private static final PrimitiveReentrantReadWriteLock<Node<?>> LOCK = new PrimitiveReentrantReadWriteLock<>(
				LOCK_VAR_HANDLE);
		private static final int LOCK_DEACTIVATED = Integer.MIN_VALUE;

		private int intLock;

		@Override
		protected Node<T> newNode() {
			return new Node<>();
		}

		@Override
		protected void startRead() {
			if (intLock != LOCK_DEACTIVATED)
				LOCK.startRead(this);
		}

		@Override
		protected void endRead() {
			if (intLock != LOCK_DEACTIVATED)
				LOCK.endRead(this);
		}

		@Override
		protected void startWrite() {
			if (intLock != LOCK_DEACTIVATED)
				LOCK.startWrite(this);
		}

		@Override
		protected void endWrite() {
			if (intLock != LOCK_DEACTIVATED)
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
		protected void compress(CommonCompressionCache cache) {
			super.compress(cache);
			intLock = LOCK_DEACTIVATED;
		}

		@Override
		protected int calculateHashCode() {
			return Objects.hash(chars, values, transitions.hashCodeParallel());
		}
	}

	@Override
	protected LockedNodeMatch<T> findNode(CharSequence cs, boolean writeAccess) {
		int length = cs.length();
		// start with the root (and reading it)
		Node<T> node = root;
		node.startRead();
		boolean writeLockAcquired = false;
		int indexInNode = 0;
		int nodeStartInString = 0;
		int indexInString = 0;
		MAIN_LOOP: while (indexInString <= length) {
			int nodeCharCount = node.charCount();
			CHECK: {
				if (indexInString == length) {
					// acquire write lock and re-check node if necessary
					if (writeAccess && !writeLockAcquired)
						break CHECK;
					boolean isExactMatch = indexInNode == nodeCharCount;
					return new LockedNodeMatch<>(isExactMatch, node, indexInNode, length, writeAccess);
				}
				char c = cs.charAt(indexInString);
				boolean nodeHasCharsLeft = nodeCharCount > indexInNode;
				if (nodeHasCharsLeft && node.chars().charAt(indexInNode) == c) {
					indexInNode++;
				} else if (!nodeHasCharsLeft) {
					Node<T> newNode = node.transitions().get(c);
					if (newNode == null) {
						// acquire write lock and re-check node if necessary
						if (writeAccess && !writeLockAcquired)
							break CHECK;
						return new LockedNodeMatch<>(false, node, indexInNode, indexInString, writeAccess);
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
					nodeStartInString = indexInString + 1;
					// reset node-dependent states
					indexInNode = 0;
					writeLockAcquired = false;
				} else {
					// acquire write lock and re-check node if necessary
					if (writeAccess && !writeLockAcquired)
						break CHECK;
					return new LockedNodeMatch<>(false, node, indexInNode, indexInString, writeAccess);
				}
				// normal for-loop order, continue with next index
				indexInString++;
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
				indexInString = nodeStartInString + newNodeCharCount;
				indexInNode = newNodeCharCount;
			} else {
				// just re-check last char
			}
		}
		throw new IllegalStateException(
				"unreachable code: cs='%s',length=%d, writeLock=%s, indexInNode=%d, nodeStartInString=%d, indexInString=%d, node=%s"
						.formatted(cs, length, writeLockAcquired, indexInNode, nodeStartInString, indexInString, node));
	}

	record LockedNodeMatch<T> (boolean success, Node<T> node, int indexInNode, int indexInString, boolean write)
			implements NodeMatch<T, Node<T>> {

		LockedNodeMatch {
			Objects.requireNonNull(node);
		}

		@Override
		public <R> R switchOnSuccess(Function<? super Node<T>, ? extends R> onSuccess,
				BiFunction<? super Node<T>, ? super Integer, ? extends R> onFailure) {
			try {
				return NodeMatch.super.switchOnSuccess(onSuccess, onFailure);
			} finally {
				releaseLock();
			}
		}

		@Override
		public void insert(CharSequence cs, T value, AbstractTypeFactory<T> factory) {
			try {
				NodeMatch.super.insert(cs, value, factory);
			} finally {
				releaseLock();
			}
		}

		void releaseLock() {
			if (write)
				node.endWrite();
			else
				node.endRead();
		}

	}

	@Override
	public Stream<T> search(CharSequence cs) {
		return findNode(cs, false).switchOnSuccess(Node<T>::valueStream, (node, end) -> Stream.of());
	}

	@Override
	public void insert(CharSequence cs, T value) {
		if (!mutable)
			throw new IllegalStateException("Trie is immutable");
		findNode(cs, true).insert(cs, value, factory);
	}
}
