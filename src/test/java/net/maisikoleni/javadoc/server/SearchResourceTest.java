package net.maisikoleni.javadoc.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyString;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SearchResourceTest {

	private static final String ROUTE_PREFIX = "/api/v2/libraries/jdk-latest/search/";

	@Test
	void testSearchAndRedirectFound() {
		given().redirects().follow(false).when().get(ROUTE_PREFIX + "redirect?query=str Col~or").then() //
				.body(is("")) //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION,
						"https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/stream/Collector.html");
	}

	@Test
	void testSearchAndRedirectNotFound() {
		given().redirects().follow(false).when().get(ROUTE_PREFIX + "redirect?query=xxx").then() //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION, "https://docs.oracle.com/en/java/javase/18/docs/api/");
	}

	@Test
	void testSearchAndRedirectEmpty() {
		given().redirects().follow(false).when().get(ROUTE_PREFIX + "redirect?query=").then() //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION, "https://docs.oracle.com/en/java/javase/18/docs/api/");
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

	@Test
	void testSuggestionsEmpty() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						["",[\
						]]"""));
	}

	@Test
	void testSuggestionsWithCount() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=str Col~or&count=1").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						["str Col~or",[\
						"java.base/java.util.stream.Collector"\
						]]"""));
	}

	@Test
	void testRedirectNoQuery() {
		given().when().get(ROUTE_PREFIX + "redirect").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testRedirectLongQuery() {
		given().when().get(ROUTE_PREFIX + "redirect?query=" + "a".repeat(1001)).then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsNoQuery() {
		given().when().get(ROUTE_PREFIX + "suggestions").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsLongQuery() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=" + "a".repeat(1001)).then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsLargeCount() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=Se&count=100").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsNegativeCount() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=Se&count=0").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsQueryWithUndefinedChar() {
		given().when().get(ROUTE_PREFIX + "suggestions?query=\u0378").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}
}
