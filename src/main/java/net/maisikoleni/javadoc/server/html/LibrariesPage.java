package net.maisikoleni.javadoc.server.html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

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
