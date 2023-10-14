package net.maisikoleni.javadoc.entities;

import static java.util.Objects.requireNonNull;

public final class Tag extends SearchableEntity {

	private final String host;
	private final String description;
	private final String url;

	public Tag(String host, String name, String description, String url) {
		super(name);
		this.host = host;
		this.description = description;
		this.url = requireNonNull(url);
	}

	public String host() {
		return host;
	}

	public String description() {
		return description;
	}

	@Override
	public String url() {
		return url;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Tag tag)
			return url().equals(tag.url());
		return false;
	}

	@Override
	public int hashCode() {
		return url().hashCode();
	}
}
