package net.maisikoleni.javadoc.search;

import java.util.List;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;
import net.maisikoleni.javadoc.util.RankedTrie.RankedConcurrentTrie;
import net.maisikoleni.javadoc.util.RankedTrie.RankingFunction;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;
import net.maisikoleni.javadoc.util.regex.Regex;

public final class RankedTrieSearchEngine extends IndexBasedSearchEngine {

	private final Trie<RankedEntry<SearchableEntity>> all;
	private final Trie<RankedEntry<Module>> modules;
	private final Trie<RankedEntry<Package>> packages;
	private final Trie<RankedEntry<Type>> types;
	private final Trie<RankedEntry<Member>> members;
	private final Trie<RankedEntry<Tag>> tags;

	public RankedTrieSearchEngine(JavadocIndex index) {
		super(index);
		var generator = new RankedConcurrentTrieGenerator();
		all = generator.generateTrie(index.stream());
		modules = generator.generateTrie(index.modules());
		packages = generator.generateTrie(index.packages());
		types = generator.generateTrie(index.types());
		members = generator.generateTrie(index.members());
		tags = generator.generateTrie(index.tags());
	}

	static final class RankedConcurrentTrieGenerator extends TrieGenerator {

		RankedConcurrentTrieGenerator() {
			super(true);
		}

		<S extends SearchableEntity> Trie<RankedEntry<S>> generateTrie(List<S> index) {
			return generateTrie(index.stream());
		}

		<S extends SearchableEntity> Trie<RankedEntry<S>> generateTrie(Stream<S> index) {
			return super.generateTrie(index.parallel(), RankedConcurrentTrieGenerator::newTrie, RankedEntry::from);
		}

		static <S extends SearchableEntity> RankedConcurrentTrie<RankedEntry<S>> newTrie() {
			return new RankedConcurrentTrie<>(SearchableEntityRankingFunction.get());
		}
	}

	record RankedEntry<T extends Comparable<? super T>> (T entity, double rank) implements Comparable<RankedEntry<T>> {

		@Override
		public int compareTo(RankedEntry<T> o) {
			// intentionally reversed
			int cmp = Double.compare(o.rank, rank);
			if (cmp != 0)
				return cmp;
			return entity.compareTo(o.entity);
		}

		static <T extends Comparable<? super T>> RankedEntry<T> from(@SuppressWarnings("unused") CharSequence name,
				T entity, double rank) {
			return new RankedEntry<>(entity, rank);
		}
	}

	@Override
	public Stream<SearchableEntity> search(String query) {
		var regex = TrieSearchEngineUtils.generateRegexFromQuery(query);
		return GroupedSearchResult.ifEmptyTry(search(all, CompiledRegex.compile(regex, false)),
				() -> search(all, CompiledRegex.compile(regex, true)));
	}

	@Override
	public GroupedSearchResult searchGroupedByType(String query) {
		var regex = TrieSearchEngineUtils.generateRegexFromQuery(query);
		return searchGroupedByType(regex, false).ifEmptyTry(() -> searchGroupedByType(regex, true));
	}

	public GroupedSearchResult searchGroupedByType(Regex regex, boolean caseInsensitive) {
		var matcher = CompiledRegex.compile(regex, caseInsensitive);
		return new GroupedSearchResult(search(modules, matcher), search(packages, matcher), search(types, matcher),
				search(members, matcher), search(tags, matcher));
	}

	private static <T extends SearchableEntity> Stream<T> search(Trie<RankedEntry<T>> trie,
			GradingLongStepMatcher matcher) {
		return trie.search(matcher).map(RankedEntry::entity);
	}

	static final class SearchableEntityRankingFunction<T extends SearchableEntity>
			implements RankingFunction<RankedEntry<T>> {

		@SuppressWarnings("rawtypes")
		private static final SearchableEntityRankingFunction INSTANCE = new SearchableEntityRankingFunction<>();

		private SearchableEntityRankingFunction() {
		}

		@Override
		public double applyAsDouble(RankedEntry<T> rankedEntry, double searchGrade) {
			return rankedEntry.rank() + searchGrade;
		}

		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && obj.getClass() == SearchableEntityRankingFunction.class;
		}

		@Override
		public String toString() {
			return "SearchableEntityRankingFunction[RankedEntry.rank() + searchGrade]";
		}

		static <T extends SearchableEntity> SearchableEntityRankingFunction<T> get() {
			return INSTANCE;
		}
	}
}
