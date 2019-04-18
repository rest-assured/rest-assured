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

import io.restassured.config.LogConfig;
import io.restassured.config.MultiPartConfig;
import io.restassured.config.ParamConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.*;
import io.restassured.internal.MapCreator;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.internal.support.ParameterUpdater;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.module.spring.commons.BodyHelper;
import io.restassured.module.spring.commons.CookieHelper;
import io.restassured.module.spring.commons.HeaderHelper;
import io.restassured.module.spring.commons.Serializer;
import io.restassured.module.spring.commons.config.AsyncConfig;
import io.restassured.module.spring.commons.config.ConfigConverter;
import io.restassured.module.spring.commons.config.ConfigMergeUtils;
import io.restassured.module.spring.commons.config.SpecificationConfig;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.config.WebTestClientParamConfig;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSender;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriBuilder;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.restassured.internal.MapCreator.CollisionStrategy.OVERWRITE;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.multipart.MultiPartInternal.OCTET_STREAM;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class WebTestClientRequestSpecificationImpl implements WebTestClientRequestSpecification {

	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ACCEPT = "Accept";
	private final Map<String, Object> params = new LinkedHashMap<String, Object>();
	private final Map<String, Object> queryParams = new LinkedHashMap<String, Object>();
	private final Map<String, Object> formParams = new LinkedHashMap<String, Object>();
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	private final ResponseSpecification responseSpecification;
	private RestAssuredWebTestClientConfig config;
	private Object requestBody = null;
	private LogRepository logRepository;
	private WebTestClientFactory webTestClientFactory;
	private final ParameterUpdater
			parameterUpdater = new ParameterUpdater(WebTestClientRequestSpecificationImpl.this::serializeIfNeeded);
	private Headers requestHeaders = new Headers();
	private String basePath;
	private RequestLoggingFilter requestLoggingFilter;
	private List<MultiPartInternal> multiParts = new ArrayList<MultiPartInternal>();
	private Cookies cookies = new Cookies();
	private AsyncConfig asyncConfig;


	public WebTestClientRequestSpecificationImpl(WebTestClientFactory webTestClientFactory,
	                                             RestAssuredWebTestClientConfig config,
	                                             String basePath,
	                                             WebTestClientRequestSpecification requestSpecification,
	                                             ResponseSpecification responseSpecification) {
		this.logRepository = new LogRepository();
		this.basePath = basePath;
		this.responseSpecification = responseSpecification;
		assignConfig(config);
		this.webTestClientFactory = webTestClientFactory == null ? new BuilderBasedWebTestClientFactory(null)
				: webTestClientFactory;
		if (requestSpecification != null) {
			spec(requestSpecification);
		}
	}

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
	public WebTestClientRequestSpecification accept(MediaType... mediaTypes) {
		notNull(mediaTypes, "mediaTypes");
		return header(ACCEPT, MediaType.toString(Arrays.asList(mediaTypes)));
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
		return new WebTestClientRequestLogSpecificationImpl(this);
	}

	@Override
	public WebTestClientRequestSpecification params(String firstParameterName, Object firstParameterValue,
	                                                Object... parameterNameValuePairs) {
		notNull(firstParameterName, "firstParameterName");
		notNull(firstParameterValue, "firstParameterValue");
		return params(MapCreator.createMapFromParams(OVERWRITE, firstParameterName,
				firstParameterValue, parameterNameValuePairs));
	}

	@Override
	public WebTestClientRequestSpecification params(Map<String, ?> parametersMap) {
		notNull(parametersMap, "parametersMap");
		parameterUpdater.updateParameters(convert(config.getParamConfig().requestParamsUpdateStrategy()),
				(Map<String, Object>) parametersMap, params);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification param(String parameterName, Object... parameterValues) {
		notNull(parameterName, "parameterName");
		parameterUpdater.updateZeroToManyParameters(convert(config.getParamConfig().requestParamsUpdateStrategy()),
				params, parameterName, parameterValues);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification param(String parameterName, Collection<?> parameterValues) {
		notNull(parameterName, "parameterName");
		notNull(parameterValues, "parameterValues");
		parameterUpdater.updateCollectionParameter(convert(config.getParamConfig().requestParamsUpdateStrategy()),
				params, parameterName, (Collection<Object>) parameterValues);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification queryParams(String firstParameterName, Object firstParameterValue,
	                                                     Object... parameterNameValuePairs) {
		notNull(firstParameterName, "firstParameterName");
		notNull(firstParameterValue, "firstParameterValue");
		return queryParams(MapCreator.createMapFromParams(OVERWRITE, firstParameterName,
				firstParameterValue, parameterNameValuePairs));
	}

	@Override
	public WebTestClientRequestSpecification queryParams(Map<String, ?> parametersMap) {
		notNull(parametersMap, "parametersMap");
		parameterUpdater.updateParameters(convert(config.getParamConfig().queryParamsUpdateStrategy()),
				(Map<String, Object>) parametersMap, queryParams);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification queryParam(String parameterName, Object... parameterValues) {
		notNull(parameterName, "parameterName");
		parameterUpdater.updateZeroToManyParameters(convert(config.getParamConfig().queryParamsUpdateStrategy()),
				queryParams, parameterName, parameterValues);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
		notNull(parameterName, "parameterName");
		notNull(parameterValues, "parameterValues");
		parameterUpdater.updateCollectionParameter(convert(config.getParamConfig().queryParamsUpdateStrategy()),
				queryParams, parameterName, (Collection<Object>) parameterValues);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification formParams(String firstParameterName, Object firstParameterValue,
	                                                    Object... parameterNameValuePairs) {
		notNull(firstParameterName, "firstParameterName");
		notNull(firstParameterValue, "firstParameterValue");
		return formParams(MapCreator.createMapFromParams(OVERWRITE, firstParameterName,
				firstParameterValue, parameterNameValuePairs));
	}

	@Override
	public WebTestClientRequestSpecification formParams(Map<String, ?> parametersMap) {
		notNull(parametersMap, "parametersMap");
		parameterUpdater.updateParameters(convert(config.getParamConfig().formParamsUpdateStrategy()),
				(Map<String, Object>) parametersMap, formParams);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification formParam(String parameterName, Object... parameterValues) {
		notNull(parameterName, "parameterName");
		parameterUpdater.updateZeroToManyParameters(convert(config.getParamConfig().formParamsUpdateStrategy()),
				formParams, parameterName, parameterValues);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification formParam(String parameterName, Collection<?> parameterValues) {
		notNull(parameterName, "parameterName");
		notNull(parameterValues, "parameterValues");
		parameterUpdater.updateCollectionParameter(convert(config.getParamConfig().formParamsUpdateStrategy()),
				formParams, parameterName, (Collection<Object>) parameterValues);
		return this;
	}

	/**
	 *
	 * @param attributeName  The attribute name
	 * @param attributeValue The attribute value
	 * @return WebTestClientRequestSpecification with updated attributes
	 * Note: This will set attributes on `ClientRequest` in `WebTestClient`; given the way `WebTesClient` works under
	 * the hood, these arguments remain on client side only and will not be propagated to the `ServerRequest`.
	 */
	@Override
	public WebTestClientRequestSpecification attribute(String attributeName, Object attributeValue) {
		notNull(attributeName, "attributeName");
		notNull(attributeValue, "attributeValue");
		ParamConfig paramConfig = config.getParamConfig();
		parameterUpdater.updateZeroToManyParameters(convert(toWebTestClientParamConfig(paramConfig)
						.attributeUpdateStrategy()),
				attributes, attributeName, attributeValue);
		return this;
	}

	/**
	 *
	 * @param attributesMap The Map containing the request attribute names and their values
	 * @return WebTestClientRequestSpecification with updated attributes
	 * Note: This will set attributes on `ClientRequest` in `WebTestClient`; given the way `WebTesClient` works under
	 * the hood, these arguments remain on client side only and will not be propagated to the `ServerRequest`.
	 */
	@Override
	public WebTestClientRequestSpecification attributes(Map<String, ?> attributesMap) {
		notNull(attributesMap, "attributesMap");
		parameterUpdater.updateParameters(convert(toWebTestClientParamConfig(config.getParamConfig())
				.attributeUpdateStrategy()), (Map<String, Object>) attributesMap, attributes);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(String body) {
		this.requestBody = body;
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(byte[] body) {
		this.requestBody = body;
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(File body) {
		this.requestBody = body;
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(Object object) {
		notNull(object, "object");
		this.requestBody = BodyHelper.toStringBody(object, config, requestHeaders);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(Object object, ObjectMapper mapper) {
		notNull(object, "object");
		notNull(mapper, "Object mapper");
		this.requestBody = BodyHelper.toSerializedBody(object, mapper, config, requestHeaders);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification body(Object object, ObjectMapperType mapperType) {
		notNull(object, "object");
		notNull(mapperType, "Object mapper type");
		String requestContentType = getRequestContentType();
		this.requestBody = ObjectMapping.serialize(object, requestContentType,
				Serializer.findEncoderCharsetOrReturnDefault(requestContentType, config), mapperType,
				config.getObjectMapperConfig(), config.getEncoderConfig());
		return this;
	}

	@Override
	public WebTestClientRequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
		return cookies(MapCreator.createMapFromParams(OVERWRITE, firstCookieName, firstCookieValue, cookieNameValuePairs));
	}

	@Override
	public WebTestClientRequestSpecification cookies(Map<String, ?> cookies) {
		notNull(cookies, "cookies");
		this.cookies = CookieHelper.cookies(this.cookies, cookies, requestHeaders, config);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification cookies(Cookies cookies) {
		notNull(cookies, "Cookies");
		this.cookies = CookieHelper.cookies(this.cookies, cookies);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification cookie(String cookieName, Object cookieValue, Object... additionalValues) {
		notNull(cookieName, "Cookie name");
		notNull(cookieValue, "Cookie value");
		return cookies(CookieHelper.cookie(cookieName, cookieValue, requestHeaders, config, additionalValues));
	}

	@Override
	public WebTestClientRequestSpecification cookie(Cookie cookie) {
		notNull(cookie, "Cookie");
		return cookies(new Cookies(Collections.singletonList(cookie)));
	}

	@Override
	public WebTestClientRequestSpecification multiPart(File file) {
		MultiPartConfig multiPartConfig = config.getMultiPartConfig();
		multiParts.add(new MultiPartInternal(file, multiPartConfig.defaultControlName(), file.getName(), OCTET_STREAM));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, File file) {
		multiParts.add(new MultiPartInternal(file, controlName, file.getName(), OCTET_STREAM));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, File file, String mimeType) {
		multiParts.add(new MultiPartInternal(file, controlName, file.getName(), mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, Object object) {
		MultiPartConfig multiPartConfig = config.getMultiPartConfig();
		multiParts.add(new MultiPartInternal(serializeIfNeeded(object), controlName, multiPartConfig.defaultFileName()));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, Object object, String mimeType) {
		MultiPartConfig multiPartConfig = config.getMultiPartConfig();
		multiParts.add(new MultiPartInternal(Serializer.serializeIfNeeded(object, mimeType, config), controlName,
				multiPartConfig.defaultFileName(), mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String filename, Object object, String mimeType) {
		multiParts.add(new MultiPartInternal(Serializer.serializeIfNeeded(object, mimeType, config),
				controlName, filename, mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, byte[] bytes) {
		multiParts.add(new MultiPartInternal(bytes, controlName, fileName, OCTET_STREAM));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
		multiParts.add(new MultiPartInternal(bytes, controlName, fileName, mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, InputStream stream) {
		multiParts.add(new MultiPartInternal(stream, controlName, fileName, OCTET_STREAM));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType) {
		multiParts.add(new MultiPartInternal(stream, controlName, fileName, mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String contentBody) {
		MultiPartConfig multiPartConfig = config.getMultiPartConfig();
		multiParts.add(new MultiPartInternal(contentBody, controlName, multiPartConfig.defaultFileName(), OCTET_STREAM));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification multiPart(String controlName, String contentBody, String mimeType) {
		MultiPartConfig multiPartConfig = config.getMultiPartConfig();
		multiParts.add(new MultiPartInternal(contentBody, controlName, multiPartConfig.defaultFileName(), mimeType));
		return this;
	}

	@Override
	public WebTestClientRequestSpecification config(RestAssuredWebTestClientConfig config) {
		assignConfig(config);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification config(SpecificationConfig config) {
		if (!(config instanceof RestAssuredWebTestClientConfig)) {
			throw new IllegalArgumentException("Only WebTestClientRequestSpecificationConfig allowed here");
		}
		assignConfig((RestAssuredWebTestClientConfig) config);
		return this;
	}

	@Override
	public WebTestClientRequestSpecification spec(WebTestClientRequestSpecification requestSpecificationToMerge) {
		notNull(requestSpecificationToMerge, WebTestClientRequestSpecification.class);

		if (!(requestSpecificationToMerge instanceof WebTestClientRequestSpecificationImpl)) {
			throw new IllegalArgumentException("requestSpecificationToMerge must be an instance of " + WebTestClientRequestSpecificationImpl.class.getName());
		}
		WebTestClientRequestSpecificationImpl specificationToMerge = (WebTestClientRequestSpecificationImpl) requestSpecificationToMerge;
		Object otherRequestBody = specificationToMerge.getRequestBody();
		if (otherRequestBody != null) {
			requestBody = otherRequestBody;
		}
		if (isNotEmpty(specificationToMerge.getBasePath())) {
			basePath = specificationToMerge.getBasePath();
		}
		WebTestClientFactory otherWebTestClientFactory = specificationToMerge.getWebTestClientFactory();
		if (otherWebTestClientFactory != null && otherWebTestClientFactory.isAssigned()) {
			webTestClientFactory = otherWebTestClientFactory;
		}
		cookies(specificationToMerge.getCookies());
		headers(specificationToMerge.getRequestHeaders());
		mergeConfig(this, specificationToMerge);
		formParams(specificationToMerge.getFormParams());
		queryParams(specificationToMerge.getQueryParams());
		params(specificationToMerge.getParams());
		attributes(specificationToMerge.getAttributes());
		multiParts.addAll(specificationToMerge.getMultiParts());
		RequestLoggingFilter otherRequestLoggingFilter = specificationToMerge.getRequestLoggingFilter();
		if (otherRequestLoggingFilter != null) {
			requestLoggingFilter = otherRequestLoggingFilter;
		}
		AsyncConfig otherAsyncConfig = specificationToMerge.getAsyncConfig();
		if (otherAsyncConfig != null) {
			asyncConfig = otherAsyncConfig;
		}
		return this;
	}

	@Override
	public WebTestClientRequestSpecification sessionId(String sessionIdValue) {
		return sessionId(config.getSessionConfig().sessionIdName(), sessionIdValue);
	}

	@Override
	public WebTestClientRequestSpecification sessionId(String sessionIdName, String sessionIdValue) {
		notNull(sessionIdName, "Session id name");
		notNull(sessionIdValue, "Session id value");
		if (cookies.hasCookieWithName(sessionIdName)) {
			CookieHelper.sessionId(cookies, sessionIdName, sessionIdValue);
		} else {
			cookie(sessionIdName, sessionIdValue);
		}
		return this;
	}

	@Override
	public WebTestClientRequestSender when() {
		LogConfig logConfig = config.getLogConfig();
		if (requestLoggingFilter == null && logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
			log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(),
					logConfig.isPrettyPrintingEnabled());
		}
		WebTestClient webTestClient = webTestClientFactory.build(config.getWebTestClientConfig());
		return new WebTestClientRequestSenderImpl(webTestClient, params, queryParams, formParams, attributes, config, requestBody,
				requestHeaders, cookies, multiParts, requestLoggingFilter, basePath,
				responseSpecification, logRepository);
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
	public WebTestClientRequestSpecification webTestClient(WebTestClient webTestClient) {
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

	public Object getRequestBody() {
		return requestBody;
	}

	public String getRequestContentType() {
		Header header = requestHeaders.get(CONTENT_TYPE);
		if (header != null) {
			return header.getValue();
		}
		return null;
	}

	@Override
	public WebTestClientResponse get(Function<UriBuilder, URI> uriFunction) {
		return when().get(uriFunction);
	}

	@Override
	public WebTestClientResponse post(Function<UriBuilder, URI> uriFunction) {
		return when().post(uriFunction);
	}

	@Override
	public WebTestClientResponse put(Function<UriBuilder, URI> uriFunction) {
		return when().put(uriFunction);
	}

	@Override
	public WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction) {
		return when().delete(uriFunction);
	}

	@Override
	public WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction) {
		return when().patch(uriFunction);
	}

	@Override
	public WebTestClientResponse head(Function<UriBuilder, URI> uriFunction) {
		return when().head(uriFunction);
	}

	@Override
	public WebTestClientResponse options(Function<UriBuilder, URI> uriFunction) {
		return when().options(uriFunction);
	}

	@Override
	public WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction) {
		return when().request(method, uriFunction);
	}

	@Override
	public WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction) {
		return when().request(method, uriFunction);
	}

	@Override
	public WebTestClientRequestSender consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer) {
		return when().consumeWith(consumer);
	}

	@Override
	public WebTestClientResponse get(String path, Object... pathParams) {
		return when().get(path, pathParams);
	}

	@Override
	public WebTestClientResponse get(String path, Map<String, ?> pathParams) {
		return when().get(path, pathParams);
	}

	@Override
	public WebTestClientResponse post(String path, Object... pathParams) {
		return when().post(path, pathParams);
	}

	@Override
	public WebTestClientResponse post(String path, Map<String, ?> pathParams) {
		return when().post(path, pathParams);
	}

	@Override
	public WebTestClientResponse put(String path, Object... pathParams) {
		return when().put(path, pathParams);
	}

	@Override
	public WebTestClientResponse put(String path, Map<String, ?> pathParams) {
		return when().put(path, pathParams);
	}

	@Override
	public WebTestClientResponse delete(String path, Object... pathParams) {
		return when().delete(path, pathParams);
	}

	@Override
	public WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
		return when().delete(path, pathParams);
	}

	@Override
	public WebTestClientResponse head(String path, Object... pathParams) {
		return when().head(path, pathParams);
	}

	@Override
	public WebTestClientResponse head(String path, Map<String, ?> pathParams) {
		return when().head(path, pathParams);
	}

	@Override
	public WebTestClientResponse patch(String path, Object... pathParams) {
		return when().patch(path, pathParams);
	}

	@Override
	public WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
		return when().patch(path, pathParams);
	}

	@Override
	public WebTestClientResponse options(String path, Object... pathParams) {
		return when().options(path, pathParams);
	}

	@Override
	public WebTestClientResponse options(String path, Map<String, ?> pathParams) {
		return when().options(path, pathParams);
	}

	@Override
	public WebTestClientResponse get(URI uri) {
		return when().get(uri);
	}

	@Override
	public WebTestClientResponse post(URI uri) {
		return when().post(uri);
	}

	@Override
	public WebTestClientResponse put(URI uri) {
		return when().put(uri);
	}

	@Override
	public WebTestClientResponse delete(URI uri) {
		return when().delete(uri);
	}

	@Override
	public WebTestClientResponse head(URI uri) {
		return when().head(uri);
	}

	@Override
	public WebTestClientResponse patch(URI uri) {
		return when().patch(uri);
	}

	@Override
	public WebTestClientResponse options(URI uri) {
		return when().options(uri);
	}

	@Override
	public WebTestClientResponse get(URL url) {
		return when().get(url);
	}

	@Override
	public WebTestClientResponse post(URL url) {
		return when().post(url);
	}

	@Override
	public WebTestClientResponse put(URL url) {
		return when().put(url);
	}

	@Override
	public WebTestClientResponse delete(URL url) {
		return when().delete(url);
	}

	@Override
	public WebTestClientResponse head(URL url) {
		return when().head(url);
	}

	@Override
	public WebTestClientResponse patch(URL url) {
		return when().patch(url);
	}

	@Override
	public WebTestClientResponse options(URL url) {
		return when().options(url);
	}

	@Override
	public WebTestClientResponse get() {
		return when().get();
	}

	@Override
	public WebTestClientResponse post() {
		return when().post();
	}

	@Override
	public WebTestClientResponse put() {
		return when().put();
	}

	@Override
	public WebTestClientResponse delete() {
		return when().delete();
	}

	@Override
	public WebTestClientResponse head() {
		return when().head();
	}

	@Override
	public WebTestClientResponse patch() {
		return when().patch();
	}

	@Override
	public WebTestClientResponse options() {
		return when().options();
	}

	@Override
	public WebTestClientResponse request(Method method) {
		return when().request(method);
	}

	@Override
	public WebTestClientResponse request(String method) {
		return when().request(method);
	}

	@Override
	public WebTestClientResponse request(Method method, String path, Object... pathParams) {
		return when().request(method, path, pathParams);
	}

	@Override
	public WebTestClientResponse request(String method, String path, Object... pathParams) {
		return when().request(method, path, pathParams);
	}

	@Override
	public WebTestClientResponse request(Method method, URI uri) {
		return when().request(method, uri);
	}

	@Override
	public WebTestClientResponse request(Method method, URL url) {
		return when().request(method, url);
	}

	@Override
	public WebTestClientResponse request(String method, URI uri) {
		return when().request(method, uri);
	}

	@Override
	public WebTestClientResponse request(String method, URL url) {
		return when().request(method, url);
	}

	public String getBasePath() {
		return basePath;
	}

	public WebTestClientFactory getWebTestClientFactory() {
		return webTestClientFactory;
	}

	public Cookies getCookies() {
		return cookies;
	}

	public Headers getRequestHeaders() {
		return requestHeaders;
	}

	private void mergeConfig(WebTestClientRequestSpecificationImpl thisOne, WebTestClientRequestSpecificationImpl other) {
		config((RestAssuredWebTestClientConfig) ConfigMergeUtils.mergeConfig(thisOne.getRestAssuredWebTestClientConfig(),
				other.getRestAssuredWebTestClientConfig()));
	}

	public Map<String, Object> getFormParams() {
		return formParams;
	}

	public Map<String, Object> getQueryParams() {
		return queryParams;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public List<MultiPartInternal> getMultiParts() {
		return multiParts;
	}

	public RequestLoggingFilter getRequestLoggingFilter() {
		return requestLoggingFilter;
	}

	public AsyncConfig getAsyncConfig() {
		return asyncConfig;
	}

	public RestAssuredWebTestClientConfig getRestAssuredWebTestClientConfig() {
		return config;
	}

	public LogRepository getLogRepository() {
		return logRepository;
	}

	public void setRequestLoggingFilter(RequestLoggingFilter requestLoggingFilter) {
		this.requestLoggingFilter = requestLoggingFilter;
	}

	public RestAssuredConfig getRestAssuredConfig() {
		return ConfigConverter.convertToRestAssuredConfig(config);
	}

	public WebTestClientRequestSpecification basePath(String path) {
		notNull(path, "Base path");
		this.basePath = path;
		return this;
	}

	private String serializeIfNeeded(Object object) {
		return Serializer.serializeIfNeeded(object, getRequestContentType(), config);
	}

	private static WebTestClientParamConfig toWebTestClientParamConfig(ParamConfig paramConfig) {
		if (!(paramConfig instanceof WebTestClientParamConfig)) {
			throw new IllegalArgumentException("Wrong ParamConfig passed to method.");
		}
		return (WebTestClientParamConfig) paramConfig;
	}

	private static ParamConfig.UpdateStrategy convert(WebTestClientParamConfig.UpdateStrategy updateStrategy) {
		return ParamConfig.UpdateStrategy.valueOf(updateStrategy.name());
	}

}
