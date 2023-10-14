package net.maisikoleni.javadoc.search;

import static net.maisikoleni.javadoc.search.TrieSearchEngineUtils.*;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.util.Cache;
import net.maisikoleni.javadoc.util.WeakCommonPool;
import net.maisikoleni.javadoc.util.regex.CharClass;
import net.maisikoleni.javadoc.util.regex.Regex;
import net.maisikoleni.javadoc.util.trie.Trie;

public class TrieGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(TrieGenerator.class);

	private static final Regex REGEX_DIVIDER = new CharClass(c -> c == '.' || c == '/',
			"[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Regex REGEX_START_AFTER = new CharClass(c -> isSeparator(c) || c == '_' || c == ' ',
			"[" + SEPARATOR_CHAR_CLASS + "_ ]");
	private static final Regex REGEX_START_BEFORE = new CharClass(
			c -> isSeparator(c) || Character.isUpperCase(c) || c == '_',
			"[" + SEPARATOR_CHAR_CLASS + "_\\p{javaUpperCase}]");

	private final boolean parallel;
	private Trie.CompressionCache cache;
	private final WeakCommonPool pool;

	public TrieGenerator(boolean parallel) {
		this.parallel = parallel;
		this.cache = newCache();
		if (parallel)
			pool = WeakCommonPool.get();
		else
			pool = null;
	}

	private Trie.CompressionCache newCache() {
		if (parallel)
			return new Trie.CompressionCache(Cache::newConcurrent);
		return new Trie.CompressionCache(Cache::newDefault);
	}

	public final void clearCache() {
		cache = newCache();
	}

	public final <S extends SearchableEntity, R, T extends Trie<R>> T generateTrie(Stream<S> index,
			Supplier<T> trieSupplier, SubdividedEntityFunction<S, R> converter) {
		if (index.isParallel() != parallel)
			LOG.warn("TrieGenerator parallelism does not match index stream");
		if (index.isParallel() && !parallel)
			throw new IllegalArgumentException("index stream must not be parallel for non-parallel TrieGenerator");
		Supplier<T> createTrie = () -> generateTrieDirectly(index, trieSupplier, converter);
		if (parallel) {
			@SuppressWarnings("resource")
			var trieTask = pool.forkJoinPool().submit(createTrie::get);
			return trieTask.join();
		}
		return createTrie.get();
	}

	private <S extends SearchableEntity, T extends Trie<R>, R> T generateTrieDirectly(Stream<S> index,
			Supplier<T> trieSupplier, SubdividedEntityFunction<S, R> converter) {
		T trie = trieSupplier.get();
		var trieName = trie.getClass().getSimpleName();
		SubdividedEntityConsumer<S> addRanked = (name, entity, rank) -> trie.insert(name,
				converter.apply(name, entity, rank));
		long t1 = System.currentTimeMillis();
		index.forEach(se -> subdivideEntity(se, addRanked));
		long t2 = System.currentTimeMillis();
		LOG.info("Constructing trie took {} ms (trie: {}, parallel: {})", t2 - t1, trieName, parallel);
		trie.compress(cache);
		long t3 = System.currentTimeMillis();
		LOG.info("Compressing trie took {} ms (trie: {}, parallel: {})", t3 - t2, trieName, parallel);
		return trie;
	}

	static <T extends SearchableEntity> void subdivideEntity(T se, SubdividedEntityConsumer<T> consumer) {
		var preProcessedName = TrieSearchEngineUtils.generateTrieName(se);
		consumer.accept(preProcessedName, se, 3.0);
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
				if (TrieSearchEngineUtils.isUseful(namePart))
					consumer.accept(namePart, se, rank);
			}
		}
	}

	@FunctionalInterface
	private interface SubdividedEntityConsumer<T> {

		void accept(CharSequence name, T entity, double rank);
	}

	@FunctionalInterface
	public interface SubdividedEntityFunction<T, R> {

		R apply(CharSequence name, T entity, double rank);

		static <T> SubdividedEntityFunction<T, T> identity() {
			return (name, entity, rank) -> entity;
		}
	}
}
