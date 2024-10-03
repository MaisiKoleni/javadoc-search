package net.maisikoleni.javadoc.search;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.SearchService;
import net.maisikoleni.javadoc.service.SearchServiceProvider.FixLibraryId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RegexSearchEngineTest {

	@Inject
	@FixLibraryId
	SearchService searchService;

	@Test
	void testSearchGroupedByType() {
		var engine = new RegexSearchEngine(searchService.javadoc().index());
		var result = engine.searchGroupedByType("Set");

		assertThat(result.modules()).isEmpty();
		assertThat(result.packages()).isEmpty();
		assertThat(result.types().map(SearchableEntity::qualifiedName).toList()) //
				.startsWith( //
						"java.base/java.util.Set", //
						"java.desktop/javax.print.attribute.SetOfIntegerSyntax", //
						"jdk.jfr/jdk.jfr.SettingControl", //
						"jdk.jfr/jdk.jfr.SettingDefinition", //
						"jdk.jfr/jdk.jfr.SettingDescriptor", //
						"jdk.management.jfr/jdk.management.jfr.SettingDescriptorInfo", //
						"java.base/java.util.AbstractSet", //
						"java.desktop/javax.accessibility.AccessibleRelationSet", //
						"java.desktop/javax.accessibility.AccessibleStateSet", //
						"java.desktop/javax.print.attribute.AttributeSet", //
						"java.desktop/javax.swing.text.AttributeSet", //
						"java.desktop/javax.print.attribute.AttributeSetUtilities", //
						"java.desktop/javax.imageio.plugins.tiff.BaselineTIFFTagSet", //
						"java.sql.rowset/javax.sql.rowset.BaseRowSet", //
						"java.base/java.util.BitSet") //
				.hasSize(72);
		assertThat(result.members().map(SearchableEntity::qualifiedName).toList()) //
				.startsWith( //
						"java.base/java.nio.file.attribute.PosixFilePermissions.asFileAttribute(Set<PosixFilePermission>)", //
						"java.base/java.util.Collections.checkedSet(Set<E>, Class<E>)", //
						"jdk.jfr/jdk.jfr.SettingControl.combine(Set<String>)", //
						"java.compiler/javax.lang.model.util.ElementFilter.constructorsIn(Set<? extends Element>)", //
						"java.base/java.lang.module.ModuleDescriptor.Builder.exports(Set<ModuleDescriptor.Exports.Modifier>, String)", //
						"java.base/java.lang.module.ModuleDescriptor.Builder.exports(Set<ModuleDescriptor.Exports.Modifier>, String, Set<String>)", //
						"java.compiler/javax.lang.model.util.ElementFilter.fieldsIn(Set<? extends Element>)", //
						"java.desktop/java.awt.font.NumericShaper.getContextualShaper(Set<NumericShaper.Range>)", //
						"java.desktop/java.awt.font.NumericShaper.getContextualShaper(Set<NumericShaper.Range>, NumericShaper.Range)", //
						"java.compiler/javax.annotation.processing.RoundEnvironment.getElementsAnnotatedWithAny(Set<Class<? extends Annotation>>)", //
						"java.base/java.lang.StackWalker.getInstance(Set<StackWalker.Option>)", //
						"java.base/java.lang.StackWalker.getInstance(Set<StackWalker.Option>, int)") //
				.hasSize(737);
		assertThat(result.tags().map(SearchableEntity::qualifiedName).toList()).containsExactly(
				"Setting a Security Manager", //
				"Setting Initial Permissions", //
				"Setting the ACL when creating a file", //
				"1.0 Creating a CachedRowSet Object", //
				"2.0 Creating a JdbcRowSet Object", //
				"2.0 How a RowSet Object Gets Its Provider", //
				"2.0 Retrieving Data from a CachedRowSet Object", //
				"2.0 Setting Properties", //
				"2.0 Standard RowSet Definitions", //
				"2.0 Using a JoinRowSet Object for Creating a JOIN", //
				"2.0 WebRowSet States", //
				"2.1 Retrieving RowSetMetaData", //
				"2.1 State 1 - Outputting a WebRowSet Object to XML", //
				"3.0 Setting the Command and Its Parameters", //
				"3.0 Updating a CachedRowSet Object", //
				"4.0 JoinRowSet Methods", //
				"4.0 Updating a FilteredRowSet Object", //
				"9.0 Setting Properties", //
				"Attribute Sets", //
				"Character Sets", //
				"Character Sets", //
				"Class AccessibleStateSet", //
				"Getting and Setting Calendar Field Values", //
				"Unmodifiable Sets");
	}
}
