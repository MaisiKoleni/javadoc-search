package net.maisikoleni.javadoc.util;

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

public final class RankedConcurrentTrie<T extends Comparable<T>> extends ConcurrentTrie<T> {

	private final RankingFunction<T> rankingFunction;

	public RankedConcurrentTrie(RankingFunction<T> rankingFunction) {
		super(new TypeFactory<>());
		this.rankingFunction = Objects.requireNonNull(rankingFunction);
	}

	static class TypeFactory<T extends Comparable<T>> extends ConcurrentTrie.TypeFactory<T> {

		@Override
		protected Set<T> newValueSet() {
			return new TreeSet<>();
		}
	}

	@Override
	public Stream<T> search(GradingLongStepMatcher matcher) {
		var rankedMerger = new RankedMerger();
		root.search(matcher, matcher.getStartState(), rankedMerger::add);
		return rankedMerger.stream().distinct();
	}

	private class RankedMerger implements Spliterator<T> {

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

		private class Entry implements Comparable<Entry> {

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
				currentRank = rankingFunction.applyAsDouble(currentValue, searchGrade);
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
				return Double.compare(o.currentRank, currentRank);
			}
		}
	}

	@FunctionalInterface
	public interface RankingFunction<T> {

		/**
		 * Returns the score of a result for a given search and match.
		 * <p>
		 * A higher value is better, 0.0 is the lowest possible rank.
		 *
		 * @param entry       the rank associated with the matched entry
		 * @param searchGrade the grade of the match itself from
		 *                    {@link GradingLongStepMatcher#grade(long)}
		 */
		double applyAsDouble(T entry, double searchGrade);
	}
}