package net.maisikoleni.javadoc.server.html;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import net.maisikoleni.javadoc.server.html.util.HtmxHeaders;

@QuarkusTest
class JavadocSearchPageSlowTest {

	private static final String ROUTE_PREFIX = "/libraries/jdk-latest/";

	@Test
	void testSearchAndRedirectFound() {
		given().redirects().follow(false).when() //
				.formParams(Map.of("query", "str Col~or")) //
				.post(ROUTE_PREFIX + "search-redirect").then() //
				.statusCode(Status.SEE_OTHER.getStatusCode()) //
				.header(HttpHeaders.LOCATION,
						"https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/stream/Collector.html");
	}

	@Test
	void testSuggestionsFound() {
		given().when().get(ROUTE_PREFIX + "search-suggestions?query=str Col~or").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.header(HtmxHeaders.Response.REPLACE_URL, ROUTE_PREFIX + "?query=str+Col~or") //
				.body(equalToCompressingWhiteSpace(
						"""
								<tr>
									<th scope="row" class="text-end">1</th>
									<td class="text-break">\
								<a href="https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/stream/Collector.html">\
								java.base/java.util.stream.Collector</a></td>
								</tr>
								<tr>
									<th scope="row" class="text-end">2</th>
									<td class="text-break">\
								<a href="https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/stream/Collectors.html">\
								java.base/java.util.stream.Collectors</a></td>
								</tr>
								"""));
	}

	@Test
	void startPageWithQuery() {
		String expectedInputValue = """
				value="jdk.a"
				""".strip();
		String expectedResultTable = """
				<tr>
					<th scope="row" class="text-end">1</th>
					<td class="text-break">\
				<a href="https://docs.oracle.com/en/java/javase/19/docs/api/jdk.accessibility/module-summary.html">\
				jdk.accessibility</a></td>
				</tr>
				<tr>
					<th scope="row" class="text-end">2</th>
					<td class="text-break">\
				<a href="https://docs.oracle.com/en/java/javase/19/docs/api/jdk.attach/module-summary.html">\
				jdk.attach</a></td>
				</tr>
				""".strip();
		given().when().get(ROUTE_PREFIX + "?query=jdk.a").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(containsString(expectedInputValue), //
						containsString(expectedResultTable));
	}

	@Test
	void testSuggestionsEmpty() {
		given().when().get(ROUTE_PREFIX + "search-suggestions?query=").then() //
				.statusCode(Status.OK.getStatusCode()) //
				.header(HtmxHeaders.Response.REPLACE_URL, ROUTE_PREFIX + "?query=") //
				.body(emptyString());
	}

	@Test
	void testRedirectNoQuery() {
		given().when().post(ROUTE_PREFIX + "search-redirect").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testRedirectLongQuery() {
		given().when().post(ROUTE_PREFIX + "search-redirect?query=" + "a".repeat(1001)).then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsNoQuery() {
		given().when().get(ROUTE_PREFIX + "search-suggestions").then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}

	@Test
	void testSuggestionsLongQuery() {
		given().when().get(ROUTE_PREFIX + "search-suggestions?query=" + "a".repeat(1001)).then() //
				.statusCode(Status.BAD_REQUEST.getStatusCode()) //
				.body(emptyString());
	}
}
