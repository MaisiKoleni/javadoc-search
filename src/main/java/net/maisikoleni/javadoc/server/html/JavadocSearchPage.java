package net.maisikoleni.javadoc.server.html;

import static net.maisikoleni.javadoc.Configuration.SUGGESTION_COUNT_DEFAULT;
import static net.maisikoleni.javadoc.Configuration.SUGGESTION_COUNT_KEY;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.server.SearchReporter;
import net.maisikoleni.javadoc.server.SearchValidator;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@Named()
@Path("/")
public final class JavadocSearchPage {

	private static final String QUERY_NAME = "query";

	@Inject
	@ConfigProperty(name = SUGGESTION_COUNT_KEY, defaultValue = SUGGESTION_COUNT_DEFAULT)
	int suggestionCount;

	@Inject
	@Jdk(Version.RELEASE_18)
	SearchService searchService;

	@Inject
	SearchValidator searchValidator;

	@Inject
	SearchReporter searchReporter;

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public String generateMainPage(@QueryParam(QUERY_NAME) Optional<String> query) {
		return Templates.searchPage(searchService.name(), query.orElse(null)).render();
	}

	@POST
	@Path("search-redirect")
	public Response searchAndRedirect(@NotNull @FormParam(QUERY_NAME) String query) {
		searchValidator.validateQuery(query);
		var startTime = System.nanoTime();
		try {
			return Response.seeOther(searchService.getBestUrl(query)).build();
		} finally {
			searchReporter.logSearchTime("html/redirect", query, startTime);
		}
	}

	@GET
	@Path("search-suggestions")
	@Produces(MediaType.TEXT_HTML)
	public Response getSearchSuggestionTable(@NotNull @QueryParam(QUERY_NAME) String query) {
		var html = getSuggestionTable(query).render();
		var newUri = UriBuilder.fromResource(JavadocSearchPage.class).replaceQueryParam(QUERY_NAME, query).build();
		return Response.ok(html).header(HtmxHeaders.Response.REPLACE_URL, newUri).build();
	}

	public TemplateInstance getSuggestionTable(String query) {
		searchValidator.validateQuery(query);
		var startTime = System.nanoTime();
		var results = searchService.searchEngine().search(query).limit(suggestionCount).toList();
		searchReporter.logSearchTime("html/suggestions", query, startTime);
		return Templates.searchSuggestions(searchService.javadoc().baseUrl(), results);
	}

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance searchPage(String javadocName, String query);

		public static native TemplateInstance searchSuggestions(URI baseUrl, Collection<SearchableEntity> suggestions);
	}
}
