package net.maisikoleni.javadoc.server.html;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

@ApplicationScoped
@Path("/")
public final class MainPage {

	@Inject
	JavadocSearchEngines javadocSearchEngines;

	@GET
	@Path("")
	public Response redirectToDefaultLibrary() {
		URI defaultLibrarySearchUri = UriBuilder.fromResource(JavadocSearchPage.class)
				.resolveTemplate(Constants.LIBRARY_ID_PARAMETER, javadocSearchEngines.getDefaultLibraryId()).build();
		return Response.temporaryRedirect(defaultLibrarySearchUri).build();
	}
}
