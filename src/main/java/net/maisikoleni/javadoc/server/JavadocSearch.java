package net.maisikoleni.javadoc.server;

import java.util.List;
import java.util.OptionalInt;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface JavadocSearch {

	int DEFAULT_COUNT = 10;

	/**
	 * Searches for the given query and redirects to the top search result.
	 *
	 * @param query The query to search for.
	 * @return The search response, either a redirect to the Javadoc or some
	 *         not-found page.
	 */
	@GET
	@Path("redirect")
	Response searchAndRedirect(@QueryParam("query") String query);

	/**
	 * Returns the top search suggestions.
	 *
	 * @param query The query to search for.
	 * @param count The number of search results per page (default 10).
	 * @return The list of suggestions for the given query
	 */
	List<String> suggestions(String query, OptionalInt count);

	/**
	 * Returns the top search suggestions to be used by a browser.
	 *
	 * @param query The query to search for.
	 * @param count The number of search results per page (default 10).
	 * @return A list containing: The query string and the list of suggestions for
	 *         the given query
	 * @implSpec This is not intended to be overridden by subclasses. For that
	 *           purpose {@link #suggestions(String, OptionalInt)} exists.
	 */
	@GET
	@Path("suggestions")
	@Produces(MediaType.APPLICATION_JSON)
	default List<Object> suggestionsForBrowser(@QueryParam("query") String query,
			@QueryParam("count") OptionalInt count) {
		return List.of(query, suggestions(query, count));
	}

}
