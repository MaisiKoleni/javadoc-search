package net.maisikoleni.javadoc.service;

import java.net.URI;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.search.SearchEngine;

public interface SearchService {

	String name();

	Javadoc javadoc();

	SearchEngine searchEngine();

	default URI getBestUrl(String query) {
		var results = searchEngine().search(query);
		var baseUrl = javadoc().baseUrl();
		return results.findFirst().map(SearchableEntity::url).map(baseUrl::resolve).orElse(baseUrl);
	}
}
