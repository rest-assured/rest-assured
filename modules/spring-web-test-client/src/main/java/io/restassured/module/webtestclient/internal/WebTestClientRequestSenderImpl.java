package io.restassured.module.webtestclient.internal;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.restassured.http.Cookies;
import io.restassured.http.Headers;
import io.restassured.http.Method;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestAsyncConfigurer;
import io.restassured.module.webtestclient.specification.WebTestClientRequestAsyncSender;
import io.restassured.specification.ResponseSpecification;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.util.UriBuilder;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestSenderImpl implements WebTestClientRequestAsyncSender {

	/*
	 List<ResultHandler> resultHandlers,
	 List<RequestPostProcessor> requestPostProcessors,
	 */

	private final WebTestClient webTestClient;
	private final Map<String, Object> params;
	private final Map<String, Object> formParams;
	private final Map<String, Object> attributes;
	private final RestAssuredWebTestClientConfig config;
	private final Object requestBody;
	private final Headers requestHeaders;
	private final Cookies cookies;
	private final Map<String, Object> sessionAttributes;
	private final List<MultiPartInternal> multiParts;
	private final ExchangeFilterFunction requestLoggingFunction;
	private final String basePath;
	private final ResponseSpecification responseSpecification;
	private final ExchangeFilterFunction authentication;
	private final LogRepository logRepository;

	public WebTestClientRequestSenderImpl(WebTestClient webTestClient, Map<String, Object> params, Map<String,
			Object> formParams, Map<String, Object> attributes, RestAssuredWebTestClientConfig config,
	                                      Object requestBody, Headers requestHeaders, Cookies cookies,
	                                      Map<String, Object> sessionAttributes, List<MultiPartInternal> multiParts,
	                                      ExchangeFilterFunction requestLoggingFunction, String basePath,
	                                      ResponseSpecification responseSpecification,
	                                      ExchangeFilterFunction authentication, LogRepository logRepository) {
		this.webTestClient = webTestClient;
		this.params = params;
		this.formParams = formParams;
		this.attributes = attributes;
		this.config = config;
		this.requestBody = requestBody;
		this.requestHeaders = requestHeaders;
		this.cookies = cookies;
		this.sessionAttributes = sessionAttributes;
		this.multiParts = multiParts;
		this.requestLoggingFunction = requestLoggingFunction;
		this.basePath = basePath;
		this.responseSpecification = responseSpecification;
		this.authentication = authentication;
		this.logRepository = logRepository;
	}

	@Override
	public WebTestClientRequestAsyncConfigurer async() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}
}
