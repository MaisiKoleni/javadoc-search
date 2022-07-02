package net.maisikoleni.javadoc.search;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
import net.maisikoleni.javadoc.util.Cache;
import net.maisikoleni.javadoc.util.ConcurrentTrie;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.regex.CharClass;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.Concatenation;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;
import net.maisikoleni.javadoc.util.regex.Literal;
import net.maisikoleni.javadoc.util.regex.Regex;
import net.maisikoleni.javadoc.util.regex.Star;

public final class TrieSearchEngine extends IndexBasedSearchEngine {

	private static final Logger LOG = LoggerFactory.getLogger(TrieSearchEngine.class);

	private static final String SEPARATOR_CHAR_CLASS = ".,()<>/\\[\\]";
	private static final Pattern SEPARATORS = Pattern.compile("[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Pattern QUERY_SPLIT = Pattern.compile("\\b|(?<=[" + SEPARATOR_CHAR_CLASS + "])");
	private static final Pattern IDENTIFIER_SPLIT = Pattern.compile("(?=[\\p{javaUpperCase}_])");
	private static final Pattern USEFUL_CHARS = Pattern.compile("[\\p{javaUpperCase}\\p{javaLowerCase}]");

	private static final Pattern QUERY_WHITESPACE = Pattern.compile("\\s++");
	private static final Pattern QUERY_UNECESSARY_SPACE = Pattern.compile(" \\B|\\B ");

	private static final Regex REGEX_WHITESPACE = new CharClass(Character::isWhitespace, "\\s");
	private static final Regex REGEX_ANY_WHITESPACE = new Star(REGEX_WHITESPACE);
	private static final Regex REGEX_LOWER_CASE = new CharClass(Character::isLowerCase, "\\p{javaLowerCase}");
	private static final Regex REGEX_ANY_LOWER_CASE = new Star(REGEX_LOWER_CASE);
	private static final Regex REGEX_ANY_NON_DIVIDER = new Star(new CharClass(c -> c != '.' && c != '/', "[^./]"));
	private static final Regex REGEX_WHITESPACE_WITH_ANY_LOWER = new Concatenation(REGEX_ANY_LOWER_CASE,
			REGEX_WHITESPACE);

	private static final Regex REGEX_START_AFTER = new CharClass(c -> isSeparator(c) || c == '_' || c == ' ',
			"[" + SEPARATOR_CHAR_CLASS + "_ ]");
	private static final Regex REGEX_START_BEFORE = new CharClass(
			c -> isSeparator(c) || Character.isUpperCase(c) || c == '_', "[" + SEPARATOR_CHAR_CLASS + "_ ]");

	private static final char[] SPEPARATOR_CHARS = new char[] { '.', ',', '(', ')', '<', '>', '/', '[', ']' };

	/*
	 * This is for resetting the own common pool forcefully and have it GC'ed
	 */
	private static final Cleaner poolCleaner = Cleaner.create();
	private static WeakReference<AtomicReference<ForkJoinPool>> ownCommonPool = new WeakReference<>(null);

	private final Trie<SearchableEntity> all;
	private final Trie<Module> modules;
	private final Trie<Package> packages;
	private final Trie<Type> types;
	private final Trie<Member> members;
	private final Trie<Tag> tags;
	@SuppressWarnings("unused")
	private final AtomicReference<ForkJoinPool> ownCommonPoolReference;

	public TrieSearchEngine(JavadocIndex index) {
		super(index);
		var cache = new Trie.CommonCompressionCache(Cache::newConcurrent);
		ownCommonPoolReference = ownCommonPool();
		all = generateTrie(index.stream(), cache);
		modules = generateTrie(index.modules(), cache);
		packages = generateTrie(index.packages(), cache);
		types = generateTrie(index.types(), cache);
		members = generateTrie(index.members(), cache);
		tags = generateTrie(index.tags(), cache);
	}

	private static synchronized AtomicReference<ForkJoinPool> ownCommonPool() {
		var cachedPool = ownCommonPool.get();
		if (cachedPool != null)
			return cachedPool;
		var newPool = new ForkJoinPool(16);
		var newPoolRef = new AtomicReference<>(newPool);
		poolCleaner.register(newPoolRef, newPool::shutdownNow);
		ownCommonPool = new WeakReference<>(newPoolRef);
		LOG.debug("New ForkJoinPool for parallel trie generation created.");
		return newPoolRef;
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
		Trie<T> trie = trieSupplier.get();
		ownCommonPool().get().invoke(new ForkJoinTask<>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean exec() {
				long t1 = System.currentTimeMillis();
				index.forEach(se -> {
					var preProcessedName = preProcessEntry(se.qualifiedName());
					splitIntoPieces(preProcessedName).forEach(s -> trie.insert(s, se));
				});
				long t2 = System.currentTimeMillis();
				LOG.info("Constructing trie took {} ms", t2 - t1);
				trie.compress(cache);
				long t3 = System.currentTimeMillis();
				LOG.info("Compressing trie took {} ms", t3 - t2);
				return true;
			}

			@Override
			protected void setRawResult(Object value) {
				// nothing
			}

			@Override
			public Object getRawResult() {
				// nothing
				return null;
			}
		});
		return trie;
	}

