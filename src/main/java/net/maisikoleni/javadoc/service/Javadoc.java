package net.maisikoleni.javadoc.service;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.maisikoleni.javadoc.config.ValidLibraryId;
import net.maisikoleni.javadoc.entities.JavadocIndex;

@JsonSerialize(as = Javadoc.class)
public interface Javadoc {

	/**
	 * Short and well defined format, see
	 * {@link ValidLibraryId#LIBRARY_ID_PATTERN_STRING}
	 */
	@JsonSerialize
	String id();

	/**
	 * May change
	 */
	@JsonSerialize
	String name();

	/**
	 * Should be permanent
	 */
	@JsonSerialize
	String description();

	@JsonIgnore
	JavadocIndex index();

	@JsonSerialize
	URI baseUrl();

}
