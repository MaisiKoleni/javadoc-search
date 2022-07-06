package net.maisikoleni.javadoc.search;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;
import net.maisikoleni.javadoc.search.TrieSearchEngineUtils.SubdividedEntityConsumer;
import net.maisikoleni.javadoc.util.Cache;
import net.maisikoleni.javadoc.util.RankedTrie.RankedConcurrentTrie;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.WeakCommonPool;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;
import net.maisikoleni.javadoc.util.regex.Regex;

public final class RankedTrieSearchEngine extends IndexBasedSearchEngine {

	private static final Logger LOG = LoggerFactory.getLogger(RankedTrieSearchEngine.class);

	private final Trie<RankedEntry<SearchableEntity>> all;
	private final Trie<RankedEntry<Module>> modules;
	private final Trie<RankedEntry<Package>> packages;
	private final Trie<RankedEntry<Type>> types;
	private final Trie<RankedEntry<Member>> members;
	private final Trie<RankedEntry<Tag>> tags;
	@SuppressWarnings("unused")
	private final WeakCommonPool weakCommonPool;

	public RankedTrieSearchEngine(JavadocIndex index) {
		super(index);
		var cache = new Trie.CommonCompressionCache(Cache::newConcurrent);
		weakCommonPool = WeakCommonPool.get();
		all = generateRankedTrie(index.stream(), cache);
		modules = generateRankedTrie(index.modules(), cache);
		packages = generateRankedTrie(index.packages(), cache);
		types = generateRankedTrie(index.types(), cache);
		members = generateRankedTrie(index.members(), cache);
		tags = generateRankedTrie(index.tags(), cache);
	}

	public static <T extends SearchableEntity> Trie<RankedEntry<T>> generateRankedTrie(List<T> index,
			Trie.CommonCompressionCache cache) {
		return generateRankedTrie(index.stream(), cache);
	}

	public static <T extends SearchableEntity> Trie<RankedEntry<T>> generateRankedTrie(Stream<T> index,
			Trie.CommonCompressionCache cache) {
		var trieTask = WeakCommonPool.get().forkJoinPool().submit(() -> {
			Trie<RankedEntry<T>> trie = new RankedConcurrentTrie<>(RankedTrieSearchEngine::rankSearchResult);
			SubdividedEntityConsumer<T> addRanked = (name, entity, rank) -> trie.insert(name,
					new RankedEntry<>(entity, rank));
			long t1 = System.currentTimeMillis();
			index.parallel().forEach(se -> TrieSearchEngineUtils.subdivideEntity(se, addRanked));
			long t2 = System.currentTimeMillis();
			LOG.info("Constructing trie took {} ms", t2 - t1);
			trie.compress(cache);
			long t3 = System.currentTimeMillis();
			LOG.info("Compressing trie took {} ms", t3 - t2);
			return trie;
		});
		return trieTask.join();
	}

	record RankedEntry<T extends SearchableEntity> (T entity, double rank) implements Comparable<RankedEntry<T>> {

		@Override
		public int compareTo(RankedEntry<T> o) {
			// intentionally reversed
			int cmp = Double.compare(o.rank, rank);
			if (cmp != 0)
				return cmp;
			return entity.compareTo(o.entity);
		}
	}

	private static <T extends SearchableEntity> double rankSearchResult(RankedEntry<T> rankedEntry,
			double searchGrade) {
		return rankedEntry.rank() + searchGrade;
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
}
