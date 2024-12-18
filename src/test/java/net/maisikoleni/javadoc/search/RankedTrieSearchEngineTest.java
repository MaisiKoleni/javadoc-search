package net.maisikoleni.javadoc.search;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.search.RankedTrieSearchEngine.RankedEntry;
import net.maisikoleni.javadoc.service.SearchService;
import net.maisikoleni.javadoc.service.SearchServiceProvider.FixLibraryId;
import net.maisikoleni.javadoc.util.trie.RankedTrie.RankedSimpleTrie;
import net.maisikoleni.javadoc.util.trie.Trie;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
class RankedTrieSearchEngineTest {

	/**
	 * This is expected to be stable on the premise that String.hashCode unchanged.
	 */
	private static final int EXPECTED_HASH_CODE = 2108656732;

	@Inject
	@FixLibraryId
	SearchService searchService;

	@Test
	void testGenerateRankedTrieConcurrently() {
		var index = searchService.javadoc().index();
		var completeConcurrentTrie = new RankedTrieSearchEngine.RankedConcurrentTrieGenerator()
				.generateTrie(index.stream());
		var completeSerialTrie = rankedSimpleTrieFrom(index.stream());

		var concurrentHashCode = completeConcurrentTrie.hashCode();
		var serialHashCode = completeSerialTrie.hashCode();

		/*
		 * Doing a more unusual assertions here because everything is slow here and a
		 * toString() in the failure reposting has the potential to make everything go
		 * unresponsive or effectively kill the process.
		 */
		assertAll(() -> {
			if (concurrentHashCode != serialHashCode)
				fail("concurrent: %d != serial: %d".formatted(concurrentHashCode, serialHashCode));
		}, () -> {
			if (!completeConcurrentTrie.equals(completeSerialTrie))
				fail("concurrent and serial are not equal");
		}, () -> {
			assertThat(concurrentHashCode).isEqualTo(EXPECTED_HASH_CODE);
		}, () -> {
			assertThat(serialHashCode).isEqualTo(EXPECTED_HASH_CODE);
		});
	}

	static <S extends SearchableEntity> Trie<RankedEntry<S>> rankedSimpleTrieFrom(Stream<S> index) {
		return new TrieGenerator(false).generateTrie(index, RankedTrieSearchEngineTest::newSimpleRankedTrie,
				RankedEntry::from);
	}

	static <S extends SearchableEntity> RankedSimpleTrie<RankedEntry<S>> newSimpleRankedTrie() {
		return new RankedSimpleTrie<>(RankedTrieSearchEngine.SearchableEntityRankingFunction.get());
	}
}
