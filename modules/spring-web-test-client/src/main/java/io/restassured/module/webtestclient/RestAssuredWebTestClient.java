/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.restassured.module.webtestclient;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Method;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.internal.BuilderBasedWebTestClientFactory;
import io.restassured.module.webtestclient.internal.StandaloneWebTestClientFactory;
import io.restassured.module.webtestclient.internal.WebTestClientFactory;
import io.restassured.module.webtestclient.internal.WebTestClientRequestSpecificationImpl;
import io.restassured.module.webtestclient.internal.WrapperWebTestClientFactory;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSender;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriBuilder;

import static io.restassured.config.LogConfig.logConfig;

/**
 * The Spring Web Test Client module's equivalent of {@link io.restassured.RestAssured}. This is the starting point of the DSL.
 * <p>Note that some Javadoc is copied from Spring Web Test Client test documentation.</p>
 */
public class RestAssuredWebTestClient {

	private static WebTestClientFactory webTestClientFactory = null;

	public static RestAssuredWebTestClientConfig config;
	public static WebTestClientRequestSpecification requestSpecification;
	public static ResponseSpecification responseSpecification = null;
	public static String basePath = "/";

	public static void webTestClient(WebTestClient webTestClient) {
		RestAssuredWebTestClient.webTestClientFactory = new WrapperWebTestClientFactory(webTestClient);
	}

	public static WebTestClientRequestSpecification with() {
		return given();
	}

	public static WebTestClientRequestSpecification given() {
		return new WebTestClientRequestSpecificationImpl(webTestClientFactory, config, basePath, requestSpecification,
				responseSpecification);
	}

	public static void standaloneSetup(Object... controllerFunctionsRoutersOrBuilders) {
		webTestClientFactory = StandaloneWebTestClientFactory.of(controllerFunctionsRoutersOrBuilders);
	}

	public static void standaloneSetup(RouterFunction routerFunction, WebTestClientConfigurer... configurers) {
		webTestClientFactory = StandaloneWebTestClientFactory.of(routerFunction, configurers);
	}

	public static void standaloneSetup(WebTestClient.Builder builder) {
		webTestClientFactory = new BuilderBasedWebTestClientFactory(builder);
	}

