package io.restassured.module.webtestclient;

import java.util.ArrayList;
import java.util.List;

import io.restassured.internal.WrapperWebTestClientFactory;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.internal.WebTestClientFactory;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * @author Olga Maciaszek-Sharma
 * The Spring Web Test Client module's equivalent of {@link io.restassured.RestAssured}. This is the starting point of the DSL.
 * <p>Note that some Javadoc is copied from Spring Web Test Client test documentation.</p>
 */
public class RestAssuredWebTestClient {

	private static WebTestClientFactory webTestClientFactory = null;
	private static List<ResultHandler> resultHandlers = new ArrayList<ResultHandler>();
	private static List<RequestPostProcessor> requestPostProcessors = new ArrayList<RequestPostProcessor>();

	public static RestAssuredWebTestClientConfig config;
	public static WebTestClientRequestSpecification requestSpecification;
	public static ResponseSpecification responseSpecification = null;
	public static String basePath = "/";

	public static void webTestClient(WebTestClient webTestClient) {
		RestAssuredWebTestClient.webTestClientFactory = new WrapperWebTestClientFactory(webTestClient);
	}


}
