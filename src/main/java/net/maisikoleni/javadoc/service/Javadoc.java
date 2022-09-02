package net.maisikoleni.javadoc.service;

import java.net.URI;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public interface Javadoc {

	String id();

	String name();

	JavadocIndex index();

	URI baseUrl();

}
