package net.maisikoleni.javadoc.search;

import static net.maisikoleni.javadoc.util.regex.CharPredicate.caseIndependent;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import net.maisikoleni.javadoc.util.RankedConcurrentTrie;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.regex.CharClass;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.Concatenation;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;
import net.maisikoleni.javadoc.util.regex.Literal;
import net.maisikoleni.javadoc.util.regex.Regex;
import net.maisikoleni.javadoc.util.regex.Star;

public final class RankedTrieSearchEngine extends IndexBasedSearchEngine {

	private static final Logger LOG = LoggerFactory.getLogger(RankedTrieSearchEngine.class);

	private static final String SKIP_CHAR = "~";
	private static final String SEPARATOR_CHAR_CLASS = ".,()<>/\\[\\]";
	private static final Pattern SEPARATORS = Pattern.compile("[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Pattern QUERY_SPLIT = Pattern
			.compile("(?<!" + SKIP_CHAR + ")\\b(?!" + SKIP_CHAR + ")|(?<=[" + SEPARATOR_CHAR_CLASS + "])");
	private static final Pattern IDENTIFIER_SPLIT = Pattern.compile("(?=[\\p{javaUpperCase}_])");
	private static final Pattern SKIP_TO_CHAR = Pattern.compile(SKIP_CHAR + "++");

	private static final Pattern QUERY_WHITESPACE = Pattern.compile("\\s++");
	private static final Pattern QUERY_UNECESSARY_SPACE = Pattern.compile(" \\B|\\B ");

	private static final Regex REGEX_WHITESPACE = new CharClass(caseIndependent(Character::isWhitespace), "\\s");
	private static final Regex REGEX_ANY_WHITESPACE = new Star(REGEX_WHITESPACE);
	private static final Regex REGEX_LOWER_CASE = new CharClass(caseIndependent(Character::isLowerCase),
			"\\p{javaLowerCase}");
	private static final Regex REGEX_ANY_LOWER_CASE = new Star(REGEX_LOWER_CASE);
	private static final Regex REGEX_ANY_NON_DIVIDER = new Star(
			new CharClass(caseIndependent(c -> c != '.' && c != '/'), "[^./]"));
	private static final Regex REGEX_WHITESPACE_WITH_ANY_LOWER = new Concatenation(REGEX_ANY_LOWER_CASE,
			new CharClass(caseIndependent(c -> c == '.' || c == '/' || Character.isWhitespace(c)), "[\\s/.]"));

	private static final Regex REGEX_USEFUL_CHARS = new Concatenation(
			new Star(new CharClass(c -> !Character.isAlphabetic(c) && !Character.isDigit(c), "[^\\p{Alnum}]")),
			new CharClass(c -> Character.isAlphabetic(c) || Character.isDigit(c), "[\\p{Alnum}]"),
			new Star(CharClass.ANY));
	private static final Regex REGEX_DIVIDER = new CharClass(c -> c == '.' || c == '/',
			"[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Regex REGEX_START_AFTER = new CharClass(c -> isSeparator(c) || c == '_' || c == ' ',
			"[" + SEPARATOR_CHAR_CLASS + "_ ]");
	private static final Regex REGEX_START_BEFORE = new CharClass(
			c -> isSeparator(c) || Character.isUpperCase(c) || c == '_',
			"[" + SEPARATOR_CHAR_CLASS + "_\\p{javaUpperCase}]");

	private static final char[] SPEPARATOR_CHARS = new char[] { '.', ',', '(', ')', '<', '>', '/', '[', ']' };

	/*
	 * This is for resetting the own common pool forcefully and have it GC'ed
	 */
	private static final Cleaner poolCleaner = Cleaner.create();
	private static WeakReference<AtomicReference<ForkJoinPool>> ownCommonPool = new WeakReference<>(null);

	private final Trie<RankedEntry<SearchableEntity>> all;
	private final Trie<RankedEntry<Module>> modules;
	private final Trie<RankedEntry<Package>> packages;
	private final Trie<RankedEntry<Type>> types;
	private final Trie<RankedEntry<Member>> members;
	private final Trie<RankedEntry<Tag>> tags;
	@SuppressWarnings("unused")
	private final AtomicReference<ForkJoinPool> ownCommonPoolReference;

	public RankedTrieSearchEngine(JavadocIndex index) {
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
		// TODO: fix deadlock, improve concurrency
		var newPool = new ForkJoinPool(16, ForkJoinPool.defaultForkJoinWorkerThreadFactory, (t, e) -> {
			// do nothing
		}, false, 16, 256, 4, null, 1, TimeUnit.MINUTES);
		var newPoolRef = new AtomicReference<>(newPool);
		poolCleaner.register(newPoolRef, newPool::shutdownNow);
		ownCommonPool = new WeakReference<>(newPoolRef);
		LOG.debug("New ForkJoinPool for parallel trie generation created.");
		return newPoolRef;
	}

	public static <T extends SearchableEntity> Trie<RankedEntry<T>> generateTrie(List<T> index,
			Trie.CommonCompressionCache cache) {
		return generateTrie(index.stream(), cache);
	}

	public static <T extends SearchableEntity> Trie<RankedEntry<T>> generateTrie(Stream<T> index,
			Trie.CommonCompressionCache cache) {
		Trie<RankedEntry<T>> trie = new RankedConcurrentTrie<>(RankedTrieSearchEngine::rankSearchResult);
		ownCommonPool().get().invoke(new ForkJoinTask<>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean exec() {
				long t1 = System.currentTimeMillis();
				index.parallel().forEach(se -> addEntity(se, trie));
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

	private static boolean isSeparator(char c) {
		for (int i = 0; i < SPEPARATOR_CHARS.length; i++)
			if (c == SPEPARATOR_CHARS[i])
				return true;
		return false;
	}

	private static <T extends SearchableEntity> void addEntity(T se, Trie<RankedEntry<T>> trie) {
		var preProcessedName = preProcessEntry(se.qualifiedName());
		trie.insert(preProcessedName, new RankedEntry<>(se, 3.0));
		int length = preProcessedName.length();
		for (int i = 1; i < length; i++) {
			double rank = 0.0;
			if (REGEX_DIVIDER.matches(preProcessedName, i - 1, i) || REGEX_DIVIDER.matches(preProcessedName, i, i + 1))
				rank = 2.5;
			else if (REGEX_START_AFTER.matches(preProcessedName, i - 1, i)
					|| REGEX_START_BEFORE.matches(preProcessedName, i, i + 1))
				rank = 1.25;
			if (rank > 0.0) {
				var namePart = preProcessedName.subSequence(i, length);
				if (isUseful(namePart))
					trie.insert(namePart, new RankedEntry<>(se, rank));
			}
		}
	}

	private static boolean isUseful(CharSequence cs) {
		return !cs.isEmpty() && (cs.length() > 2 || REGEX_USEFUL_CHARS.matches(cs));
	}

	@Override
	public Stream<SearchableEntity> search(String query) {
		var regex = generateRegexFromQuery(query);
		return GroupedSearchResult.ifEmptyTry(search(all, CompiledRegex.compile(regex, false)),
				() -> search(all, CompiledRegex.compile(regex, true)));
	}

	@Override
	public GroupedSearchResult searchGroupedByType(String query) {
		var regex = generateRegexFromQuery(query);
		return searchGroupedByType(regex, false).ifEmptyTry(() -> searchGroupedByType(regex, true));
	}

	public GroupedSearchResult searchGroupedByType(Regex regex, boolean caseInsensitive) {
		var matcher = CompiledRegex.compile(regex, caseInsensitive);
		return new GroupedSearchResult(search(modules, matcher), search(packages, matcher), search(types, matcher),
				search(members, matcher), search(tags, matcher));
	}

	private static CharSequence preProcessEntry(CharSequence query) {
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
			return Stream.of(IDENTIFIER_SPLIT.split(part)).map(x ->
			// allow skipping lower case chars with ~ and use the next as char class
			Stream.of(SKIP_TO_CHAR.split(x)).map(Literal::new).collect(Concatenation.joining((prev, next) -> {
				var nextChar = next.chars().charAt(0);
				var charClass = new CharClass(caseIndependent(c -> c != nextChar && Character.isLowerCase(c)),
						"[^" + nextChar + "]");
				return Concatenation.of(charClass, new Star(charClass));
			}))).collect(Concatenation.joining(REGEX_ANY_LOWER_CASE));
		}).collect(
				Concatenation.joining(REGEX_ANY_WHITESPACE, (prev, next) -> next != REGEX_WHITESPACE_WITH_ANY_LOWER)),
				REGEX_ANY_NON_DIVIDER);
	}

	private static Regex generateRegexFromQuery(String query) {
		var cleanQuery = preProcessEntry(query.strip());
		// TODO: keep?
		if (cleanQuery.toString().endsWith("^^"))
			cleanQuery = cleanQuery.subSequence(0, cleanQuery.length() - 2).toString().toUpperCase(Locale.ROOT);
		return generateRegex(cleanQuery);
	}

	private static <T extends SearchableEntity> Stream<T> search(Trie<RankedEntry<T>> trie,
			GradingLongStepMatcher matcher) {
		return trie.search(matcher).map(RankedEntry::entity);
	}
}
