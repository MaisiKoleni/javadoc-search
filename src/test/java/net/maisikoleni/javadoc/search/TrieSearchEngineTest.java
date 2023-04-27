package net.maisikoleni.javadoc.search;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.SearchService;
import net.maisikoleni.javadoc.service.SearchServiceProvider.FixLibraryId;

@QuarkusTest
class TrieSearchEngineTest {

	@Inject
	@FixLibraryId
	SearchService searchService;

	@Test
	void testSearchGroupedByType() {
		var engine = new TrieSearchEngine(searchService.javadoc().index());
		var result = engine.searchGroupedByType("Set");

		assertThat(result.modules()).isEmpty();
		assertThat(result.packages()).isEmpty();
		assertThat(result.types().map(SearchableEntity::qualifiedName).toList()) //
				.contains( //
						"java.base/java.util.Set", //
						"java.base/java.util.AbstractSet", //
						"java.base/java.util.EnumSet", //
						"java.sql/java.sql.ResultSet", //
						"jdk.jfr/jdk.jfr.SettingControl") //
				.hasSize(71);
		assertThat(result.members().map(SearchableEntity::qualifiedName).toList()) //
				.contains( //
						"java.base/java.util.Calendar.isSet", //
						"java.base/java.util.AbstractSet.AbstractSet()", //
						"java.base/java.util.BitSet.BitSet()", //
						"java.base/java.util.EnumMap.keySet()", //
						"jdk.jfr/jdk.jfr.Recording.getSettings()") //
				.hasSize(733);
		assertThat(result.tags()).isEmpty();
	}
}
