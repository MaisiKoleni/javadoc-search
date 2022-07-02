package net.maisikoleni.javadoc.entities;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes") // Current eclipse compiler bug
public abstract sealed class SearchableEntity
		implements Comparable<SearchableEntity>permits Module, NestedSearchableEntity, Tag {

	protected static final String DOT = ".";
	protected static final String HASHTAG = "#";
	protected static final String SLASH = "/";
	protected static final String HTML_SUFFIX = ".html";

	private final String name;
	private CharSequence qualifiedName;

	protected SearchableEntity(String name) {
		this.name = requireNonNull(name);
	}

	public final String name() {
		return name;
	}

	public final CharSequence qualifiedName() {
		if (qualifiedName == null)
			qualifiedName = qualifiedNameSegments().collect(Collectors.joining());
		return qualifiedName;
	}

	public final Stream<String> qualifiedNameSegments() {
		return qualifiedNameSegments(Stream.builder()).build();
	}

	protected Stream.Builder<String> qualifiedNameSegments(Stream.Builder<String> builder) {
		return builder.add(name);
	}

	protected String urlPrefix() {
		return name;
	}

	public abstract String url();

	public final String url(URI baseUrl) {
		return baseUrl.resolve(url()).toString();
	}

	protected static String concatUrl(String... segments) {
		return Stream.of(segments).filter(not(String::isEmpty)).collect(Collectors.joining(SLASH));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof SearchableEntity se)
			return qualifiedName().equals(se.qualifiedName());
		return false;
	}

	@Override
	public int hashCode() {
		return qualifiedName().hashCode();
	}

	@Override
	public String toString() {
		return qualifiedName().toString();
	}

	@Override
	public int compareTo(SearchableEntity o) {
		return CharSequence.compare(qualifiedName(), o.qualifiedName());
	}
}