package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.setup.CookieProcessor;
import org.apache.commons.codec.Charsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriUtils;

import static org.apache.commons.codec.Charsets.UTF_8;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * @author Olga Maciaszek-Sharma
 */
public class SetCookiesTest {

	@BeforeClass
	public static void configureWebTestClientInstance() {
		RouterFunction<ServerResponse> setAttributesRoute = RouterFunctions
				.route(path("/setCookies").and(method(HttpMethod.GET)),
						new CookieProcessor()::processCookies);
		RestAssuredWebTestClient.standaloneSetup(setAttributesRoute);
	}

	@AfterClass
	public static void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void
	can_receive_cookies() {
		RestAssuredWebTestClient.given()
				.queryParam("name", "John Doe")
				.queryParam("project", "rest assured")
				.when()
				.get("/setCookies")
				.then()
				.statusCode(200)
				.cookie("name", UriUtils.encode("John Doe", UTF_8))
				.cookie("project", UriUtils.encode("rest assured", UTF_8));
	}

}
