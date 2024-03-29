package net.maisikoleni.javadoc.parser;

import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonEmpty;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNonNull;
import static net.maisikoleni.javadoc.parser.JsonSearchableEntity.requireNullOrNonEmpty;

import com.fasterxml.jackson.annotation.JsonAlias;

record JsonTag( //
		/**
		 * Label.
		 */
		String l, //
		/**
		 * Holder, may be empty.
		 */
		String h, //
		/**
		 * Description, may be <code>null</code>.
		 */
		String d, //
		/**
		 * URL.
		 */
		@JsonAlias("url") String u) implements JsonSearchableEntity {

	JsonTag {
		requireNonEmpty(l, "Label 'l'");
		requireNonNull(h, "Holder 'h'");
		requireNullOrNonEmpty(d, "Description 'd'");
		requireNonEmpty(u, "URL 'u'");
	}
}
