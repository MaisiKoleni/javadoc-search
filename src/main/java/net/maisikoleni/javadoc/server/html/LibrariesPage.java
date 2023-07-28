package net.maisikoleni.javadoc.server.html;

import java.util.*;

import org.jboss.resteasy.annotations.cache.Cache;

import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Cache(maxAge = Constants.HTTP_CACHE_MAX_AGE)
@Path("/libraries/")
public class LibrariesPage {

	@Inject
	JavadocSearchEngines javadocSearchEngines;

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public String showLibraries() {
		var allJavadocs = new ArrayList<>(javadocSearchEngines.allJavadocs());
		Collections.sort(allJavadocs, Comparator.comparing(Javadoc::id));
		return Templates.libraryOverview(allJavadocs).render();
	}

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance libraryOverview(Collection<Javadoc> libraries);
	}
}
