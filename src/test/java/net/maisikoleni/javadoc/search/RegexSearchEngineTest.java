package net.maisikoleni.javadoc.search;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@QuarkusTest
class RegexSearchEngineTest {

	@Inject
	@Jdk(Version.RELEASE_18)
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
				.hasSize(71);
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
				.hasSize(727);
		assertThat(result.tags()).isEmpty();
	}
}
