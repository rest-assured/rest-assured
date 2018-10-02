package io.restassured.module.webtestclient.internal;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.restassured.RestAssured;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.Cookie;
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
import io.restassured.module.webtestclient.specification.WebTestClientRequestAsyncConfigurer;
import io.restassured.module.webtestclient.specification.WebTestClientRequestAsyncSender;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.internal.assertion.AssertParameter.notNull;
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static io.restassured.module.spring.commons.BodyHelper.toByteArray;
import static io.restassured.module.spring.commons.HeaderHelper.mapToArray;
import static io.restassured.module.spring.commons.RequestLogger.logParamsAndHeaders;
import static io.restassured.module.spring.commons.RequestLogger.logRequestBody;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.parseMediaType;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestSenderImpl implements WebTestClientRequestAsyncSender, WebTestClientRequestAsyncConfigurer {

	/*
	 List<ResultHandler> resultHandlers,
	 List<RequestPostProcessor> requestPostProcessors,
	 */

	private static final String ATTRIBUTE_NAME_URL_TEMPLATE = "org.springframework.restdocs.urlTemplate";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String LINE_SEPARATOR = "line.separator";

	private final WebTestClient webTestClient;
	private final Map<String, Object> params;
	private final Map<String, Object> queryParams;
	private final Map<String, Object> formParams;
	private final Map<String, Object> attributes;
	private final RestAssuredWebTestClientConfig config;
	private final Object requestBody;
	private final Object authentication;
	private final Cookies cookies;
	private final List<MultiPartInternal> multiParts;
	private final boolean isAsyncRequest;
	private final String basePath;
	private final ResponseSpecification responseSpecification;
	private final Map<String, Object> sessionAttributes;
	private final LogRepository logRepository;
	private Headers headers;
	private final RequestLoggingFilter requestLoggingFilter;

	WebTestClientRequestSenderImpl(WebTestClient webTestClient, Map<String, Object> params,
	                               Map<String, Object> queryParams, Map<String, Object> formParams,
	                               Map<String, Object> attributes,
	                               RestAssuredWebTestClientConfig config, Object requestBody, Headers headers,
	                               Cookies cookies, Map<String, Object> sessionAttributes,
	                               List<MultiPartInternal> multiParts, RequestLoggingFilter requestLoggingFilter,
	                               String basePath, ResponseSpecification responseSpecification,
	                               Object authentication, LogRepository logRepository) {
		this(webTestClient, params, queryParams, formParams, attributes, config, requestBody, headers, cookies,
				sessionAttributes, multiParts, requestLoggingFilter,
				basePath, responseSpecification, authentication, logRepository, false);
	}

	private WebTestClientRequestSenderImpl(WebTestClient webTestClient, Map<String, Object> params, Map<String,
			Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
	                                       RestAssuredWebTestClientConfig config, Object requestBody, Headers headers,
	                                       Cookies cookies, Map<String, Object> sessionAttributes,
	                                       List<MultiPartInternal> multiParts,
	                                       RequestLoggingFilter requestLoggingFilter, String basePath,
	                                       ResponseSpecification responseSpecification,
	                                       Object authentication, LogRepository logRepository, boolean isAsyncRequest) {
		this.webTestClient = webTestClient;
		this.params = params;
		this.queryParams = queryParams;
		this.formParams = formParams;
		this.attributes = attributes;
		this.config = config;
		this.requestBody = requestBody;
		this.headers = headers;
		this.cookies = cookies;
		this.sessionAttributes = sessionAttributes;
		this.multiParts = multiParts;
		this.basePath = basePath;
		this.responseSpecification = responseSpecification;
		this.authentication = authentication;
		this.logRepository = logRepository;
		this.isAsyncRequest = isAsyncRequest;
		this.requestLoggingFilter = requestLoggingFilter;
	}
	@Override
	public WebTestClientRequestAsyncConfigurer async() {
		return new WebTestClientRequestSenderImpl(webTestClient, params, queryParams, formParams,
				attributes, config, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter,
				basePath, responseSpecification, authentication, logRepository, true);
	}

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
			throw new IllegalArgumentException("HTTP method '" + method + "' is not supported by MockMvc");
		}
		return httpMethod;
	}	@Override
	public WebTestClientResponse get(String path, Object... pathParams) {
		return sendRequest(GET, path, pathParams);
	}

	// TODO
	private WebTestClientResponse sendRequest(HttpMethod method, Function<UriBuilder, URI> uriFunction) {
		return null;
	}

	private WebTestClientResponse sendRequest(HttpMethod method, String path, Object[] pathParams) {
		notNull(path, "Path");
		verifyNoBodyAndMultipartTogether();

		String baseUri;
		if (isNotBlank(basePath)) {
			baseUri = mergeAndRemoveDoubleSlash(basePath, path);
		} else {
			baseUri = path;
		}

		// TODO: handle params and queryParam differently?
		// TODO: refactor duplicates

		final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUri);
		if (!queryParams.isEmpty()) {
			new ParamApplier(queryParams) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					uriComponentsBuilder.queryParam(paramName, paramValues);
				}
			}.applyParams();
		}

		String requestContentType = HeaderHelper.findContentType(headers, (List<Object>) (List<?>) multiParts, config);
		if (!params.isEmpty()) {
			new ParamApplier(params) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					uriComponentsBuilder.queryParam(paramName, paramValues);
				}
			}.applyParams();

			if (StringUtils.isBlank(requestContentType) && method == POST && !isMultipartRequest()) {  // TODO: handle POST content type
				setContentTypeToApplicationFormUrlEncoded(requestContentType);
			}
		}

		if (!multiParts.isEmpty()) {
			sendMultiPartRequest();
		}

