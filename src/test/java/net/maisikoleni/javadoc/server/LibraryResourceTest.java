package net.maisikoleni.javadoc.server;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class LibraryResourceTest {

	private static final String ROUTE_PREFIX = "/api/v2/libraries";

	@Test
	void testGetAll() {
		given().when().get(ROUTE_PREFIX).then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						[{\
						"name":"latest JDK (23)",\
						"id":"jdk-latest",\
						"description":"Latest JDK Release API",\
						"baseUrl":"https://docs.oracle.com/en/java/javase/23/docs/api/"\
						},{\
						"name":"JUnit 5",\
						"id":"junit5-latest",\
						"description":"Current JUnit 5 API",\
						"baseUrl":"https://junit.org/junit5/docs/current/api/"\
						},{\
						"name":"JDK 21",\
						"id":"jdk-21",\
						"description":"JDK 21",\
						"baseUrl":"https://docs.oracle.com/en/java/javase/21/docs/api/"\
						}]\
						"""));
	}
}
