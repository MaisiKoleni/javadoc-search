package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNullOrNonEmpty;

record JsonType( //
		/**
		 * Package, may be <code>null</code>.
		 */
		String p, //
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
		String u) implements JsonSearchablePackagedEntity {

	JsonType {
		requireNullOrNonEmpty(m, "Module 'm'");
		requireNullOrNonEmpty(p, "Package 'p'");
		requireNonEmpty(l, "Label 'l'");
		requireNullOrNonEmpty(u, "URL 'u'");
	}
}
