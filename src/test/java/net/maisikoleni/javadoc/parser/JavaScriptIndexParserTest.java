package net.maisikoleni.javadoc.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JavaScriptIndexParserTest {

	static final String MODULE_INDEX = """
			[{
			    "l": "java.base"
			}]""";
	static final JsonModule[] EXPECTED_MODULES = { //
			new JsonModule("java.base") //
	};

	static final String PACKAGE_INDEX = """
			[{
			    "l": "All Packages",
			    "u": "allpackages-index.html"
			}, {
			    "m": "java.base",
			    "l": "java.io"
			}]""";
	static final JsonPackage[] EXPECTED_PACKAGES = { //
			new JsonPackage(null, "All Packages", "allpackages-index.html"), //
			new JsonPackage("java.base", "java.io", null) //
	};

	static final String TYPE_INDEX = """
			[{
			    "l": "All Classes and Interfaces",
			    "u": "allclasses-index.html"
			}, {
			    "p": "java.io",
			    "m": "java.base",
			    "l": "SelectorProvider"
			}, {
			    "p": "java.io",
			    "l": "HTML.Attribute"
			}]""";
	static final JsonType[] EXPECTED_TYPES = { //
			new JsonType(null, null, "All Classes and Interfaces", "allclasses-index.html"), //
			new JsonType("java.io", "java.base", "SelectorProvider", null), //
			new JsonType("java.io", null, "HTML.Attribute", null) //
	};

	static final String MEMBER_INDEX = """
			[{
			    "p": "java.io",
			    "c": "SelectorProvider",
			    "l": "getDividerLocation(JSplitPane)",
			    "u": "getDividerLocation(javax.swing.JSplitPane)"
			}, {
			    "p": "java.io",
			    "c": "SelectorProvider",
			    "l": "SelectorProvider(Class<? extends Enum>, String)",
			    "u": "%3Cinit%3E(java.lang.Class,java.lang.String)"
			}, {
			    "p": "java.io",
			    "c": "HTML.Attribute",
			    "l": "abs()"
			}, {
			    "p": "java.io",
			    "c": "HTML.Attribute",
			    "l": "ABSTRACT"
			}, {
			    "m": "java.base",
			    "p": "java.io",
			    "c": "HTML.Attribute",
			    "l": "Attribute()",
			    "u": "%3Cinit%3E()"
			}]""";
	static final JsonMember[] EXPECTED_MEMBERS = { //
			new JsonMember(null, "java.io", "SelectorProvider", "getDividerLocation(JSplitPane)",
					"getDividerLocation(javax.swing.JSplitPane)"), //
			new JsonMember(null, "java.io", "SelectorProvider", "SelectorProvider(Class<? extends Enum>, String)",
					"%3Cinit%3E(java.lang.Class,java.lang.String)"), //
			new JsonMember(null, "java.io", "HTML.Attribute", "abs()", null), //
			new JsonMember(null, "java.io", "HTML.Attribute", "ABSTRACT", null), //
			new JsonMember("java.base", "java.io", "HTML.Attribute", "Attribute()", "%3Cinit%3E()") //
	};

	static final String TAG_INDEX = """
			[{
			    "l": "file.encoding",
			    "h": "java.lang.System.getProperties()",
			    "d": "System Property",
			    "u": "java.base/java/lang/System.html#file.encoding"
			}, {
			    "l": "Annotation Processing",
			    "h": "module java.compiler",
			    "u": "java.compiler/module-summary.html#AnnotationProcessing"
			}, {
			    "l": "Serialized Form",
			    "h": "",
			    "u": "serialized-form.html"
			}]""";
	static final JsonTag[] EXPECTED_TAGS = { //
			new JsonTag("file.encoding", "java.lang.System.getProperties()", "System Property",
					"java.base/java/lang/System.html#file.encoding"), //
			new JsonTag("Annotation Processing", "module java.compiler", null,
					"java.compiler/module-summary.html#AnnotationProcessing"), //
			new JsonTag("Serialized Form", "", null, "serialized-form.html") //
	};

	static final String MEMBER_INDEX_JS = """
			memberSearchIndex = %s;
			updateSearchResults();""".formatted(MEMBER_INDEX);

	@MethodSource("indexes")
	@ParameterizedTest(name = "{index}: {2} conversion")
	<T extends JsonSearchableEntity> void testParseJavaScriptIndexJson(String source, T[] expected,
			Class<T> targetType) {
		var jsonMembers = JavaScriptIndexParser.parseJavaScriptIndex(source, targetType);
		assertThat(jsonMembers).containsExactly(expected);
	}

	@Test
	void testParseJavaScriptIndexJavascript() {
		var jsonMembers = JavaScriptIndexParser.parseJavaScriptIndex(MEMBER_INDEX_JS, JsonMember.class);
		assertThat(jsonMembers).containsExactly(EXPECTED_MEMBERS);
	}

	static List<Arguments> indexes() {
		return List.of(arguments(MODULE_INDEX, EXPECTED_MODULES, JsonModule.class),
				arguments(PACKAGE_INDEX, EXPECTED_PACKAGES, JsonPackage.class),
				arguments(TYPE_INDEX, EXPECTED_TYPES, JsonType.class),
				arguments(MEMBER_INDEX, EXPECTED_MEMBERS, JsonMember.class),
				arguments(TAG_INDEX, EXPECTED_TAGS, JsonTag.class));
	}

	@Test
	void testParseJavaScriptIndexInvalid() {
		assertThatThrownBy(() -> JavaScriptIndexParser.parseJavaScriptIndex("abc", JsonMember.class))
				.isInstanceOf(IllegalArgumentException.class).message()
				.isEqualTo("invalid javadoc search index content");
	}

	@Test
	void testParseJavaScriptIndexes() {
		var jsonIndex = createTestJsonJavadocIndex();
		assertThat(jsonIndex.modules()).containsExactly(EXPECTED_MODULES);
		assertThat(jsonIndex.packages()).containsExactly(EXPECTED_PACKAGES);
		assertThat(jsonIndex.types()).containsExactly(EXPECTED_TYPES);
		assertThat(jsonIndex.members()).containsExactly(EXPECTED_MEMBERS);
		assertThat(jsonIndex.tags()).containsExactly(EXPECTED_TAGS);
	}

	static JsonJavadocIndex createTestJsonJavadocIndex() {
		return JavaScriptIndexParser.parseJavaScriptIndexes(MODULE_INDEX, PACKAGE_INDEX, TYPE_INDEX, MEMBER_INDEX,
				TAG_INDEX);
	}

}
