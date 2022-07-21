package net.maisikoleni.javadoc.util.trie;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;
import net.maisikoleni.javadoc.util.trie.AbstractTrie.GradedValueSet;

public class RankedTrie<T extends Comparable<T>> implements Trie<T> {

	private final AbstractTrie<T, ?> trie;
	private final RankingFunction<T> rankingFunction;

	public RankedTrie(AbstractTrie<T, ?> trie, RankingFunction<T> rankingFunction) {
		this.trie = Objects.requireNonNull(trie);
		this.rankingFunction = Objects.requireNonNull(rankingFunction);
	}

	public static final class RankedSimpleTrie<T extends Comparable<T>> extends RankedTrie<T> {

		public RankedSimpleTrie(RankingFunction<T> rankingFunction) {
			super(new SimpleTrie<>(new TypeFactory<T>()), rankingFunction); // $NOSONAR - Eclipse Compiler Bug
		}

		private static class TypeFactory<T extends Comparable<T>> extends SimpleTrie.TypeFactory<T> {

			@Override
			protected Set<T> newValueSet() {
				return new TreeSet<>();
			}
		}
	}

	public static final class RankedConcurrentTrie<T extends Comparable<T>> extends RankedTrie<T> {

		public RankedConcurrentTrie(RankingFunction<T> rankingFunction) {
			super(new ConcurrentTrie<>(new TypeFactory<T>()), rankingFunction); // $NOSONAR - Eclipse Compiler Bug
		}

		private static class TypeFactory<T extends Comparable<T>> extends ConcurrentTrie.TypeFactory<T> {

			@Override
			protected Set<T> newValueSet() {
				return new TreeSet<>();
			}
		}
	}

	@Override
	public void insert(CharSequence key, T value) {
		trie.insert(key, value);
	}

	@Override
	public Stream<T> search(CharSequence key) {
		return trie.search(key);
	}

	@Override
	public void compress(CompressionCache compressionCache) {
		trie.compress(compressionCache);
	}

	@Override
	public Stream<T> search(GradingLongStepMatcher matcher) {
		var rankedMerger = new RankedMerger();
		trie.root.search(matcher, matcher.getStartState(), rankedMerger::add);
		return rankedMerger.stream().distinct();
	}

	private final class RankedMerger implements Spliterator<T> {

		private final PriorityQueue<Entry> sets = new PriorityQueue<>();
		private int size;

		void add(GradedValueSet<T> gradedValueSet) {
			size += gradedValueSet.values().size();
			sets.add(new Entry(gradedValueSet.values(), gradedValueSet.grade()));
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			if (sets.isEmpty())
				return false;
			action.accept(sets.poll().nextAndAdvance(sets));
			return true;
		}

		@Override
		public Spliterator<T> trySplit() {
			return null; // split not possible
		}

		@Override
		public long estimateSize() {
			return size;
		}

		@Override
		public int characteristics() {
			return Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED;
		}

		Stream<T> stream() {
			return StreamSupport.stream(this, false);
		}

		private final class Entry implements Comparable<Entry> {

			private final double searchGrade;
			private final Iterator<T> values;
			private T currentValue;
			private double currentRank;

			Entry(Collection<T> values, double searchGrade) {
				this.searchGrade = searchGrade;
				this.values = values.iterator();
				advance();
			}

			private boolean advance() {
				if (!values.hasNext()) {
					currentValue = null;
					currentRank = 0.0;
					return false;
				}
				currentValue = values.next();
				currentRank = rankingFunction.rank(currentValue, searchGrade);
				return true;
			}

			T nextAndAdvance(PriorityQueue<Entry> sets) {
				var value = currentValue;
				if (advance())
					sets.add(this);
				return value;
			}

			@Override
			public int compareTo(Entry o) {
				// reversed to have higher values first
				var rankComparison = Double.compare(o.currentRank, currentRank);
				if (rankComparison != 0)
					return rankComparison;
				return currentValue.compareTo(o.currentValue);
			}
		}
	}

	@FunctionalInterface
	public interface RankingFunction<T> {

		/**
		 * Returns the rank of a result for a given search and match.
		 * <p>
		 * A higher value is better, 0.0 is the lowest possible rank.
		 *
		 * @param entry       the rank associated with the matched entry
		 * @param searchGrade the grade of the match itself from
		 *                    {@link GradingLongStepMatcher#grade(long)}
		 */
		double rank(T entry, double searchGrade);
	}

	@Override
	public final int hashCode() {
		return rankingFunction.hashCode() * 31 + trie.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof RankedTrie<?> other))
			return false;
		return rankingFunction.equals(other.rankingFunction) && trie.equals(other.trie);
	}

	@Override
	public final String toString() {
		return "Ranked" + trie.toString();
	}
}
