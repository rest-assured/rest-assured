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

package io.restassured.module.mockmvc.internal;

import io.restassured.RestAssured;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.*;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.filter.FilterContextImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.support.PathSupport;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestAsyncConfigurer;
import io.restassured.module.mockmvc.specification.MockMvcRequestAsyncSender;
import io.restassured.module.mockmvc.specification.MockMvcRequestSender;
import io.restassured.module.mockmvc.util.ReflectionUtil;
import io.restassured.module.spring.commons.BodyHelper;
import io.restassured.module.spring.commons.HeaderHelper;
import io.restassured.module.spring.commons.ParamApplier;
import io.restassured.module.spring.commons.config.AsyncConfig;
import io.restassured.module.spring.commons.config.ConfigConverter;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.common.classpath.ClassPathResolver.existInCP;
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static io.restassured.module.mockmvc.internal.SpringSecurityClassPathChecker.isSpringSecurityInClasspath;
import static io.restassured.module.mockmvc.util.ReflectionUtil.invokeConstructor;
import static io.restassured.module.mockmvc.util.ReflectionUtil.invokeMethod;
import static io.restassured.module.spring.commons.HeaderHelper.mapToArray;
import static io.restassured.module.spring.commons.RequestLogger.logParamsAndHeaders;
import static io.restassured.module.spring.commons.RequestLogger.logRequestBody;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

class MockMvcRequestSenderImpl implements MockMvcRequestSender, MockMvcRequestAsyncConfigurer, MockMvcRequestAsyncSender {
    private static final String ATTRIBUTE_NAME_URL_TEMPLATE = "org.springframework.restdocs.urlTemplate";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final boolean isSpring6OrLater = existInCP("org.springframework.aot.AotDetector");
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    private final MockMvc mockMvc;
    private final Map<String, Object> params;
    private final Map<String, Object> namedPathParams;
    private final Map<String, Object> queryParams;
    private final Map<String, Object> formParams;
    private final Map<String, Object> attributes;
    private final RestAssuredMockMvcConfig config;
    private final Object requestBody;
    private Headers headers;
    private final Cookies cookies;
    private final List<MockMvcMultiPart> multiParts;
    private final RequestLoggingFilter requestLoggingFilter;
    private final List<ResultHandler> resultHandlers;
    private final List<RequestPostProcessor> requestPostProcessors;
    private final MockHttpServletRequestBuilderInterceptor interceptor;
    private final String basePath;
    private final ResponseSpecification responseSpecification;
    private final Object authentication;
    private final LogRepository logRepository;
    private final boolean isAsyncRequest;
    private final Map<String, Object> sessionAttributes;

    MockMvcRequestSenderImpl(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> namedPathParams, Map<String, Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
                             RestAssuredMockMvcConfig config, Object requestBody, Headers headers, Cookies cookies, Map<String, Object> sessionAttributes,
                             List<MockMvcMultiPart> multiParts, RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers,
                             List<RequestPostProcessor> requestPostProcessors, MockHttpServletRequestBuilderInterceptor interceptor, String basePath, ResponseSpecification responseSpecification,
                             Object authentication, LogRepository logRepository) {
        this(mockMvc, params, namedPathParams, queryParams, formParams, attributes, config, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter, resultHandlers, requestPostProcessors, interceptor,
                basePath, responseSpecification, authentication, logRepository, false);
    }

    private MockMvcRequestSenderImpl(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> namedPathParams, Map<String, Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
                                     RestAssuredMockMvcConfig config, Object requestBody, Headers headers, Cookies cookies, Map<String, Object> sessionAttributes,
                                     List<MockMvcMultiPart> multiParts, RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers,
                                     List<RequestPostProcessor> requestPostProcessors, MockHttpServletRequestBuilderInterceptor interceptor, String basePath, ResponseSpecification responseSpecification,
                                     Object authentication, LogRepository logRepository, boolean isAsyncRequest) {
        this.mockMvc = mockMvc;
        this.params = params;
        this.namedPathParams = namedPathParams;
        this.queryParams = queryParams;
        this.formParams = formParams;
        this.attributes = attributes;
        this.config = config;
        this.requestBody = requestBody;
        this.headers = headers;
        this.cookies = cookies;
        this.sessionAttributes = sessionAttributes;
        this.multiParts = multiParts;
        this.requestLoggingFilter = requestLoggingFilter;
        this.resultHandlers = resultHandlers;
        this.requestPostProcessors = requestPostProcessors;
        this.interceptor = interceptor;
        this.basePath = basePath;
        this.responseSpecification = responseSpecification;
        this.authentication = authentication;
        this.logRepository = logRepository;
        this.isAsyncRequest = isAsyncRequest;
    }

