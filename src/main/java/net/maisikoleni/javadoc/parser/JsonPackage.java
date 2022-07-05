package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNullOrNonEmpty;

record JsonPackage( //
		/**
		 * Module, may be <code>null</code>.
		 */
		String m, //
		/**
		 * Label.
		 */
		String l, //
		/**
		 * URL, may be <code>null</code>.
		 */
		String u) implements JsonSearchableEntity {

	JsonPackage {
		requireNullOrNonEmpty(m, "Module 'm'");
		requireNonEmpty(l, "Label 'l'");
		requireNullOrNonEmpty(u, "URL 'u'");
	}
}
