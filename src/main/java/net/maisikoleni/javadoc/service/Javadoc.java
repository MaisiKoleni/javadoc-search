package net.maisikoleni.javadoc.service;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public interface Javadoc {

	String id();

	String name();

	@JsonIgnore
	JavadocIndex index();

	URI baseUrl();

}
