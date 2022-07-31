package net.maisikoleni.javadoc.service;

import java.net.URI;

import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.search.SearchEngine;

public interface SearchService {

	String name();

	Javadoc javadoc();

	SearchEngine searchEngine();

	default URI getBestUrl(String query) {
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
