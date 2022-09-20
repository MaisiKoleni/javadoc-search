package net.maisikoleni.javadoc.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LegacyApiV1SearchResourceTest {

	private static final String ROUTE_PREFIX = "/api/search/";

	@Test
	void testSearchAndRedirectFound() {
		given().redirects().follow(false).when().get(ROUTE_PREFIX + "redirect?query=str Col~or").then() //
				.body(is("")) //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION,
						"https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/stream/Collector.html");
	}

	@Test
	void testSuggestionsFound() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=str Col~or").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						["str Col~or",[\
						"java.base/java.util.stream.Collector",\
						"java.base/java.util.stream.Collectors"\
						]]"""));
	}
}
