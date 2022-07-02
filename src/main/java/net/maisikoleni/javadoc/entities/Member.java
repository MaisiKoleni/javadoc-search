package net.maisikoleni.javadoc.entities;

public final class Member extends NestedSearchableEntity<Type> {

	public Member(Type container, String name) {
		super(container, name);
	}

	public Member(Type container, String name, String urlName) {
		super(container, name, urlName);
	}

	@Override
	protected String urlPrefix() {
		return super.urlPrefix() + HASHTAG + (hasUrlName() ? urlName() : name());
	}

	@Override
	public String url() {
		return urlPrefix();
	}
}
