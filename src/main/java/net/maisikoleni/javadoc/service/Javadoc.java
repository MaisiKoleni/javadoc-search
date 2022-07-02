package net.maisikoleni.javadoc.service;

import java.net.URI;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public interface Javadoc {

	JavadocIndex index();

	URI baseUrl();

}
