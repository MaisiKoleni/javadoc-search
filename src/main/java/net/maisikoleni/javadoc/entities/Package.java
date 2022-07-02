package net.maisikoleni.javadoc.entities;

import java.util.stream.Stream;

public final class Package extends NestedSearchableEntity<Module> {

	public static final String UNNAMED = "<Unnamed>";

	public Package(Module container) {
		super(container, UNNAMED);
	}

	public Package(Module container, String name) {
		super(container, name);
	}

	public Package(Module container, String name, String urlName) {
		super(container, name, urlName);
	}

	@Override
	protected Stream.Builder<String> qualifiedNameSegments(Stream.Builder<String> builder) {
		if (container() != null)
			return container().qualifiedNameSegments(builder).add(SLASH).add(name());
		return builder.add(name());
	}

	public boolean isUnnamed() {
		return UNNAMED.equals(name());
	}

	@Override
	protected String urlPrefix() {
		// Move UNNAMED from urlPrefix to url because of package as container
		return concatUrl(super.urlPrefix(), isUnnamed() ? "" : name().replace(DOT, SLASH));
	}

	@Override
	public String url() {
		if (hasUrlName())
			return urlName();
		// Move UNNAMED from urlPrefix to url because of package as container
		return concatUrl(urlPrefix(), isUnnamed() ? UNNAMED : "", "package-summary.html");
	}

}
