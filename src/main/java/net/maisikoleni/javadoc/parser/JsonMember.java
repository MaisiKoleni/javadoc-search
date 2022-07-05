package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNullOrNonEmpty;

record JsonMember( //
		/**
		 * Module, may be <code>null</code>.
		 */
		String m, //
		/**
		 * Package, may be <code>null</code>.
		 */
		String p, //
		/**
		 * Class/type, may be <code>null</code>.
		 */
		String c, //
		/**
		 * Label.
		 */
		String l, //
		/**
		 * URL.
		 */
		String u) implements JsonSearchablePackagedEntity {

	JsonMember {
		requireNullOrNonEmpty(m, "Module 'm'");
		requireNonEmpty(p, "Package 'p'");
		requireNonEmpty(c, "Class/type 'c'");
		requireNonEmpty(l, "Label 'l'");
		requireNullOrNonEmpty(u, "URL 'u'");
	}
}
