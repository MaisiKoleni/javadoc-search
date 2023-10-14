package net.maisikoleni.javadoc.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.SearchService;
import net.maisikoleni.javadoc.service.SearchServiceProvider.FixLibraryId;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

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
				.hasSize(72);
		assertThat(result.members().map(SearchableEntity::qualifiedName).toList()) //
				.contains( //
						"java.base/java.util.Calendar.isSet", //
						"java.base/java.util.AbstractSet.AbstractSet()", //
						"java.base/java.util.BitSet.BitSet()", //
						"java.base/java.util.EnumMap.keySet()", //
						"jdk.jfr/jdk.jfr.Recording.getSettings()") //
				.hasSize(736);
		assertThat(result.tags().map(SearchableEntity::qualifiedName).toList()).containsExactly(
				"Class AccessibleStateSet", //
				"2.0 Standard RowSet Definitions", //
				"4.0 JoinRowSet Methods", //
				"3.0 Updating a CachedRowSet Object", //
				"1.0 Creating a CachedRowSet Object", //
				"2.0 Retrieving Data from a CachedRowSet Object", //
				"4.0 Updating a FilteredRowSet Object", //
				"2.0 Creating a JdbcRowSet Object", //
				"2.0 How a RowSet Object Gets Its Provider", //
				"2.0 Using a JoinRowSet Object for Creating a JOIN", //
				"2.1 State 1 - Outputting a WebRowSet Object to XML", //
				"2.0 WebRowSet States", //
				"2.1 Retrieving RowSetMetaData", //
				"Character Sets", //
				"Character Sets", //
				"Unmodifiable Sets", //
				"Attribute Sets", //
				"Getting and Setting Calendar Field Values", //
				"Setting Initial Permissions", //
				"9.0 Setting Properties", //
				"2.0 Setting Properties", //
				"Setting a Security Manager", //
				"Setting the ACL when creating a file", //
				"3.0 Setting the Command and Its Parameters");
	}
}
