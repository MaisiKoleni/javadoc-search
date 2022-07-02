package net.maisikoleni.javadoc.entities;

public final class Type extends NestedSearchableEntity<Package> {

	public Type(Package container, String name) {
		super(container, name);
	}

	public Type(Package container, String name, String urlName) {
		super(container, name, urlName);
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
