/*
 * Copyright 2019 the original author or authors.
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

import io.restassured.RestAssured;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.http.Method;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.filter.FilterContextImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.internal.support.PathSupport;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.module.spring.commons.HeaderHelper;
import io.restassured.module.spring.commons.ParamApplier;
import io.restassured.module.spring.commons.config.ConfigConverter;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.codec.Charsets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static io.restassured.module.spring.commons.BodyHelper.toByteArray;
import static io.restassured.module.spring.commons.HeaderHelper.mapToArray;
import static io.restassured.module.spring.commons.RequestLogger.logParamsAndHeaders;
import static io.restassured.module.spring.commons.RequestLogger.logRequestBody;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.parseMediaType;

public class WebTestClientRequestSenderImpl implements WebTestClientRequestSender {

	private static final String CONTENT_TYPE = "Content-Type";

	private final WebTestClient webTestClient;
	private final Map<String, Object> params;
	private final Map<String, Object> queryParams;
	private final Map<String, Object> formParams;
	private final Map<String, Object> attributes;
	private final RestAssuredWebTestClientConfig config;
	private final Object requestBody;
	private final Cookies cookies;
	private final List<MultiPartInternal> multiParts;
	private final String basePath;
	private final ResponseSpecification responseSpecification;
	private final LogRepository logRepository;
	private Headers headers;
	private final RequestLoggingFilter requestLoggingFilter;
	private Consumer<EntityExchangeResult<byte[]>> consumer;

	@Override
	public WebTestClientResponse get(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(GET, uriFunction);
	}

	@Override
	public WebTestClientResponse post(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(POST, uriFunction);
	}

	@Override
	public WebTestClientResponse put(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(PUT, uriFunction);
	}

	@Override
	public WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(DELETE, uriFunction);
	}

	@Override
	public WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(PATCH, uriFunction);
	}

	@Override
	public WebTestClientResponse head(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(HEAD, uriFunction);
	}

	@Override
	public WebTestClientResponse options(Function<UriBuilder, URI> uriFunction) {
		return sendRequest(OPTIONS, uriFunction);
	}

	@Override
	public WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction) {
		return request(notNull(method, Method.class).name(), uriFunction);
	}

	@Override
	public WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction) {
		return sendRequest(toValidHttpMethod(method), uriFunction);
	}

	private HttpMethod toValidHttpMethod(String method) {
		String httpMethodAsString = notNull(trimToNull(method), "HTTP Method");
		HttpMethod httpMethod = HttpMethod.resolve(httpMethodAsString.toUpperCase());
		if (httpMethod == null) {
			throw new IllegalArgumentException("HTTP method '" + method + "' is not supported by WebTestClient");
		}
		return httpMethod;
	}

	@Override
	public WebTestClientResponse get(String path, Object... pathParams) {
		return sendRequest(GET, path, pathParams);
	}

	public WebTestClientRequestSenderImpl(WebTestClient webTestClient, Map<String, Object> params, Map<String,
			Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
										  RestAssuredWebTestClientConfig config, Object requestBody, Headers headers,
										  Cookies cookies, List<MultiPartInternal> multiParts,
										  RequestLoggingFilter requestLoggingFilter, String basePath,
										  ResponseSpecification responseSpecification,
										  LogRepository logRepository) {
		this.webTestClient = webTestClient;
		this.params = params;
		this.queryParams = queryParams;
		this.formParams = formParams;
		this.attributes = attributes;
		this.config = config;
		this.requestBody = requestBody;
		this.headers = headers;
		this.cookies = cookies;
		this.multiParts = multiParts;
		this.basePath = basePath;
		this.responseSpecification = responseSpecification;
		this.logRepository = logRepository;
		this.requestLoggingFilter = requestLoggingFilter;
	}

	@Override
	public WebTestClientRequestSender consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer) {
		this.consumer = consumer;
		return this;
	}

	@Override
	public WebTestClientResponse get(String path, Map<String, ?> pathParams) {
		return get(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse post(String path, Object... pathParams) {
		return sendRequest(POST, path, pathParams);
	}

	@Override
	public WebTestClientResponse post(String path, Map<String, ?> pathParams) {
		return post(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse put(String path, Object... pathParams) {
		return sendRequest(PUT, path, pathParams);
	}

	@Override
	public WebTestClientResponse put(String path, Map<String, ?> pathParams) {
		return put(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse delete(String path, Object... pathParams) {
		return sendRequest(DELETE, path, pathParams);
	}

	@Override
	public WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
		return delete(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse head(String path, Object... pathParams) {
		return sendRequest(HEAD, path, pathParams);
	}

	@Override
	public WebTestClientResponse head(String path, Map<String, ?> pathParams) {
		return head(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse patch(String path, Object... pathParams) {
		return sendRequest(PATCH, path, pathParams);
	}

	@Override
	public WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
		return patch(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse options(String path, Object... pathParams) {
		return sendRequest(OPTIONS, path, pathParams);
	}

	@Override
	public WebTestClientResponse options(String path, Map<String, ?> pathParams) {
		return options(path, mapToArray(pathParams));
	}

	@Override
	public WebTestClientResponse get(URI uri) {
		return get(uri.toString());
	}

	@Override
	public WebTestClientResponse post(URI uri) {
		return post(uri.toString());
	}

	@Override
	public WebTestClientResponse put(URI uri) {
		return put(uri.toString());
	}

	@Override
	public WebTestClientResponse delete(URI uri) {
		return delete(uri.toString());
	}

	@Override
	public WebTestClientResponse head(URI uri) {
		return head(uri.toString());
	}

	@Override
	public WebTestClientResponse patch(URI uri) {
		return patch(uri.toString());
	}

	@Override
	public WebTestClientResponse options(URI uri) {
		return options(uri.toString());
	}

	@Override
	public WebTestClientResponse get(URL url) {
		return get(url.toString());
	}

	@Override
	public WebTestClientResponse post(URL url) {
		return post(url.toString());
	}

	@Override
	public WebTestClientResponse put(URL url) {
		return put(url.toString());
	}

	@Override
	public WebTestClientResponse delete(URL url) {
		return delete(url.toString());
	}

	@Override
	public WebTestClientResponse head(URL url) {
		return head(url.toString());
	}

	@Override
	public WebTestClientResponse patch(URL url) {
		return patch(url.toString());
	}

	@Override
	public WebTestClientResponse options(URL url) {
		return options(url.toString());
	}

	@Override
	public WebTestClientResponse get() {
		return get("");
	}

	@Override
	public WebTestClientResponse post() {
		return post("");
	}

	@Override
	public WebTestClientResponse put() {
		return put("");
	}

	@Override
	public WebTestClientResponse delete() {
		return delete("");
	}

	@Override
	public WebTestClientResponse head() {
		return head("");
	}

	@Override
	public WebTestClientResponse patch() {
		return patch("");
	}

	@Override
	public WebTestClientResponse options() {
		return options("");
	}

	@Override
	public WebTestClientResponse request(Method method) {
		return request(method, "");
	}

	@Override
	public WebTestClientResponse request(String method) {
		return request(method, "");
	}

	@Override
	public WebTestClientResponse request(Method method, String path, Object... pathParams) {
		return request(notNull(method, Method.class).name(), path, pathParams);
	}

	@Override
	public WebTestClientResponse request(String method, String path, Object... pathParams) {
		return sendRequest(toValidHttpMethod(method), path, pathParams);
	}

	@Override
	public WebTestClientResponse request(Method method, URI uri) {
		return request(method, notNull(uri, URI.class).toString());
	}

	@Override
	public WebTestClientResponse request(Method method, URL url) {
		return request(method, notNull(url, URL.class).toString());
	}

	@Override
	public WebTestClientResponse request(String method, URI uri) {
		return request(method, notNull(uri, URI.class).toString());
	}

	@Override
	public WebTestClientResponse request(String method, URL url) {
		return request(method, notNull(url, URL.class).toString());
	}

	private WebTestClientResponse sendRequest(HttpMethod method, String path, Object[] pathParams) {
		String requestContentType = HeaderHelper.findContentType(headers, (List<Object>) (List<?>) multiParts, config);
		WebTestClient.RequestBodySpec requestBodySpec = buildFromPath(method, requestContentType, path, pathParams);
		addRequestElements(method, requestContentType, requestBodySpec);
		logRequestIfApplicable(method, getBaseUri(path), path, pathParams);
		return performRequest(requestBodySpec);
	}

	private WebTestClient.RequestBodySpec buildFromPath(HttpMethod method, String requestContentType, String path,
														Object[] pathParams) {
		notNull(path, "Path");
		String baseUri = getBaseUri(path);
		String uri = buildUri(method, requestContentType, baseUri);
		return webTestClient.method(method).uri(uri, resolvePathParams(pathParams));
	}

	private void addRequestElements(HttpMethod method, String requestContentType, WebTestClient.RequestBodySpec requestBodySpec) {
		verifyNoBodyAndMultipartTogether();
		if (isNotBlank(requestContentType)) {
			requestBodySpec.contentType(parseMediaType(requestContentType));
		}
		applyRequestBody(requestBodySpec);
		applyMultipartBody(method, requestBodySpec);
		applyAttributes(requestBodySpec);
		headers.forEach(header -> requestBodySpec.header(header.getName(), header.getValue()));
		cookies.asList().forEach(cookie -> requestBodySpec.cookie(cookie.getName(), cookie.getValue()));
	}

	private void logRequestIfApplicable(HttpMethod method, String uri, String originalPath, Object[] unnamedPathParams) {
		if (requestLoggingFilter == null) {
			return;
		}
		final RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("http://localhost",
				RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.emptyList(),
				null, true, ConfigConverter.convertToRestAssuredConfig(config), logRepository, null);
		logParamsAndHeaders(reqSpec, method.toString(), uri, unnamedPathParams, params, queryParams, formParams, headers, cookies);
		logRequestBody(reqSpec, requestBody, headers, (List<Object>) (List<?>) multiParts, config);
		ofNullable(multiParts).map(List::stream).orElseGet(Stream::empty)
				.forEach(multiPart -> addMultipartToReqSpec(reqSpec, multiPart));
		String originalUriPath = PathSupport.getPath(originalPath);
		String uriPath = PathSupport.getPath(uri);
		requestLoggingFilter.filter(reqSpec, null, new FilterContextImpl(uri, originalUriPath,
				uriPath, uri, uri, new Object[0], method.toString(), null,
				Collections.<Filter>emptyList().iterator(), new HashMap<>()));
	}

	private String getBaseUri(String path) {
		String baseUri;
		if (isNotBlank(basePath)) {
			baseUri = mergeAndRemoveDoubleSlash(basePath, path);
		} else {
			baseUri = path;
		}
		return baseUri;
	}

	private WebTestClientResponse performRequest(WebTestClient.RequestBodySpec requestBuilder) {
		FluxExchangeResult<byte[]> result;
		WebTestClientRestAssuredResponseImpl restAssuredResponse;
		try {
			final long start = System.currentTimeMillis();
			WebTestClient.ResponseSpec responseSpec = requestBuilder.exchange();
			final long responseTime = System.currentTimeMillis() - start;
			result = responseSpec.returnResult(byte[].class);
			restAssuredResponse = new ExchangeResultConverter().toRestAssuredResponse(result, responseSpec, responseTime,
					logRepository, config, consumer, getRpr());
			if (responseSpecification != null) {
				responseSpecification.validate(ResponseConverter.toStandardResponse(restAssuredResponse));
			}
		} catch (Exception e) {
			return SafeExceptionRethrower.safeRethrow(e);
		}
		return restAssuredResponse;
	}

	private String buildUri(HttpMethod method, String requestContentType, String baseUri) {
		final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUri);
		applyQueryParams(uriComponentsBuilder);
		applyParams(method, uriComponentsBuilder, requestContentType);
		applyFormParams(method, uriComponentsBuilder, requestContentType);
		return uriComponentsBuilder.build().toUriString();
	}

	private Object[] resolvePathParams(Object[] pathParams) {
		Arrays.stream(pathParams).filter(param -> !(param instanceof String))
				.findAny().ifPresent(param -> {
			throw new IllegalArgumentException("Only Strings allowed in path parameters.");
		});
		return Arrays.stream(pathParams)
				.map(param -> UriUtils.encode((String) param, Charsets.UTF_8)).toArray();
	}

	private void verifyNoBodyAndMultipartTogether() {
		if (requestBody != null && !multiParts.isEmpty()) {
			throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request." +
					" Perhaps you want to change the body to a multi part?");
		}
	}

	private void applyRequestBody(WebTestClient.RequestBodySpec requestBodySpec) {
		if (requestBody != null) {
			if (requestBody instanceof byte[]) {
				requestBodySpec.syncBody(requestBody);
			} else if (requestBody instanceof File) {
				byte[] bytes = toByteArray((File) requestBody);
				requestBodySpec.syncBody(bytes);
			} else {
				requestBodySpec.syncBody(requestBody.toString());
			}
		}
	}

	private void applyMultipartBody(HttpMethod method, WebTestClient.RequestBodySpec requestBodySpec) {
		if (!multiParts.isEmpty()) {
			if (method != POST && method != PUT) {
				throw new IllegalArgumentException("Currently multi-part file data uploading only works for POST and PUT method.");
			}
			requestBodySpec.syncBody(getMultipartBody());
		}
	}

	private void applyAttributes(WebTestClient.RequestBodySpec requestBodySpec) {
		if (!attributes.isEmpty()) {
			new ParamApplier(attributes) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					requestBodySpec.attribute(paramName, paramValues[0]);
				}
			}.applyParams();
		}
	}

	private void addMultipartToReqSpec(RequestSpecification requestSpecification, MultiPartInternal multiPart) {
		requestSpecification.multiPart(new MultiPartSpecBuilder(multiPart.getContent())
				.controlName(multiPart.getControlName()).
						fileName(multiPart.getFileName()).
						mimeType(multiPart.getMimeType()).
						build());
	}

	private void applyQueryParams(UriComponentsBuilder uriComponentsBuilder) {
		if (!queryParams.isEmpty()) {
			new ParamApplier(queryParams) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					uriComponentsBuilder.queryParam(paramName, paramValues);
				}
			}.applyParams();
		}
	}

	private void applyParams(HttpMethod method, UriComponentsBuilder uriComponentsBuilder, String requestContentType) {
		if (!params.isEmpty()) {
			new ParamApplier(params) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					uriComponentsBuilder.queryParam(paramName, paramValues);
				}
			}.applyParams();

			if (isBlank(requestContentType) && method == POST && !isMultipartRequest()) {
				setContentTypeToApplicationFormUrlEncoded();
			}
		}
	}

	private void applyFormParams(HttpMethod method, UriComponentsBuilder uriComponentsBuilder, String requestContentType) {
		if (!formParams.isEmpty()) {
			if (method == GET) {
				throw new IllegalArgumentException("Cannot use form parameters in a GET request");
			}
			new ParamApplier(formParams) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					uriComponentsBuilder.queryParam(paramName, paramValues);
				}
			}.applyParams();
			if (isBlank(requestContentType) && !isMultipartRequest()) {
				setContentTypeToApplicationFormUrlEncoded();
			}
		}
	}

	private MultiValueMap<String, HttpEntity<?>> getMultipartBody() {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multiParts.stream().forEach(multipart -> multipartBodyBuilder.part(multipart.getFileName(), multipart.getContentBody()));
		return multipartBodyBuilder.build();
	}

	private boolean isMultipartRequest() {
		return !multiParts.isEmpty();
	}

	private void setContentTypeToApplicationFormUrlEncoded() {
		String requestContentType = parseMediaType(HeaderHelper.buildApplicationFormEncodedContentType(config,
				APPLICATION_FORM_URLENCODED_VALUE)).toString();
		List<Header> newHeaders = new ArrayList<>(headers.asList());
		newHeaders.add(new Header(CONTENT_TYPE, requestContentType));
		headers = new Headers(newHeaders);
	}

	private WebTestClientResponse sendRequest(HttpMethod method, Function<UriBuilder, URI> uriFunction) {
		String requestContentType = HeaderHelper.findContentType(headers, (List<Object>) (List<?>) multiParts, config);
		WebTestClient.RequestBodySpec requestBodySpec = buildFromUriFunction(method, uriFunction);
		addRequestElements(method, requestContentType, requestBodySpec);
		logRequestIfApplicable(method, uriFunction);
		return performRequest(requestBodySpec);
	}

	private WebTestClient.RequestBodySpec buildFromUriFunction(HttpMethod method, Function<UriBuilder, URI> uriFunction) {
		return webTestClient.method(method).uri(uriFunction);
	}

	private void logRequestIfApplicable(HttpMethod method, Function<UriBuilder, URI> uriFunction) {
		if (requestLoggingFilter == null) {
			return;
		}
		final RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("http://localhost",
				RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.emptyList(),
				null, true, ConfigConverter.convertToRestAssuredConfig(config), logRepository, null);
		logParamsAndHeaders(reqSpec, method.toString(), "Request from uri function" + uriFunction.toString(),
				null, params, queryParams, formParams,
				headers, cookies);
		logRequestBody(reqSpec, requestBody, headers, (List<Object>) (List<?>) multiParts, config);
		ofNullable(multiParts).map(List::stream).orElseGet(Stream::empty)
				.forEach(multiPart -> addMultipartToReqSpec(reqSpec, multiPart));
		requestLoggingFilter.filter(reqSpec, null,
				new FilterContextImpl("Request from uri function" + uriFunction.toString(),
						null, null, null, null, new Object[0],
						method.toString(), null, Collections.<Filter>emptyList().iterator(), new HashMap<>()));
	}

	private ResponseParserRegistrar getRpr() {
		if (responseSpecification instanceof ResponseSpecificationImpl) {
			return ((ResponseSpecificationImpl) responseSpecification).getRpr();
		}
		return new ResponseParserRegistrar();
	}
}
