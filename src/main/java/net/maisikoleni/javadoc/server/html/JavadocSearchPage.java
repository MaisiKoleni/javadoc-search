package net.maisikoleni.javadoc.server.html;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.config.Configuration;
import net.maisikoleni.javadoc.config.Configuration.ServerConfig.HtmlConfig;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.server.SearchReporter;
import net.maisikoleni.javadoc.server.SearchValidator;
import net.maisikoleni.javadoc.server.html.util.HtmxHeaders;
import net.maisikoleni.javadoc.service.SearchService;

@Named()
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
	@Path("")
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

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance searchPage(String javadocName, String query);

		public static native TemplateInstance searchSuggestions(URI baseUrl, Collection<SearchableEntity> suggestions);
	}
}
