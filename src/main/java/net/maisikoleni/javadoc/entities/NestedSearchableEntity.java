package net.maisikoleni.javadoc.entities;

import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public abstract sealed class NestedSearchableEntity<C extends SearchableEntity> // $NOSONAR$ - equals checks class
		extends SearchableEntity permits Package, Type, Member {

	private final C container;
	private final String urlName;

	protected NestedSearchableEntity(C container, String name, String urlName) {
		super(name);
		this.container = container;
		this.urlName = urlName;
		if (container instanceof NestedSearchableEntity<?> nse && nse.hasUrlName())
			throw new IllegalArgumentException("Containing entity must not have a URL name");
	}

	protected NestedSearchableEntity(C container, String name) {
		this(container, name, null);
	}

	@Override
	protected Stream.Builder<String> qualifiedNameSegments(Stream.Builder<String> builder) {
		if (container != null)
			return nameSegments(container.qualifiedNameSegments(builder).add(DOT));
		return nameSegments(builder);
	}

	protected Builder<String> nameSegments(Builder<String> builder) {
		return builder.add(name());
	}

	protected String urlName() {
		return urlName;
	}

	protected boolean hasUrlName() {
		return urlName != null;
	}

	public final C container() {
		return container;
	}

	@Override
	protected String urlPrefix() {
		return container == null ? "" : container.urlPrefix();
	}
}
