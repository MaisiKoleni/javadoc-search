package net.maisikoleni.javadoc.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public final class JavaScriptIndexParser {

	private static final Pattern JSON_ENTITY_ARRAY_PATTERN = Pattern
			.compile("^\\w++\\s*+=\\s*+([^;]++);\\s*+\\w++\\(\\)\\s*+;\\s*+$");

	private JavaScriptIndexParser() {
	}

	static <T extends JsonSearchableEntity> List<T> parseJavaScriptIndex(String content, Class<T> entityType) {
		/*
		 * If possible, deal with the fact that the index files are JavaScript and not
		 * JSON by extracting the JSON array in the assignment.
		 */
		Matcher matcher = JSON_ENTITY_ARRAY_PATTERN.matcher(content);
		String jsonEntityArray = matcher.matches() ? matcher.group(1) : content;
		try {
			return new ObjectMapper().readValue(jsonEntityArray,
					TypeFactory.defaultInstance().constructParametricType(List.class, entityType));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("invalid javadoc search index content", e);
		}
	}

	public static JsonJavadocIndex parseJavaScriptIndexes(String moduleSearchIndex, String packageSearchIndex,
			String typeSearchIndex, String memberSearchIndex, String tagSearchIndex) {
		return new JsonJavadocIndex(parseJavaScriptIndex(moduleSearchIndex, JsonModule.class),
				parseJavaScriptIndex(packageSearchIndex, JsonPackage.class),
				parseJavaScriptIndex(typeSearchIndex, JsonType.class),
				parseJavaScriptIndex(memberSearchIndex, JsonMember.class),
				parseJavaScriptIndex(tagSearchIndex, JsonTag.class));
	}
}
