package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;

record JsonModule( //
		/**
		 * Label.
		 */
		String l) implements JsonSearchableEntity {

	JsonModule {
		requireNonEmpty(l, "Label 'l'");
	}
}
