package net.maisikoleni.javadoc.service;

import java.net.URI;
import java.util.Objects;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public record SimpleJavadoc(String id, String name, String description, JavadocIndex index, URI baseUrl)
		implements Javadoc {

	public SimpleJavadoc {
		if (id.isBlank())
			throw new IllegalArgumentException("id must not be empty");
		if (name.isBlank())
			throw new IllegalArgumentException("name must not be empty");
		if (description.isBlank())
			throw new IllegalArgumentException("description must not be empty");
		Objects.requireNonNull(index);
		Objects.requireNonNull(baseUrl);
	}
}
