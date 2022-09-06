package net.maisikoleni.javadoc.server;

import java.util.List;
import java.util.OptionalInt;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import net.maisikoleni.javadoc.service.SearchService;
import net.maisikoleni.javadoc.service.SearchServiceProvider.FixLibraryId;

@Deprecated
@RequestScoped
@Path("/api/search")
public class LegacyApiV1 implements JavadocSearch {

	@Inject
	@FixLibraryId
	SearchService searchService;

	@Inject
	SearchValidator searchValidator;

	@Inject
	SearchReporter searchReporter;

	@Override
	public Response searchAndRedirect(String query) {
		return newSearchResource().searchAndRedirect(query);
	}

	@Override
	public List<String> suggestions(String query, OptionalInt count) {
		return newSearchResource().suggestions(query, count);
	}

	private SearchResource newSearchResource() {
		return new SearchResource(searchService, searchValidator, searchReporter);
	}
}