//		if (!formParams.isEmpty()) {  // TODO: handle form params
//			if (method == GET) {
//				throw new IllegalArgumentException("Cannot use form parameters in a GET request");
//			}
//			new ParamApplier(formParams) {
//				@Override
//				protected void applyParam(String paramName, String[] paramValues) {
//					uriComponentsBuilder.queryParam(paramName, paramValues);
//				}
//			}.applyParams();
//		}

		String uri = uriComponentsBuilder.buildAndExpand(pathParams).encode().toUriString();

		WebTestClient.RequestBodySpec requestBodySpec = webTestClient.method(method)
				.uri(uri); // TODO


		if (StringUtils.isNotBlank(requestContentType)) {
			requestBodySpec.contentType(parseMediaType(requestContentType));
		}

		// TODO: add publisher body
		if (requestBody != null) {
			if (requestBody instanceof byte[]) {
				requestBodySpec.body(BodyInserters.fromObject(requestBody));
			} else if (requestBody instanceof File) {
				byte[] bytes = toByteArray((File) requestBody);
				requestBodySpec.body(BodyInserters.fromObject(bytes));
			} else {
				requestBodySpec.body(BodyInserters.fromObject(requestBody.toString()));
			}
		}

		if (!attributes.isEmpty()) {
			new ParamApplier(attributes) {
				@Override
				protected void applyParam(String paramName, String[] paramValues) {
					requestBodySpec.attribute(paramName, paramValues[0]);
				}
			}.applyParams();
		}

//
//		if (RestDocsClassPathChecker.isSpringRestDocsInClasspath() && config.getMockMvcConfig().shouldAutomaticallyApplySpringRestDocsMockMvcSupport()) {
//			request.requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, PathSupport.getPath(uri));
//		} // TODO: handle restdocs config


		headers.forEach(header -> requestBodySpec.header(header.getName(), header.getValue()));
		cookies.asList().forEach(cookie -> requestBodySpec.cookie(cookie.getName(), cookie.getValue()));

