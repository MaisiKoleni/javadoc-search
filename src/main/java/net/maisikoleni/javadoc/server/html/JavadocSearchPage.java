package net.maisikoleni.javadoc.server.html;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import org.jboss.resteasy.annotations.cache.Cache;

import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.config.Configuration;
import net.maisikoleni.javadoc.config.Configuration.ServerConfig.HtmlConfig;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.server.SearchReporter;
import net.maisikoleni.javadoc.server.SearchValidator;
import net.maisikoleni.javadoc.server.html.util.HtmxHeaders;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.SearchService;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Named()
@Cache(maxAge = Constants.HTTP_CACHE_MAX_AGE)
@Path("/libraries/{" + Constants.LIBRARY_ID_PARAMETER + "}/")
@RequestScoped
public final class JavadocSearchPage {

	static final String QUERY_NAME = "query";

	private final HtmlConfig config;
	private final SearchService searchService;
	private final SearchValidator searchValidator;
	private final SearchReporter searchReporter;

	@PathParam(Constants.LIBRARY_ID_PARAMETER)
	String libraryId;

	@Inject
	public JavadocSearchPage(Configuration config, SearchService searchService, SearchValidator searchValidator,
			SearchReporter searchReporter) {
		this.config = config.server().html();
		this.searchService = searchService;
		this.searchValidator = searchValidator;
		this.searchReporter = searchReporter;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String generateMainPage(@QueryParam(QUERY_NAME) Optional<String> query) {
		return Templates.searchPage(searchService.javadoc().name(), query.orElse(null)).render();
	}

	@POST
	@Path("search-redirect")
	public Response searchAndRedirect(@FormParam(QUERY_NAME) String query) {
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
	public Response getSearchSuggestionTable(@QueryParam(QUERY_NAME) String query) {
		var html = getSuggestionTable(query).render();
		var newUri = UriBuilder.fromResource(JavadocSearchPage.class).replaceQueryParam(QUERY_NAME, query)
				.build(libraryId);
		return Response.ok(html).header(HtmxHeaders.Response.REPLACE_URL, newUri).build();
	}

	public TemplateInstance getSuggestionTable(String query) {
		searchValidator.validateQuery(query);
		var startTime = System.nanoTime();
		var results = searchService.searchEngine().search(query).limit(config.suggestionCount()).toList();
		searchReporter.logSearchTime("html/suggestions", query, startTime);
		return Templates.searchSuggestions(searchService.javadoc().baseUrl(), results);
	}

	@GET
	@Path("opensearch.xml")
	@Produces(MediaType.TEXT_XML)
	public String opensearchXml() {
		var javadoc = searchService.javadoc();
		if (javadoc == null)
			throw new NotFoundException("No library with id '" + libraryId + "' found.");
		return Templates.opensearch(javadoc).render();
	}

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance searchPage(String javadocName, String query);

		public static native TemplateInstance searchSuggestions(URI baseUrl, Collection<SearchableEntity> suggestions);

		public static native TemplateInstance opensearch(Javadoc javadoc);
	}
}
