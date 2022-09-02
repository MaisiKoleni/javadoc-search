package net.maisikoleni.javadoc.server.html;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

@Path("libraries")
public class LibrariesPage {

	@Inject
	JavadocSearchEngines javadocSearchEngines;

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public String showLibraries() {
		return Templates.libraryOverview(javadocSearchEngines.allJavadocs()).render();
	}

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance libraryOverview(Collection<Javadoc> libraries);
	}
}
