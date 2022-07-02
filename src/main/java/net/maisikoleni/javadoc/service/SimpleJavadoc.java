package net.maisikoleni.javadoc.service;

import java.net.URI;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public record SimpleJavadoc(JavadocIndex index, URI baseUrl) implements Javadoc{
	// just a record
}