//
//		if (!sessionAttributes.isEmpty()) {
//			request.sessionAttrs(sessionAttributes);
//		}  // TODO

		logRequestIfApplicable(method, baseUri, path, pathParams); // TODO

		return performRequest(requestBodySpec);
	}

	private boolean isMultipartRequest() {
		return !multiParts.isEmpty();
	}

	private void setContentTypeToApplicationFormUrlEncoded(String requestContentType) {
		requestContentType = parseMediaType(HeaderHelper.buildApplicationFormEncodedContentType(config,
				APPLICATION_FORM_URLENCODED_VALUE)).toString();
		List<Header> newHeaders = new ArrayList<Header>(headers.asList());
		newHeaders.add(new Header(CONTENT_TYPE, requestContentType));
		headers = new Headers(newHeaders);
	}

	private void logRequestIfApplicable(HttpMethod method, String uri, String originalPath, Object[] unnamedPathParams) {
		if (requestLoggingFilter == null) {
			return;
		}

		final RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("http://localhost", RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.<Filter>emptyList(),
				null, true, ConfigConverter.convertToRestAssuredConfig(config), logRepository, null);
		logParamsAndHeaders(reqSpec, method.toString(), uri, unnamedPathParams, params, queryParams, formParams, headers, cookies);
		logRequestBody(reqSpec, requestBody, headers, (List<Object>) (List<?>) multiParts, config);

		if (multiParts != null) {
			for (MultiPartInternal multiPart : multiParts) {
				reqSpec.multiPart(new MultiPartSpecBuilder(multiPart.getContent()).
						controlName(multiPart.getControlName()).
						fileName(multiPart.getFileName()).
						mimeType(multiPart.getMimeType()).
						build());
			}
		}
		String uriPath = PathSupport.getPath(uri);
		String originalUriPath = PathSupport.getPath(originalPath);
		requestLoggingFilter.filter(reqSpec, null, new FilterContextImpl(uri, originalUriPath,
				uriPath, uri, uri, new Object[0], method.toString(), null,
				Collections.<Filter>emptyList().iterator(), new HashMap<String, Object>()));
	}

	private void verifyNoBodyAndMultipartTogether() {
		if (requestBody != null && !multiParts.isEmpty()) {
			throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request. Perhaps you want to change the body to a multi part?");
		}
	}

	private void sendMultiPartRequest() {

//		if (multiParts.isEmpty()) {
//			request = MockMvcRequestBuilders.request(method, uri, pathParams);
//		} else if (method != POST) {
//			throw new IllegalArgumentException("Currently multi-part file data uploading only works for " + POST);
//		} else {
//			request = MockMvcRequestBuilders.fileUpload(uri, pathParams);
//		}

//		if (!multiParts.isEmpty()) {
//			MockMultipartHttpServletRequestBuilder multiPartRequest = (MockMultipartHttpServletRequestBuilder) request;
//			for (MockMvcMultiPart multiPart : multiParts) {
//				MockMultipartFile multipartFile;
//				String fileName = multiPart.getFileName();
//				String controlName = multiPart.getControlName();
//				String mimeType = multiPart.getMimeType();
//				if (multiPart.isByteArray()) {
//					multipartFile = new MockMultipartFile(controlName, fileName, mimeType, (byte[]) multiPart.getContent());
//				} else if (multiPart.isFile() || multiPart.isInputStream()) {
//					InputStream inputStream;
//					if (multiPart.isFile()) {
//						try {
//							inputStream = new FileInputStream((File) multiPart.getContent());
//						} catch (FileNotFoundException e) {
//							return SafeExceptionRethrower.safeRethrow(e);
//						}
//					} else {
//						inputStream = (InputStream) multiPart.getContent();
//					}
//					try {
//						multipartFile = new MockMultipartFile(controlName, fileName, mimeType, inputStream);
//					} catch (IOException e) {
//						return SafeExceptionRethrower.safeRethrow(e);
//					}
//				} else { // String
//					multipartFile = new MockMultipartFile(controlName, fileName, mimeType, ((String) multiPart.getContent()).getBytes());
//				}
//				multiPartRequest.file(multipartFile);
//			}
//		}

		throw new UnsupportedOperationException("Please, implement me.");
	}

	private WebTestClientResponse performRequest(WebTestClient.RequestBodySpec requestBuilder) {
		FluxExchangeResult<Object> result;

//		if (isSpringSecurityInClasspath() && authentication instanceof org.springframework.security.core.Authentication) {
//			org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) authentication);
//		}
//		if (authentication instanceof Principal) {
//			requestBuilder.principal((Principal) authentication);
//		} // TODO: handle auth

		WebTestClientRestAssuredResponseImpl restAssuredResponse;

		try {
			final long start = System.currentTimeMillis();  // TODO support webtestclient expect assertions
			WebTestClient.ResponseSpec responseSpec = requestBuilder.exchange();
			final long responseTime = System.currentTimeMillis() - start;
			result = responseSpec.returnResult(Object.class);
			restAssuredResponse = new WebTestClientRestAssuredResponseImpl(responseSpec, logRepository);
			restAssuredResponse.setConfig(ConfigConverter.convertToRestAssuredConfig(config));
			restAssuredResponse.setContent(result.getResponseBodyContent());
			MediaType contentType = result.getResponseHeaders().getContentType();
			restAssuredResponse.setContentType(ofNullable(contentType).map(MimeType::toString).orElse(null));
			restAssuredResponse.setHasExpectations(false);
			restAssuredResponse.setStatusCode(result.getStatus().value());
			List<Header> responseHeaders = assembleHeaders(result.getResponseHeaders());
			restAssuredResponse.setResponseHeaders(new Headers(responseHeaders));
			restAssuredResponse.setRpr(getRpr());
			restAssuredResponse.setStatusLine(String.valueOf(result.getStatus().value()));
			restAssuredResponse.setFilterContextProperties(new HashMap() {{
				put(TimingFilter.RESPONSE_TIME_MILLISECONDS, responseTime);
			}});
			restAssuredResponse.setCookies(convertCookies(result.getResponseCookies()));

			if (responseSpecification != null) {
				responseSpecification.validate(ResponseConverter.toStandardResponse(restAssuredResponse));
			}

		} catch (Exception e) {
			return SafeExceptionRethrower.safeRethrow(e);
//		} finally {
//			if (isSpringSecurityInClasspath()) {
//				org.springframework.security.core.context.SecurityContextHolder.clearContext();
//			} // TODO
		}
		return restAssuredResponse;
	}

	private List<Header> assembleHeaders(HttpHeaders headers) {
		return headers.keySet().stream()
				.map(headerName -> headers.get(headerName).stream()
						.map(headerValue -> new Header(headerName, headerValue))
						.collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.toList());
	}

	private ResponseParserRegistrar getRpr() {
		if (responseSpecification instanceof ResponseSpecificationImpl) {
			return ((ResponseSpecificationImpl) responseSpecification).getRpr();
		}
		return new ResponseParserRegistrar();
	}

	private Cookies convertCookies(MultiValueMap<String, ResponseCookie> responseCookies) {
		List<Cookie> cookies = responseCookies.keySet().stream().map(cookie -> responseCookies.get(cookie).stream()
				.map(responseCookie -> new Cookie.Builder(responseCookie.getName(), responseCookie.getValue()).build())
				.collect(Collectors.toList())
		).flatMap(Collection::stream).collect(Collectors.toList());
		return new Cookies(cookies);
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


}
