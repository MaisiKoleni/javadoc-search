package net.maisikoleni.javadoc.service;

import java.net.URI;

import javax.ws.rs.NotFoundException;

import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.search.SearchEngine;

public class SearchService {

	private final Javadoc javadoc;
	private final SearchEngine searchEngine;

	SearchService(JavadocSearchEngines javadocSearchEngines, String id) {
		this.javadoc = javadocSearchEngines.getJavadoc(id);
		if (this.javadoc == null)
			throw new NotFoundException("No Javadoc with id '" + id + "' registered.");
		this.searchEngine = javadocSearchEngines.getSearchEngine(id);
	}

	public Javadoc javadoc() {
		return javadoc;
	}

	public SearchEngine searchEngine() {
		return searchEngine;
	}

	public URI getBestUrl(String query) {
		var results = searchEngine().search(query);
		var baseUrl = javadoc().baseUrl();
		try {
			return results.findFirst().map(SearchableEntity::url).map(baseUrl::resolve).orElse(baseUrl);
		} catch (IllegalArgumentException e) {
			LoggerFactory.getLogger(SearchService.class).warn("Failed to generate URL, using base URL.", e);
			return baseUrl;
		}
	}
}
