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

package io.restassured.module.webtestclient.internal;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.http.Method;
import io.restassured.internal.MapCreator;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.internal.support.ParameterUpdater;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.module.spring.commons.HeaderHelper;
import io.restassured.module.spring.commons.SpringClientSerializer;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestAsyncSender;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriBuilder;

import static io.restassured.internal.assertion.AssertParameter.notNull;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestSpecificationImpl implements WebTestClientRequestSpecification {

	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ACCEPT = "Accept";
	private final Map<String, Object> params = new LinkedHashMap<String, Object>();
	private final Map<String, Object> queryParams = new LinkedHashMap<String, Object>();
	private final Map<String, Object> formParams = new LinkedHashMap<String, Object>();
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	private final ResponseSpecification responseSpecification;
	private final Map<String, Object> sessionAttributes = new LinkedHashMap<String, Object>();
	private final String basePath;
	private Object requestBody = null;
	private LogRepository logRepository;
	private WebTestClientFactory webTestClientFactory;
	private RestAssuredWebTestClientConfig config;
	private final SpringClientSerializer serializer = new SpringClientSerializer(config);
	private Headers requestHeaders = new Headers();
	private final ParameterUpdater parameterUpdater = new ParameterUpdater(new ParameterUpdater.Serializer() {
		public String serializeIfNeeded(Object value) {
			return serializer.serializeIfNeeded(value, getRequestContentType());
		}
	});
	private ExchangeFilterFunction requestLoggingFunction;
	private List<MultiPartInternal> multiParts = new ArrayList<MultiPartInternal>();
	private Cookies cookies = new Cookies();
	private ExchangeFilterFunction authentication;


	public WebTestClientRequestSpecificationImpl(ResponseSpecification responseSpecification, String basePath) {
		this.responseSpecification = responseSpecification;
		this.basePath = basePath;
		assignConfig(config);
	}

	// TODO: extract duplicate?
	private void assignConfig(RestAssuredWebTestClientConfig config) {
		if (config == null) {
			this.config = new RestAssuredWebTestClientConfig();
		} else {
			this.config = config;
		}
	}

	@Override
	public WebTestClientRequestSpecification contentType(ContentType contentType) {
		notNull(contentType, "contentType");
		return header(CONTENT_TYPE, contentType.toString());
	}

	@Override
	public WebTestClientRequestSpecification contentType(String contentType) {
		notNull(contentType, "contentType");
		return header(CONTENT_TYPE, contentType);
	}

	@Override
	public WebTestClientRequestSpecification accept(ContentType contentType) {
		notNull(contentType, "contentType");
		return header(ACCEPT, contentType.getAcceptHeader());
	}

	@Override
	public WebTestClientRequestSpecification accept(String mediaTypes) {
		notNull(mediaTypes, "mediaTypes");
		return header(ACCEPT, mediaTypes);
	}

	@Override
	public WebTestClientRequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs) {
		return headers(MapCreator.createMapFromParams(MapCreator.CollisionStrategy.MERGE,
				firstHeaderName, firstHeaderValue, headerNameValuePairs));
	}

	@Override
	public WebTestClientRequestSpecification headers(Map<String, ?> headers) {
		notNull(headers, "headers");
		this.requestHeaders = HeaderHelper.headers(requestHeaders, headers, config);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification headers(Headers headers) {
		notNull(headers, "Headers");
		requestHeaders = HeaderHelper.headers(requestHeaders, headers, config.getHeaderConfig());
		return this;
	}

	@Override
	public WebTestClientRequestSpecification header(String headerName, Object headerValue, Object... additionalHeaderValues) {
		notNull(headerName, "Header name");
		notNull(headerValue, "Header value");
		return headers(HeaderHelper.headers(requestHeaders, headerName, headerValue, config, additionalHeaderValues));
	}

	@Override
	public WebTestClientRequestSpecification header(Header header) {
		notNull(header, "Header");
		return headers(new Headers(Collections.singletonList(header)));
	}

	@Override
	public WebTestClientRequestLogSpecification log() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification params(Map<String, ?> parametersMap) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification param(String parameterName, Object... parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification param(String parameterName, Collection<?> parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification queryParams(Map<String, ?> parametersMap) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification queryParam(String parameterName, Object... parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification formParams(Map<String, ?> parametersMap) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification formParam(String parameterName, Object... parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification formParam(String parameterName, Collection<?> parameterValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification attribute(String attributeName, Object attributeValue) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification attributes(Map<String, ?> attributesMap) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(String body) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(byte[] body) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(File body) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(Object object) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(Object object, ObjectMapper mapper) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(Object object, ObjectMapperType mapperType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookies(Map<String, ?> cookies) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookies(Cookies cookies) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookie(String cookieName, Object value, Object... additionalValues) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookie(Cookie cookie) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(File file) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, File file) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, File file, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, Object object) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, Object object, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String filename, Object object, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, byte[] bytes) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, InputStream stream) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String contentBody) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String contentBody, String mimeType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification config(RestAssuredWebTestClientConfig config) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification spec(WebTestClientRequestSpecification requestSpecificationToMerge) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification sessionId(String sessionIdValue) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification sessionId(String sessionIdName, String sessionIdValue) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification sessionAttrs(Map<String, Object> sessionAttributes) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification sessionAttr(String name, Object value) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestAsyncSender when() {
		LogConfig logConfig = config.getLogConfig();
		if (requestLoggingFunction == null && logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
			// TODO: implement logging setup
			log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
		}
		WebTestClient webTestClient = webTestClientFactory.build(config.getWebTestClientConfig());
		return new WebTestClientRequestSenderImpl(webTestClient, params, formParams, attributes, config, requestBody,
				requestHeaders, cookies, sessionAttributes, multiParts, requestLoggingFunction, basePath, responseSpecification, authentication,
				logRepository);
	}

	@Override
	public WebTestClientRequestSpecification standaloneSetup(Object... controllerOrWebTestClientConfigurer) {
		WebTestClientFactory webTestClientFactory = StandaloneWebTestClientFactory.of(controllerOrWebTestClientConfigurer);
		return toRequestSpecification(webTestClientFactory);
	}

	@Override
	public WebTestClientRequestSpecification standaloneSetup(WebTestClient.Builder builder) {
		notNull(builder, WebTestClient.Builder.class);
		return toRequestSpecification(new BuilderBasedWebTestClientFactory(builder));
	}

	@Override
	public WebTestClientRequestSpecification standaloneSetup(RouterFunction routerFunction, WebTestClientConfigurer... configurers) {
		notNull(routerFunction, RouterFunction.class);
		WebTestClientFactory webTestClientFactory = StandaloneWebTestClientFactory.of(routerFunction, configurers);
		return toRequestSpecification(webTestClientFactory);
	}

	@Override
	public WebTestClientRequestSpecification WebTestClient(WebTestClient webTestClient) {
		notNull(webTestClient, WebTestClient.class);
		return toRequestSpecification(new WrapperWebTestClientFactory(webTestClient));
	}

	@Override
	public WebTestClientRequestSpecification webAppContextSetup(WebApplicationContext context, WebTestClientConfigurer... configurers) {
		return applicationContextSetup(context, configurers);
	}

	@Override
	public WebTestClientRequestSpecification applicationContextSetup(ApplicationContext context, WebTestClientConfigurer... configurers) {
		WebTestClientFactory webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurers);
		return toRequestSpecification(webTestClientFactory);
	}

	@Override
	public WebTestClientRequestSpecification and() {
		return this;
	}

	private WebTestClientRequestSpecification toRequestSpecification(WebTestClientFactory webTestClientFactory) {
		this.webTestClientFactory = webTestClientFactory;
		return this;
	}

	private String serializeIfNeeded(Object object) {
		return serializer.serializeIfNeeded(object, getRequestContentType());
	}

	// TODO: extract duplicate?
	private String getRequestContentType() {
		Header header = requestHeaders.get(CONTENT_TYPE);
		if (header != null) {
			return header.getValue();
		}
		return null;
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
