package net.maisikoleni.javadoc.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;

class JsonJavadocIndexTest {

	static Module JAVA_BASE = new Module("java.base");

	static Package ALL_PACKAGES = new Package(null, "All Packages", "allpackages-index.html");
	static Package JAVA_IO = new Package(JAVA_BASE, "java.io");

	static Type ALL_CLASSES = new Type(null, "All Classes and Interfaces", "allclasses-index.html");
	static Type SELECTOR_PROVIDER = new Type(JAVA_IO, "SelectorProvider");
	static Type HTML_ATTRIBUTE = new Type(JAVA_IO, "HTML.Attribute");

	static Member GET_DIVIDER_LOCATION = new Member(SELECTOR_PROVIDER, "getDividerLocation(JSplitPane)",
			"getDividerLocation(javax.swing.JSplitPane)");
	static Member SELECTOR_PROVIDER_CONSTRUCTOR = new Member(SELECTOR_PROVIDER,
			"SelectorProvider(Class<? extends Enum>, String)", "%3Cinit%3E(java.lang.Class,java.lang.String)");
	static Member ABS = new Member(HTML_ATTRIBUTE, "abs()");
	static Member ABSTRACT = new Member(HTML_ATTRIBUTE, "ABSTRACT");
	static Member ATTRIBUTE_CONSTRUCTOR = new Member(HTML_ATTRIBUTE, "Attribute()", "%3Cinit%3E()");

	static Tag FILE_ENCODING = new Tag("java.lang.System.getProperties()", "file.encoding", "System Property",
			"java.base/java/lang/System.html#file.encoding");
	static Tag ANNOTATION_PROCESSING = new Tag("module java.compiler", "Annotation Processing", null,
			"java.compiler/module-summary.html#AnnotationProcessing"); //
	static Tag SERIALIZED_FORM = new Tag("", "Serialized Form", null, "serialized-form.html");

	@Test
	void testToJavadocIndex() {
		var jsonIndex = JavaScriptIndexParserTest.createTestJsonJavadocIndex();
		var index = jsonIndex.toJavadocIndex();

		assertThat(index.modules()).containsExactly(JAVA_BASE);
		assertThat(index.packages()).containsExactly(ALL_PACKAGES, JAVA_IO);
		assertThat(index.types()).containsExactly(ALL_CLASSES, SELECTOR_PROVIDER, HTML_ATTRIBUTE);
		assertThat(index.members()).containsExactly(GET_DIVIDER_LOCATION, SELECTOR_PROVIDER_CONSTRUCTOR, ABS, ABSTRACT,
				ATTRIBUTE_CONSTRUCTOR);
		assertThat(index.tags()).containsExactly(FILE_ENCODING, ANNOTATION_PROCESSING, SERIALIZED_FORM);
	}

	@Test
	void testUrlCreation() {
		var jsonIndex = JavaScriptIndexParserTest.createTestJsonJavadocIndex();
		var index = jsonIndex.toJavadocIndex();
		var jsonUrls = jsonIndex.stream().map(entry -> JdkUrlGeneration.getUrl("", entry, jsonIndex.packages()))
				.toList();
		var urls = index.stream().map(SearchableEntity::url).toList();

		assertThat(urls).containsExactlyElementsOf(jsonUrls);
	}
}