    private Object assembleHeaders(MockHttpServletResponse response) {
        Collection<String> headerNames = response.getHeaderNames();

        List<Header> headers = new ArrayList<>();
        for (String headerName : headerNames) {
            List<String> headerValues = response.getHeaders(headerName);
            for (String headerValue : headerValues) {
                headers.add(new Header(headerName, headerValue));
            }
        }
        return new Headers(headers);
    }

    private Cookies convertCookies(Object[] servletCookies) {
        List<Cookie> cookies = new ArrayList<>();
        for (Object servletCookie : servletCookies) {
            Cookie.Builder cookieBuilder = new Cookie.Builder(
                    invokeMethod(servletCookie, "getName"),
                    invokeMethod(servletCookie, "getValue")
            );

            String comment = invokeMethod(servletCookie, "getComment");
            if (comment != null) {
                cookieBuilder.setComment(comment);
            }

            String domain = invokeMethod(servletCookie, "getDomain");
            if (domain != null) {
                cookieBuilder.setDomain(domain);
            }

            String path = invokeMethod(servletCookie, "getPath");
            if (path != null) {
                cookieBuilder.setPath(path);
            }
            int getMaxAge = invokeMethod(servletCookie, "getMaxAge");
            cookieBuilder.setMaxAge(getMaxAge);
            cookieBuilder.setVersion(invokeMethod(servletCookie, "getVersion"));
            cookieBuilder.setSecured(invokeMethod(servletCookie, "getSecure"));
            cookieBuilder.setHttpOnly(invokeMethod(servletCookie, "isHttpOnly"));

            // Attempt to copy properties from org.springframework.mock.web.MockCookie
            try {
            	String sameSite = invokeMethod(servletCookie, "getSameSite");
            	if(sameSite != null) {
                    cookieBuilder.setSameSite(sameSite);
            	}
            } catch(IllegalArgumentException e) {
            	// Do nothing as only found on MockCookie
            }

            try {
                ZonedDateTime expires = invokeMethod(servletCookie, "getExpires");
                if(expires != null) {
                    cookieBuilder.setExpiryDate(Date.from(expires.toInstant()));
                }
            } catch(IllegalArgumentException e) {
            	// Do nothing as only found on MockCookie
            }

            cookies.add(cookieBuilder.build());
        }
        return new Cookies(cookies);
    }

