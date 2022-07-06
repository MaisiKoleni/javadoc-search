package net.maisikoleni.javadoc.search;

import java.util.List;
import java.util.function.Supplier;
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
import net.maisikoleni.javadoc.util.ConcurrentTrie;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.WeakCommonPool;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public final class TrieSearchEngine extends IndexBasedSearchEngine {

	private static final Logger LOG = LoggerFactory.getLogger(TrieSearchEngine.class);

	private final Trie<SearchableEntity> all;
	private final Trie<Module> modules;
	private final Trie<Package> packages;
	private final Trie<Type> types;
	private final Trie<Member> members;
	private final Trie<Tag> tags;
	@SuppressWarnings("unused")
	private final WeakCommonPool weakCommonPool;

	public TrieSearchEngine(JavadocIndex index) {
		super(index);
		var cache = new Trie.CommonCompressionCache(Cache::newConcurrent);
		weakCommonPool = WeakCommonPool.get();
		all = generateTrie(index.stream(), cache);
		modules = generateTrie(index.modules(), cache);
		packages = generateTrie(index.packages(), cache);
		types = generateTrie(index.types(), cache);
		members = generateTrie(index.members(), cache);
		tags = generateTrie(index.tags(), cache);
	}

	public static <T extends SearchableEntity> Trie<T> generateTrie(List<T> index, Trie.CommonCompressionCache cache) {
		return generateTrie(index.stream(), cache);
	}

	public static <T extends SearchableEntity> Trie<T> generateTrie(Stream<T> index,
			Trie.CommonCompressionCache cache) {
		return generateTrie(index.parallel(), cache, ConcurrentTrie::new);
	}

	public static <T extends SearchableEntity> Trie<T> generateTrie(Stream<T> index, Trie.CommonCompressionCache cache,
			Supplier<Trie<T>> trieSupplier) {
		var trieTask = WeakCommonPool.get().forkJoinPool().submit(() -> {
			Trie<T> trie = trieSupplier.get();
			long t1 = System.currentTimeMillis();
			SubdividedEntityConsumer<T> addToTrie = (name, entity, rank) -> trie.insert(name, entity);
			index.forEach(se -> TrieSearchEngineUtils.subdivideEntity(se, addToTrie));
			long t2 = System.currentTimeMillis();
			LOG.info("Constructing trie took {} ms", t2 - t1);
			trie.compress(cache);
			long t3 = System.currentTimeMillis();
			LOG.info("Compressing trie took {} ms", t3 - t2);
			return trie;
		});
		return trieTask.join();
	}

	@Override
	public Stream<SearchableEntity> search(String query) {
		var precessedQuery = compileMatcher(query);
		return search(all, precessedQuery);
	}

	@Override
	public GroupedSearchResult searchGroupedByType(String query) {
		var precessedQuery = compileMatcher(query);
		return new GroupedSearchResult(search(modules, precessedQuery), search(packages, precessedQuery),
				search(types, precessedQuery), search(members, precessedQuery), search(tags, precessedQuery));
	}

	private static GradingLongStepMatcher compileMatcher(String query) {
		var simpleRegex = TrieSearchEngineUtils.generateRegexFromQuery(query);
		return CompiledRegex.compile(simpleRegex);
	}

	private static <T extends SearchableEntity> Stream<T> search(Trie<T> trie, GradingLongStepMatcher matcher) {
		return trie.search(matcher);
	}
}
