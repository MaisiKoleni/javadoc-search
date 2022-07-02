package net.maisikoleni.javadoc.search;

import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.SearchableEntity;

public interface SearchEngine {

	Stream<SearchableEntity> search(String query);

	GroupedSearchResult searchGroupedByType(String query);
}
