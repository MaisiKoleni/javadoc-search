package net.maisikoleni.javadoc.service;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.maisikoleni.javadoc.config.Configuration;
import net.maisikoleni.javadoc.config.Configuration.LibraryConfigValue;
import net.maisikoleni.javadoc.db.JavadocIndexes;
import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.search.RankedTrieSearchEngine;
import net.maisikoleni.javadoc.search.SearchEngine;

import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Startup
@Singleton
public class JavadocSearchEngines {

	private final String defaultLibraryId;
	private final Map<String, Javadoc> installedJavadocs;
	private final Map<URI, SearchEngine> searchEngines;

	@Inject
	public JavadocSearchEngines(Configuration configuration, JavadocIndexes javadocIndexes) {
		var libraries = configuration.libraries();
		defaultLibraryId = extractDefaultLibraryId(libraries);
		installedJavadocs = libraries.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> entryToJavadoc(entry, javadocIndexes)));
		var commonGenerator = RankedTrieSearchEngine.RankedConcurrentTrieGenerator.of();
		record IndexWithBaseUrl(URI baseUrl, JavadocIndex index) {}
		searchEngines = installedJavadocs.values().stream()
				.map(javadoc -> new IndexWithBaseUrl(javadoc.baseUrl(), javadoc.index())).distinct()
				.collect(Collectors.toMap(IndexWithBaseUrl::baseUrl,
						indexWithBaseUrl -> new RankedTrieSearchEngine(indexWithBaseUrl.index(), commonGenerator)));
	}

	public Collection<Javadoc> allJavadocs() {
		return installedJavadocs.values();
	}

	public String getDefaultLibraryId() {
		return defaultLibraryId;
	}

	public Javadoc getJavadoc(String id) {
		return installedJavadocs.get(id);
	}

	public SearchEngine getSearchEngine(String id) {
		return searchEngines.get(getJavadoc(id).baseUrl());
	}

	private static String extractDefaultLibraryId(Map<String, LibraryConfigValue> map) {
		var defaultLibraries = map.entrySet().stream().filter(entry -> entry.getValue().isDefault()).toList();
		int defaultLibraryCount = defaultLibraries.size();
		if (defaultLibraryCount != 1)
			throw new IllegalStateException(
					"The javadoc-search application must be configured with exactly one default library, not "
							+ defaultLibraryCount);
		return defaultLibraries.get(0).getKey();
	}

	static Javadoc entryToJavadoc(Map.Entry<String, LibraryConfigValue> configEntry, JavadocIndexes javadocIndexes) {
		var libraryConfigValue = configEntry.getValue();
		return new JavadocImpl(configEntry.getKey(), libraryConfigValue.name(), libraryConfigValue.description(),
				libraryConfigValue.baseUrl(), javadocIndexes);
	}
}
