package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonNull;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNullOrNonEmpty;

import com.fasterxml.jackson.annotation.JsonAlias;

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
		@JsonAlias("url") String u) implements JsonSearchablePackagedEntity {

	JsonMember {
		requireNullOrNonEmpty(m, "Module 'm'");
		// FIXME p and c must not be empty, this is due to a JDK Javadoc bug
		requireNonNull(p, "Package 'p'");
		requireNonNull(c, "Class/type 'c'");
		requireNonEmpty(l, "Label 'l'");
		requireNullOrNonEmpty(u, "URL 'u'");
	}
}
