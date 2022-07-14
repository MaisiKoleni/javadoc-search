package net.maisikoleni.javadoc.entities;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Type extends NestedSearchableEntity<Package> {

	private static final Pattern SPLIT_NESTED_TYPES = Pattern.compile("(?<=\\.)|(?=\\.)");

	public Type(Package container, String name) {
		super(container, name);
	}

	public Type(Package container, String name, String urlName) {
		super(container, name, urlName);
	}

	@Override
	protected Stream.Builder<String> nameSegments(Stream.Builder<String> builder) {
		for (var nameSegment : SPLIT_NESTED_TYPES.split(name()))
			builder.add(nameSegment);
		return builder;
	}

	@Override
	protected String urlPrefix() {
		return concatUrl(super.urlPrefix(), name() + HTML_SUFFIX);
	}

	@Override
	public String url() {
		if (hasUrlName())
			return urlName();
		return urlPrefix();
	}
}
