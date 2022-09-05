package net.maisikoleni.javadoc.service;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.maisikoleni.javadoc.config.ValidLibraryId;
import net.maisikoleni.javadoc.entities.JavadocIndex;

public interface Javadoc {

	/**
	 * Short and well defined format, see
	 * {@link ValidLibraryId#LIBRARY_ID_PATTERN_STRING}
	 */
	String id();

	/**
	 * May change
	 */
	String name();

	/**
	 * Should be permanent
	 */
	String description();

	@JsonIgnore
	JavadocIndex index();

	URI baseUrl();

}
