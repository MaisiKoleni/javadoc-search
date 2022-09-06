package net.maisikoleni.javadoc.server;

import java.util.List;
import java.util.OptionalInt;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.SearchService;

@RequestScoped
@Path("/api/v2/libraries/{" + Constants.LIBRARY_ID_PARAMETER + "}/search")
public class SearchResource implements JavadocSearch {

	private final SearchService searchSerivce;
	private final SearchValidator searchValidator;
	private final SearchReporter searchReporter;

	@Inject
	public SearchResource(SearchService searchSerivce, SearchValidator searchValidator, SearchReporter searchReporter) {
		this.searchSerivce = searchSerivce;
		this.searchValidator = searchValidator;
		this.searchReporter = searchReporter;
	}

	@Override
	public Response searchAndRedirect(String query) {
		searchValidator.validateQuery(query);
		var startTime = System.nanoTime();
		var destination = searchSerivce.getBestUrl(query);
		searchReporter.logSearchTime("api/redirect", query, startTime);
		return Response.seeOther(destination).build();
	}

	@Override
	public List<String> suggestions(String query, OptionalInt count) {
		searchValidator.validateQuery(query);
		if (count.isPresent())
			searchValidator.validateSuggestionCount(count.getAsInt());
		var engine = searchSerivce.searchEngine();
		var startTime = System.nanoTime();
		try {
			var results = engine.search(query);
			return results.limit(count.orElse(DEFAULT_COUNT)).map(SearchableEntity::qualifiedName).map(Object::toString)
					.toList();
		} finally {
			searchReporter.logSearchTime("api/suggestions", query, startTime);
		}
	}
}
