package net.maisikoleni.javadoc.parser;

import java.util.Objects;

sealed interface JsonSearchableEntity permits JsonModule, JsonPackage, JsonSearchablePackagedEntity, JsonTag {

	String l();

	static void requireNonNull(String s, String name) {
		Objects.requireNonNull(s, name);
	}

	static void requireNonEmpty(String s, String name) {
		requireNonNull(s, name);
		if (s.isEmpty())
			throw new IllegalArgumentException(name + " must not be empty.");
	}

	static void requireNullOrNonEmpty(String s, String name) {
		if (s != null && s.isEmpty())
			throw new IllegalArgumentException(name + " must not be empty.");
	}
}
