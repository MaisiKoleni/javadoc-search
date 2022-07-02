package net.maisikoleni.javadoc.db;

import java.net.URI;
import java.util.function.Supplier;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public interface JavadocIndexes {

	JavadocIndex getIndexByBaseUrl(URI baseUrl, Supplier<JavadocIndex> alternativeSource);
}
