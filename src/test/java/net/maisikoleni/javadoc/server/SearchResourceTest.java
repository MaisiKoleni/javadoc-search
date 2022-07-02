package net.maisikoleni.javadoc.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SearchResourceTest {

	@Test
	void testSearchAndRedirectFound() {
		given().redirects().follow(false).when().get("api/search/redirect?query=str Col~or").then() //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION,
						"https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/stream/Collector.html");
	}

	@Test
	void testSearchAndRedirectNotFound() {
		given().redirects().follow(false).when().get("api/search/redirect?query=xxx").then() //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION, "https://docs.oracle.com/en/java/javase/18/docs/api/");
	}

	@Test
	void testSuggestions() {
		given().when().get("api/search/suggestions?query=str Col~or").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						["str Col~or",[\
						"java.base/java.util.stream.Collector",\
						"java.base/java.util.stream.Collectors"\
						]]"""));
	}
}