	private static boolean isSeparator(char c) {
		for (int i = 0; i < SPEPARATOR_CHARS.length; i++)
			if (c == SPEPARATOR_CHARS[i])
				return true;
		return false;
	}

	private static Stream<CharSequence> splitIntoPieces(CharSequence preProcessedName) {
		var streamBuilder = Stream.<CharSequence>builder();
		streamBuilder.add(preProcessedName);
		int length = preProcessedName.length();
		for (int i = 1; i < length; i++) {
			if (REGEX_START_AFTER.matches(preProcessedName, i - 1, i)
					|| REGEX_START_BEFORE.matches(preProcessedName, i, i + 1))
				streamBuilder.add(preProcessedName.subSequence(i, length));
		}
		return streamBuilder.build().filter(TrieSearchEngine::isUseful);
	}

	private static boolean isUseful(CharSequence cs) {
		return !cs.isEmpty() && (cs.length() > 2 || USEFUL_CHARS.matcher(cs).find());
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

	private static CharSequence preProcessEntry(CharSequence query) {
		// TODO this makes it a string
		var queryWithSpaces = QUERY_WHITESPACE.matcher(query).replaceAll(" ");
		return QUERY_UNECESSARY_SPACE.matcher(queryWithSpaces).replaceAll("");
	}

	private static Regex generateRegex(CharSequence cleanQuery) {
		if (cleanQuery.isEmpty() || cleanQuery.length() == 1 && isSeparator(cleanQuery.charAt(0)))
			return new Concatenation();
		return Concatenation.of(Stream.of(QUERY_SPLIT.split(cleanQuery)).map(String::strip).map(part -> {
			// preserve "significant" whitespace between words
			if (part.isEmpty())
				return REGEX_WHITESPACE_WITH_ANY_LOWER;
			// keep separators as they are
			if (SEPARATORS.matcher(part).matches())
				return Concatenation.of(REGEX_ANY_LOWER_CASE, new Literal(part));
			// insert patterns for partial identifier matches
			return Stream.of(IDENTIFIER_SPLIT.split(part)).map(Literal::new)
					.collect(Concatenation.joining(REGEX_ANY_LOWER_CASE));
		}).collect(Concatenation.joining(Concatenation.of(REGEX_ANY_WHITESPACE),
				(prev, next) -> next != REGEX_WHITESPACE_WITH_ANY_LOWER)), REGEX_ANY_NON_DIVIDER);
	}

	private static GradingLongStepMatcher compileMatcher(String query) {
		var cleanQuery = preProcessEntry(query.strip());
		var simpleRegex = generateRegex(cleanQuery);
		return CompiledRegex.compile(simpleRegex);
	}

	private static <T extends SearchableEntity> Stream<T> search(Trie<T> trie, GradingLongStepMatcher matcher) {
		return trie.search(matcher);
	}
}