	public static void webAppContextSetup(WebApplicationContext context, WebTestClientConfigurer... configurers) {
		webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurers);
	}

	public static void applicationContextSetup(ApplicationContext applicationContext,
	                                           WebTestClientConfigurer... configurers) {
		webTestClientFactory = StandaloneWebTestClientFactory.of(applicationContext, configurers);
	}

	public static void reset() {
		webTestClientFactory = null;
		config = null;
		basePath = "/";
		responseSpecification = null;
		requestSpecification = null;
	}

	public static WebTestClientResponse get(String path, Object... pathParams) {
		return given().get(path, pathParams);
	}

	public static WebTestClientResponse get(String path, Map<String, ?> pathParams) {
		return given().get(path, pathParams);
	}

	public static WebTestClientResponse get(Function<UriBuilder, URI> uriFunction) {
		return given().get(uriFunction);
	}

	public static WebTestClientResponse get(URI uri) {
		return given().get(uri);
	}

	public static WebTestClientResponse get(URL url) {
		return given().get(url);
	}

	public static WebTestClientResponse get() {
		return given().get();
	}

	public static WebTestClientResponse request(Method method) {
		return given().request(method);
	}

	public static WebTestClientResponse request(String method) {
		return given().request(method);
	}

	public static WebTestClientResponse request(Method method, String path, Object... pathParams) {
		return given().request(method, path, pathParams);
	}

	public static WebTestClientResponse request(String method, String path, Object... pathParams) {
		return given().request(method, path, pathParams);
	}

	public static WebTestClientResponse request(Method method, URI uri) {
		return given().request(method, uri);
	}

	public static WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction) {
		return given().request(method, uriFunction);
	}

	public static WebTestClientResponse request(Method method, URL url) {
		return given().request(method, url);
	}

	public static WebTestClientResponse request(String method, URI uri) {
		return given().request(method, uri);
	}

	public static WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction) {
		return given().request(method, uriFunction);
	}

	public static WebTestClientResponse request(String method, URL url) {
		return given().request(method, url);
	}

	public static WebTestClientResponse post(String path, Object... pathParams) {
		return given().post(path, pathParams);
	}

	public static WebTestClientResponse post(String path, Map<String, ?> pathParams) {
		return given().post(path, pathParams);
	}

	public static WebTestClientResponse post(Function<UriBuilder, URI> uriFunction) {
		return given().post(uriFunction);
	}

	public static WebTestClientResponse post(URI uri) {
		return given().post(uri);
	}

	public static WebTestClientResponse post(URL url) {
		return given().post(url);
	}

	public static WebTestClientResponse post() {
		return given().post();
	}

	public static WebTestClientResponse put(String path, Object... pathParams) {
		return given().put(path, pathParams);
	}

	public static WebTestClientResponse put(String path, Map<String, ?> pathParams) {
		return given().put(path, pathParams);
	}

	public static WebTestClientResponse put(Function<UriBuilder, URI> uriFunction) {
		return given().put(uriFunction);
	}

	public static WebTestClientResponse put(URI uri) {
		return given().put(uri);
	}

	public static WebTestClientResponse put(URL url) {
		return given().put(url);
	}

	public static WebTestClientResponse put() {
		return given().put();
	}

	public static WebTestClientResponse delete(String path, Object... pathParams) {
		return given().delete(path, pathParams);
	}

	public static WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
		return given().delete(path, pathParams);
	}

	public static WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction) {
		return given().delete(uriFunction);
	}

	public static WebTestClientResponse delete(URI uri) {
		return given().delete(uri);
	}

	public static WebTestClientResponse delete(URL url) {
		return given().delete(url);
	}

	public static WebTestClientResponse delete() {
		return given().delete();
	}

	public static WebTestClientResponse head(String path, Object... pathParams) {
		return given().head(path, pathParams);
	}

	public static WebTestClientResponse head(String path, Map<String, ?> pathParams) {
		return given().head(path, pathParams);
	}

	public static WebTestClientResponse head(Function<UriBuilder, URI> uriFunction) {
		return given().head(uriFunction);
	}

	public static WebTestClientResponse head(URI uri) {
		return given().head(uri);
	}

	public static WebTestClientResponse head(URL url) {
		return given().head(url);
	}

	public static WebTestClientResponse head() {
		return given().head();
	}

	public static WebTestClientResponse patch(String path, Object... pathParams) {
		return given().patch(path, pathParams);
	}

	public static WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
		return given().patch(path, pathParams);
	}

	public static WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction) {
		return given().patch(uriFunction);
	}

	public static WebTestClientResponse patch(URI uri) {
		return given().patch(uri);
	}

	public static WebTestClientResponse patch(URL url) {
		return given().patch(url);
	}

	public static WebTestClientResponse patch() {
		return given().patch();
	}

	public static WebTestClientResponse options(String path, Object... pathParams) {
		return given().options(path, pathParams);
	}

	public static WebTestClientResponse options(String path, Map<String, ?> pathParams) {
		return given().options(path, pathParams);
	}

	public static WebTestClientResponse options(Function<UriBuilder, URI> uriFunction) {
		return given().options(uriFunction);
	}

	public static WebTestClientResponse options(URI uri) {
		return given().options(uri);
	}

	public static WebTestClientResponse options(URL url) {
		return given().options(url);
	}

	public static WebTestClientResponse options() {
		return given().options();
	}

	public static void enableLoggingOfRequestAndResponseIfValidationFails() {
		enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
	}

	static void enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
		config = config == null ? new RestAssuredWebTestClientConfig() : config;
		config = config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
		// Update request specification if already defined otherwise it'll override the configs.
		// Note that request spec also influence response spec when it comes to logging if validation fails
		// due to the way filters work
		if (requestSpecification instanceof WebTestClientRequestSpecificationImpl) {
			RestAssuredWebTestClientConfig restAssuredConfig = ((WebTestClientRequestSpecificationImpl) requestSpecification)
					.getRestAssuredWebTestClientConfig();
			if (restAssuredConfig == null) {
				restAssuredConfig = config;
			} else {
				LogConfig logConfigForRequestSpec = restAssuredConfig.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail);
				restAssuredConfig = restAssuredConfig.logConfig(logConfigForRequestSpec);
			}
			requestSpecification.config(restAssuredConfig);
		}
	}

	/**
	 * @return The assigned config or a new config is no config is assigned
	 */
	public static RestAssuredWebTestClientConfig config() {
		return config == null ? new RestAssuredWebTestClientConfig() : config;
	}

	public WebTestClientRequestSender when() {
		return given().when();
	}
}
