package net.maisikoleni.javadoc.db;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public final class TestJavadocIndexes implements JavadocIndexes {

	private final Map<URI, JavadocIndex> indexesByUri = new ConcurrentHashMap<>();

	@Override
	public JavadocIndex getIndexByBaseUrl(URI baseUrl, Supplier<JavadocIndex> alternativeSource) {
		return indexesByUri.computeIfAbsent(baseUrl, url -> alternativeSource.get());
	}

	public Map<URI, JavadocIndex> getRawIndexesByUriMap() {
		return indexesByUri;
	}
}