    @SuppressWarnings("unchecked")
    private MockMvcResponse performRequest(Object requestBuilder) {
        MockHttpServletResponse response;

        if (interceptor != null) {
			ReflectionUtil.invokeMethod(interceptor, "intercept", new Class[] { MockHttpServletRequestBuilder.class }, requestBuilder);
        }

        if (isSpringSecurityInClasspath() && authentication instanceof org.springframework.security.core.Authentication) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) authentication);
        }
        if (authentication instanceof Principal) {
			// To support Spring Framework 7
			ReflectionUtil.invokeMethod(requestBuilder, "principal", new Class[] { Principal.class },  authentication);
        }

        for (RequestPostProcessor requestPostProcessor : requestPostProcessors) {
			// To support Spring Framework 7
			ReflectionUtil.invokeMethod(requestBuilder, "with", new Class[] { RequestPostProcessor.class }, requestPostProcessor);
        }

        MockMvcRestAssuredResponseImpl restAssuredResponse;
        try {
            final long start = System.currentTimeMillis();
            ResultActions perform = mockMvc.perform((RequestBuilder) requestBuilder);
            final long responseTime = System.currentTimeMillis() - start;
            if (!resultHandlers.isEmpty()) {
                for (ResultHandler resultHandler : resultHandlers) {
                    perform.andDo(resultHandler);
                }
            }
            MvcResult mvcResult = getMvcResult(perform, isAsyncRequest);
            response = mvcResult.getResponse();
            restAssuredResponse = new MockMvcRestAssuredResponseImpl(perform, logRepository);
            restAssuredResponse.setConfig(ConfigConverter.convertToRestAssuredConfig(config));
            restAssuredResponse.setDecoderConfig(config.getDecoderConfig());
            restAssuredResponse.setContent(response.getContentAsByteArray());
            restAssuredResponse.setContentType(response.getContentType());
            restAssuredResponse.setHasExpectations(false);
            restAssuredResponse.setStatusCode(response.getStatus());
            restAssuredResponse.setResponseHeaders(assembleHeaders(response));
            restAssuredResponse.setRpr(getRpr());
            restAssuredResponse.setStatusLine(assembleStatusLine(response, mvcResult.getResolvedException()));
            restAssuredResponse.setFilterContextProperties(new HashMap() {{
                put(TimingFilter.RESPONSE_TIME_MILLISECONDS, responseTime);
            }});
            restAssuredResponse.setCookies(convertCookies(invokeMethod(response, "getCookies")));

            if (responseSpecification != null) {
                responseSpecification.validate(ResponseConverter.toStandardResponse(restAssuredResponse));
            }

        } catch (Exception e) {
            return SafeExceptionRethrower.safeRethrow(e);
        } finally {
            if (isSpringSecurityInClasspath()) {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }
        return restAssuredResponse;
    }

    private MvcResult getMvcResult(ResultActions perform, boolean isAsyncRequest) throws Exception {
        MvcResult mvcResult;
        if (isAsyncRequest) {
            MvcResult startedAsyncRequestProcessing = perform.andExpect(MockMvcResultMatchers.request().asyncStarted()).andReturn();
            startedAsyncRequestProcessing.getAsyncResult(config.getAsyncConfig().timeoutInMs());
            mvcResult = mockMvc.perform(asyncDispatch(startedAsyncRequestProcessing)).andReturn();
        } else {
            mvcResult = perform.andReturn();
        }
        return mvcResult;
    }

    private ResponseParserRegistrar getRpr() {
        if (responseSpecification instanceof ResponseSpecificationImpl) {
            return ((ResponseSpecificationImpl) responseSpecification).getRpr();
        }
        return new ResponseParserRegistrar();
    }

    private String assembleStatusLine(MockHttpServletResponse response, Exception resolvedException) {
        StringBuilder builder = new StringBuilder();
        builder.append(response.getStatus());
        if (isNotBlank(response.getErrorMessage())) {
            builder.append(" ").append(response.getErrorMessage());
        } else if (resolvedException != null) {
            builder.append(" ").append(resolvedException.getMessage());
        }
        return builder.toString();
    }

    private MockMvcResponse sendRequest(HttpMethod method, String path, Object[] unnamedPathParams) {
        notNull(path, "Path");
        verifyNoBodyAndMultipartTogether();

        final String baseUri = buildBaseUri(path);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUri);

        applyQueryParams(uriComponentsBuilder);
        applyPathParams(uriComponentsBuilder, baseUri, unnamedPathParams);

        final String uri = uriComponentsBuilder.build().toUriString();
        final Object request =  applyMultiPartsAndGetRequest(method, uri, unnamedPathParams);

        String requestContentType = HeaderHelper.findContentType(headers, (List<Object>) (List<?>) multiParts, config);
        applyParams(request, method, requestContentType);

        applyFormParams(request, method, requestContentType);
        applyAttributes(request);

        if (RestDocsClassPathChecker.isSpringRestDocsInClasspath() && config.getMockMvcConfig().shouldAutomaticallyApplySpringRestDocsMockMvcSupport()) {
			ReflectionUtil.invokeMethod(request, "requestAttr", new Class[] { String.class, Object.class }, ATTRIBUTE_NAME_URL_TEMPLATE, PathSupport.getPath(baseUri));
        }

        applyHeaders(request);

        if (StringUtils.isNotBlank(requestContentType)) {
			// To support Spring Framework 7
			ReflectionUtil.invokeMethod(request, "contentType", new Class[] { MediaType.class }, MediaType.parseMediaType(requestContentType));
        }

        applyCookies(request);

        if (!sessionAttributes.isEmpty()) {
			// To support Spring Framework 7
			ReflectionUtil.invokeMethod(request, "sessionAttrs", new Class[] { Map.class }, sessionAttributes);
        }

        try {
            applyMultipartBody(request);
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }

        applyRequestBody(request);
        logRequestIfApplicable(method, baseUri, path, unnamedPathParams);

        return performRequest(request);
    }

    private void verifyNoBodyAndMultipartTogether() {
        if (requestBody != null && !multiParts.isEmpty()) {
            throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request. Perhaps you want to change the body to a multi part?");
        }
    }

    private String buildBaseUri(final String path) {
        if (isNotBlank(basePath)) {
            return mergeAndRemoveDoubleSlash(basePath, path);
        }

        return path;
    }

    private void applyQueryParams(final UriComponentsBuilder uriComponentsBuilder) {
        if (!queryParams.isEmpty()) {
            new ParamApplier(queryParams) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    uriComponentsBuilder.queryParam(paramName, paramValues);
                }
            }.applyParams();
        }
    }

    private void applyPathParams(
            final UriComponentsBuilder uriComponentsBuilder,
            final String baseUri,
            final Object[] unnamedPathParams
    ) {
        final Matcher pathParamMatcher = PATH_PARAM_PATTERN.matcher(baseUri);
        if (!pathParamMatcher.find()) {
            return;
        }

        if (namedPathParams.isEmpty() && ArrayUtils.isEmpty(unnamedPathParams)) {
            throw new IllegalArgumentException("No values were found for the request's pathParams.");
        }

        final AtomicInteger nextUnnamedPathParamIndex = new AtomicInteger(0);
        final Function<String, Optional<Object>> getPathParamValueFunction = param -> {
            if (namedPathParams.containsKey(param)) {
                return Optional.of(namedPathParams.get(param));
            }

            if (unnamedPathParams.length > 0) {
                return Optional.of(unnamedPathParams[nextUnnamedPathParamIndex.getAndIncrement()]);
            }

            return Optional.empty();
        };

        final Map<String, Object> uriVariables = new HashMap<>();
        do {
            final String paramName = pathParamMatcher.group(1);
            getPathParamValueFunction.apply(paramName).ifPresent(paramValue ->
                    uriVariables.put(paramName, paramValue)
            );
        } while (pathParamMatcher.find());

        uriComponentsBuilder.uriVariables(uriVariables);
    }

    private Object applyMultiPartsAndGetRequest(
            final HttpMethod method,
            final String uri,
            final Object[] unnamedPathParams
    ) {
        if (multiParts.isEmpty()) {
            return MockMvcRequestBuilders.request(method, uri, unnamedPathParams);
        }

        if (method == POST || method == PUT || method == PATCH) {
            final Object request = isSpring6OrLater
                    ? invokeMethod(MockMvcRequestBuilders.class, "multipart", new Class[]{String.class, Object[].class}, uri, unnamedPathParams)
                    : invokeMethod(MockMvcRequestBuilders.class, "fileUpload", new Class[]{String.class, Object[].class}, uri, unnamedPathParams);
			return ReflectionUtil.invokeMethod(request, "with", new Class[] { RequestPostProcessor.class },
					new RequestPostProcessor() {
					@Override
					public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
						request.setMethod(method.name());
						return request;
					}
				});
        } else {
            throw new IllegalArgumentException("Currently multi-part file data uploading only works for POST and PUT methods");
        }
    }

    private void applyParams(
            final Object request,
            final HttpMethod method,
            final String requestContentType
    ) {
        if (!params.isEmpty()) {
            new ParamApplier(params) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
					// To support Spring Framework 7
					ReflectionUtil.invokeMethod(request, "param", new Class[] { String.class, String[].class }, paramName, paramValues);
                }
            }.applyParams();

            if (StringUtils.isBlank(requestContentType) && method == POST && !isInMultiPartMode(request)) {
                setContentTypeToApplicationFormUrlEncoded(request);
            }
        }
    }

    private void applyFormParams(
            final Object request,
            final HttpMethod method,
            final String requestContentType
    ) {
        if (!formParams.isEmpty()) {
            if (method == GET) {
                throw new IllegalArgumentException("Cannot use form parameters in a GET request");
            }
            new ParamApplier(formParams) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
					// To support Spring Framework 7
					ReflectionUtil.invokeMethod(request, "param", new Class[] { String.class, String[].class }, paramName, paramValues);
                }
            }.applyParams();

            boolean isInMultiPartMode = isInMultiPartMode(request);
            if (StringUtils.isBlank(requestContentType) && !isInMultiPartMode) {
                setContentTypeToApplicationFormUrlEncoded(request);
            }
        }
    }

    private void applyAttributes(final Object request) {
        if (!attributes.isEmpty()) {
            new ParamApplier(attributes) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
					// To support Spring Framework 7
					ReflectionUtil.invokeMethod(request, "requestAttr", new Class[] { String.class, Object.class }, paramName, paramValues[0]);
                }
            }.applyParams();
        }
    }

    private void applyHeaders(final Object request) {
        if (headers.exist()) {
            for (Header header : headers) {
				List<Object> args = new ArrayList<>();
				args.add(header.getValue());
				ReflectionUtil.invokeMethod(request, "header", new Class[] { String.class, Object[].class }, header.getName(), args.toArray());
            }
        }
    }

    private void applyCookies(final Object request) {
        if (cookies.exist()) {
            for (Cookie cookie : cookies) {
                final String cookieClassName = resolveServletCookieClassName(request);
                final Object servletCookie = invokeConstructor(cookieClassName, cookie.getName(), cookie.getValue());

                if (cookie.hasComment()) {
                    invokeMethod(servletCookie, "setComment", cookie.getComment());
                }
                if (cookie.hasDomain()) {
                    invokeMethod(servletCookie, "setDomain", cookie.getDomain());
                }
                if (cookie.hasMaxAge()) {
                    invokeMethod(servletCookie, "setMaxAge", (int) cookie.getMaxAge());
                }
                if (cookie.hasPath()) {
                    invokeMethod(servletCookie, "setPath", cookie.getPath());
                }
                if (cookie.hasVersion()) {
                    invokeMethod(servletCookie, "setVersion", cookie.getVersion());
                }
                invokeMethod(servletCookie, "setSecure", new Class[]{boolean.class}, cookie.isSecured());
                invokeMethod(request, "cookie", new Class[]{arrayNameOf(cookieClassName)}, servletCookie);
            }
        }
    }

    private String resolveServletCookieClassName(Object request) {
        final String jakartaCookieClassName = "jakarta.servlet.http.Cookie";
        final String javaxCookieClassName = "javax.servlet.http.Cookie";

        Class<?> jakartaCookieClass = loadClassIfPresent(jakartaCookieClassName);
        if (jakartaCookieClass != null && hasCookieMethod(request, jakartaCookieClass)) {
            return jakartaCookieClassName;
        }

        Class<?> javaxCookieClass = loadClassIfPresent(javaxCookieClassName);
        if (javaxCookieClass != null && hasCookieMethod(request, javaxCookieClass)) {
            return javaxCookieClassName;
        }

        if (jakartaCookieClass != null) {
            return jakartaCookieClassName;
        }
        if (javaxCookieClass != null) {
            return javaxCookieClassName;
        }

        throw new IllegalStateException("Neither jakarta.servlet.http.Cookie nor javax.servlet.http.Cookie was found in the classpath.");
    }

    private static Class<?> loadClassIfPresent(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static boolean hasCookieMethod(Object request, Class<?> cookieClass) {
        try {
            Class<?> arrayClass = java.lang.reflect.Array.newInstance(cookieClass, 0).getClass();
            request.getClass().getMethod("cookie", arrayClass);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private void applyMultipartBody(final Object request) throws IOException {
        if (!multiParts.isEmpty()) {
            for (MockMvcMultiPart multiPart : multiParts) {
                MockMultipartFile multipartFile;
                String fileName = multiPart.getFileName();
                String controlName = multiPart.getControlName();
                String mimeType = multiPart.getMimeType();
                if (multiPart.isByteArray()) {
                    multipartFile = new MockMultipartFile(controlName, fileName, mimeType, (byte[]) multiPart.getContent());
                } else if (multiPart.isFile() || multiPart.isInputStream()) {
                    InputStream inputStream;
                    if (multiPart.isFile()) {
                        inputStream = new FileInputStream((File) multiPart.getContent());
                    } else {
                        inputStream = (InputStream) multiPart.getContent();
                    }
                    multipartFile = new MockMultipartFile(controlName, fileName, mimeType, inputStream);
                } else { // String
                    multipartFile = new MockMultipartFile(controlName, fileName, mimeType, ((String) multiPart.getContent()).getBytes());
                }
				ReflectionUtil.invokeMethod(request, "file", new Class[] { MockMultipartFile.class }, multipartFile);
            }
        }
    }

    private void applyRequestBody(final Object request) {
        if (requestBody != null) {
            if (requestBody instanceof byte[]) {
				callContent(request, (byte[]) requestBody);
            } else if (requestBody instanceof File) {
                byte[] bytes = BodyHelper.toByteArray((File) requestBody);
				callContent(request, bytes);
			} else {
				// To support Spring Framework 7
				ReflectionUtil.invokeMethod(request, "content", new Class[] { String.class }, requestBody.toString());
            }
        }
    }

	private static void callContent(Object request, byte[] bytes) {
		// To support Spring Framework 7
		ReflectionUtil.invokeMethod(request, "content", new Class[] { byte[].class }, bytes);
	}

	private Class<?> arrayNameOf(String cookieClassName) {
        try {
            return Class.forName("[L" + cookieClassName + ";");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void setContentTypeToApplicationFormUrlEncoded(Object request) {
        MediaType mediaType = MediaType.parseMediaType(HeaderHelper.buildApplicationFormEncodedContentType(config, APPLICATION_FORM_URLENCODED_VALUE));
		// To support Spring Framework 7
		ReflectionUtil.invokeMethod(request, "contentType", new Class[] { MediaType.class }, mediaType);
        List<Header> newHeaders = new ArrayList<>(headers.asList());
        newHeaders.add(new Header(CONTENT_TYPE, mediaType.toString()));
        headers = new Headers(newHeaders);
    }

    private boolean isInMultiPartMode(Object request) {
        return request instanceof MockMultipartHttpServletRequestBuilder;
    }

    private void logRequestIfApplicable(HttpMethod method, String uri, String originalPath, Object[] unnamedPathParams) {
        if (requestLoggingFilter == null) {
            return;
        }

        final RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("http://localhost", RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.<Filter>emptyList(),
                null, true, ConfigConverter.convertToRestAssuredConfig(config), logRepository, null, true, true);
        logParamsAndHeaders(reqSpec, method.toString(), uri, unnamedPathParams, params, namedPathParams, queryParams, formParams, headers, cookies);
        logRequestBody(reqSpec, requestBody, headers, (List<Object>) (List<?>) multiParts, config);

        if (multiParts != null) {
            for (MockMvcMultiPart multiPart : multiParts) {
                reqSpec.multiPart(new MultiPartSpecBuilder(multiPart.getContent()).
                        controlName(multiPart.getControlName()).
                        fileName(multiPart.getFileName()).
                        mimeType(multiPart.getMimeType()).
                        build());
            }
        }

        String uriPath = PathSupport.getPath(uri);
        String originalUriPath = PathSupport.getPath(originalPath);
        requestLoggingFilter.filter(reqSpec, null, new FilterContextImpl(uri, originalUriPath, uriPath, uri, uri, new Object[0], method.toString(), null, Collections.<Filter>emptyList().iterator(), new HashMap<>()));
    }

    public MockMvcResponse get(String path, Object... pathParams) {
        return sendRequest(GET, path, pathParams);
    }

    public MockMvcResponse get(String path, Map<String, ?> pathParams) {
        return get(path, mapToArray(pathParams));
    }

    public MockMvcResponse post(String path, Object... pathParams) {
        return sendRequest(POST, path, pathParams);
    }

    public MockMvcResponse post(String path, Map<String, ?> pathParams) {
        return post(path, mapToArray(pathParams));
    }

    public MockMvcResponse put(String path, Object... pathParams) {
        return sendRequest(PUT, path, pathParams);
    }

    public MockMvcResponse put(String path, Map<String, ?> pathParams) {
        return put(path, mapToArray(pathParams));
    }

    public MockMvcResponse delete(String path, Object... pathParams) {
        return sendRequest(DELETE, path, pathParams);
    }

    public MockMvcResponse delete(String path, Map<String, ?> pathParams) {
        return delete(path, mapToArray(pathParams));
    }

    public MockMvcResponse head(String path, Object... pathParams) {
        return sendRequest(HEAD, path, pathParams);
    }

    public MockMvcResponse head(String path, Map<String, ?> pathParams) {
        return head(path, mapToArray(pathParams));
    }

    public MockMvcResponse patch(String path, Object... pathParams) {
        return sendRequest(PATCH, path, pathParams);
    }

    public MockMvcResponse patch(String path, Map<String, ?> pathParams) {
        return patch(path, mapToArray(pathParams));
    }

    public MockMvcResponse options(String path, Object... pathParams) {
        return sendRequest(OPTIONS, path, pathParams);
    }

    public MockMvcResponse options(String path, Map<String, ?> pathParams) {
        return options(path, mapToArray(pathParams));
    }

    public MockMvcResponse get(URI uri) {
        return get(uri.toString());
    }

    public MockMvcResponse post(URI uri) {
        return post(uri.toString());
    }

    public MockMvcResponse put(URI uri) {
        return put(uri.toString());
    }

    public MockMvcResponse delete(URI uri) {
        return delete(uri.toString());
    }

    public MockMvcResponse head(URI uri) {
        return head(uri.toString());
    }

    public MockMvcResponse patch(URI uri) {
        return patch(uri.toString());
    }

    public MockMvcResponse options(URI uri) {
        return options(uri.toString());
    }

    public MockMvcResponse get(URL url) {
        return get(url.toString());
    }

    public MockMvcResponse post(URL url) {
        return post(url.toString());
    }

    public MockMvcResponse put(URL url) {
        return put(url.toString());
    }

    public MockMvcResponse delete(URL url) {
        return delete(url.toString());
    }

    public MockMvcResponse head(URL url) {
        return head(url.toString());
    }

    public MockMvcResponse patch(URL url) {
        return patch(url.toString());
    }

    public MockMvcResponse options(URL url) {
        return options(url.toString());
    }

    public MockMvcResponse get() {
        return get("");
    }

    public MockMvcResponse post() {
        return post("");
    }

    public MockMvcResponse put() {
        return put("");
    }

    public MockMvcResponse delete() {
        return delete("");
    }

    public MockMvcResponse head() {
        return head("");
    }

    public MockMvcResponse patch() {
        return patch("");
    }

    public MockMvcResponse options() {
        return options("");
    }

    public MockMvcResponse request(Method method) {
        return request(method, "");
    }

    public MockMvcResponse request(String method) {
        return request(method, "");
    }

    public MockMvcResponse request(Method method, String path, Object... pathParams) {
        return request(notNull(method, Method.class).name(), path, pathParams);
    }

    public MockMvcResponse request(String method, String path, Object... pathParams) {
        return sendRequest(toValidHttpMethod(method), path, pathParams);
    }

    public MockMvcResponse request(Method method, URI uri) {
        return request(method, notNull(uri, URI.class).toString());
    }

    public MockMvcResponse request(Method method, URL url) {
        return request(method, notNull(url, URL.class).toString());
    }

    public MockMvcResponse request(String method, URI uri) {
        return request(method, notNull(uri, URI.class).toString());
    }

    public MockMvcResponse request(String method, URL url) {
        return request(method, notNull(url, URL.class).toString());
    }

    public MockMvcRequestAsyncConfigurer with() {
        return this;
    }

    public MockMvcRequestAsyncConfigurer and() {
        return this;
    }

    public MockMvcRequestAsyncConfigurer timeout(long duration, TimeUnit timeUnit) {
        RestAssuredMockMvcConfig newConfig = config.asyncConfig(new AsyncConfig(duration, timeUnit));
        return new MockMvcRequestSenderImpl(mockMvc, params, namedPathParams, queryParams, formParams,
                attributes, newConfig, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter, resultHandlers, requestPostProcessors, interceptor,
                basePath, responseSpecification, authentication, logRepository, isAsyncRequest);
    }

    public MockMvcRequestAsyncConfigurer timeout(long durationInMs) {
        return timeout(durationInMs, TimeUnit.MILLISECONDS);
    }

    public MockMvcRequestSender then() {
        return this;
    }

    public MockMvcRequestAsyncConfigurer async() {
        return new MockMvcRequestSenderImpl(mockMvc, params, namedPathParams, queryParams, formParams,
                attributes, config, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter, resultHandlers, requestPostProcessors, interceptor,
                basePath, responseSpecification, authentication, logRepository, true);
    }

    private HttpMethod toValidHttpMethod(String method) {
        String httpMethodAsString = notNull(trimToNull(method), "HTTP Method");
        try {
            return HttpMethod.valueOf(httpMethodAsString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("HTTP method '" + method + "' is not supported by MockMvc");
        }
    }
}
