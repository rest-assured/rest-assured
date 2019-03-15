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
import io.restassured.module.spring.commons.BodyHelper;
import io.restassured.module.spring.commons.HeaderHelper;
import io.restassured.module.spring.commons.ParamApplier;
import io.restassured.module.spring.commons.config.AsyncConfig;
import io.restassured.module.spring.commons.config.ConfigConverter;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static io.restassured.module.mockmvc.internal.SpringSecurityClassPathChecker.isSpringSecurityInClasspath;
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
    private static final String CHARSET = "charset";

    private final MockMvc mockMvc;
    private final Map<String, Object> params;
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

    MockMvcRequestSenderImpl(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
                             RestAssuredMockMvcConfig config, Object requestBody, Headers headers, Cookies cookies, Map<String, Object> sessionAttributes,
                             List<MockMvcMultiPart> multiParts, RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers,
                             List<RequestPostProcessor> requestPostProcessors, MockHttpServletRequestBuilderInterceptor interceptor, String basePath, ResponseSpecification responseSpecification,
                             Object authentication, LogRepository logRepository) {
        this(mockMvc, params, queryParams, formParams, attributes, config, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter, resultHandlers, requestPostProcessors, interceptor,
                basePath, responseSpecification, authentication, logRepository, false);
    }

    private MockMvcRequestSenderImpl(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
                                     RestAssuredMockMvcConfig config, Object requestBody, Headers headers, Cookies cookies, Map<String, Object> sessionAttributes,
                                     List<MockMvcMultiPart> multiParts, RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers,
                                     List<RequestPostProcessor> requestPostProcessors, MockHttpServletRequestBuilderInterceptor interceptor, String basePath, ResponseSpecification responseSpecification,
                                     Object authentication, LogRepository logRepository, boolean isAsyncRequest) {
        this.mockMvc = mockMvc;
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

        List<Header> headers = new ArrayList<Header>();
        for (String headerName : headerNames) {
            List<String> headerValues = response.getHeaders(headerName);
            for (String headerValue : headerValues) {
                headers.add(new Header(headerName, headerValue));
            }
        }
        return new Headers(headers);
    }

    private Cookies convertCookies(javax.servlet.http.Cookie[] servletCookies) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        for (javax.servlet.http.Cookie servletCookie : servletCookies) {
            Cookie.Builder cookieBuilder = new Cookie.Builder(servletCookie.getName(), servletCookie.getValue());
            if (servletCookie.getComment() != null) {
                cookieBuilder.setComment(servletCookie.getComment());
            }
            if (servletCookie.getDomain() != null) {
                cookieBuilder.setDomain(servletCookie.getDomain());
            }
            if (servletCookie.getPath() != null) {
                cookieBuilder.setPath(servletCookie.getPath());
            }
            cookieBuilder.setMaxAge(servletCookie.getMaxAge());
            cookieBuilder.setVersion(servletCookie.getVersion());
            cookieBuilder.setSecured(servletCookie.getSecure());
            cookies.add(cookieBuilder.build());
        }
        return new Cookies(cookies);
    }

    @SuppressWarnings("unchecked")
    private MockMvcResponse performRequest(MockHttpServletRequestBuilder requestBuilder) {
        MockHttpServletResponse response;

        if (interceptor != null) {
            interceptor.intercept(requestBuilder);
        }

        if (isSpringSecurityInClasspath() && authentication instanceof org.springframework.security.core.Authentication) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) authentication);
        }
        if (authentication instanceof Principal) {
            requestBuilder.principal((Principal) authentication);
        }

        for (RequestPostProcessor requestPostProcessor : requestPostProcessors) {
            requestBuilder.with(requestPostProcessor);
        }

        MockMvcRestAssuredResponseImpl restAssuredResponse;
        try {
            final long start = System.currentTimeMillis();
            ResultActions perform = mockMvc.perform(requestBuilder);
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
            restAssuredResponse.setCookies(convertCookies(response.getCookies()));

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

    private MockMvcResponse sendRequest(HttpMethod method, String path, Object[] pathParams) {
        notNull(path, "Path");
        if (requestBody != null && !multiParts.isEmpty()) {
            throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request. Perhaps you want to change the body to a multi part?");
        }

        String baseUri;
        if (isNotBlank(basePath)) {
            baseUri = mergeAndRemoveDoubleSlash(basePath, path);
        } else {
            baseUri = path;
        }

        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUri);
        if (!queryParams.isEmpty()) {
            new ParamApplier(queryParams) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    uriComponentsBuilder.queryParam(paramName, paramValues);
                }
            }.applyParams();
        }
        String uri = uriComponentsBuilder.build().toUriString();

        final MockHttpServletRequestBuilder request;
        if (multiParts.isEmpty()) {
            request = MockMvcRequestBuilders.request(method, uri, pathParams);
        } else if (method != POST) {
            throw new IllegalArgumentException("Currently multi-part file data uploading only works for " + POST);
        } else {
            request = MockMvcRequestBuilders.fileUpload(uri, pathParams);
        }

        String requestContentType = HeaderHelper.findContentType(headers, (List<Object>) (List<?>) multiParts, config);

        if (!params.isEmpty()) {
            new ParamApplier(params) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    request.param(paramName, paramValues);
                }
            }.applyParams();

            if (StringUtils.isBlank(requestContentType) && method == POST && !isInMultiPartMode(request)) {
                setContentTypeToApplicationFormUrlEncoded(request);
            }
        }

        if (!formParams.isEmpty()) {
            if (method == GET) {
                throw new IllegalArgumentException("Cannot use form parameters in a GET request");
            }
            new ParamApplier(formParams) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    request.param(paramName, paramValues);
                }
            }.applyParams();

            boolean isInMultiPartMode = isInMultiPartMode(request);
            if (StringUtils.isBlank(requestContentType) && !isInMultiPartMode) {
                setContentTypeToApplicationFormUrlEncoded(request);
            }
        }

        if (!attributes.isEmpty()) {
            new ParamApplier(attributes) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    request.requestAttr(paramName, paramValues[0]);
                }
            }.applyParams();
        }


        if (RestDocsClassPathChecker.isSpringRestDocsInClasspath() && config.getMockMvcConfig().shouldAutomaticallyApplySpringRestDocsMockMvcSupport()) {
            request.requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, PathSupport.getPath(uri));
        }

        if (headers.exist()) {
            for (Header header : headers) {
                request.header(header.getName(), header.getValue());
            }
        }

        if (StringUtils.isNotBlank(requestContentType)) {
            request.contentType(MediaType.parseMediaType(requestContentType));
        }

        if (cookies.exist()) {
            for (Cookie cookie : cookies) {
                javax.servlet.http.Cookie servletCookie = new javax.servlet.http.Cookie(cookie.getName(), cookie.getValue());
                if (cookie.hasComment()) {
                    servletCookie.setComment(cookie.getComment());
                }
                if (cookie.hasDomain()) {
                    servletCookie.setDomain(cookie.getDomain());
                }
                if (cookie.hasMaxAge()) {
                    servletCookie.setMaxAge(cookie.getMaxAge());
                }
                if (cookie.hasPath()) {
                    servletCookie.setPath(cookie.getPath());
                }
                if (cookie.hasVersion()) {
                    servletCookie.setVersion(cookie.getVersion());
                }
                servletCookie.setSecure(cookie.isSecured());
                request.cookie(servletCookie);
            }
        }

        if (!sessionAttributes.isEmpty()) {
            request.sessionAttrs(sessionAttributes);
        }

        if (!multiParts.isEmpty()) {
            MockMultipartHttpServletRequestBuilder multiPartRequest = (MockMultipartHttpServletRequestBuilder) request;
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
                        try {
                            inputStream = new FileInputStream((File) multiPart.getContent());
                        } catch (FileNotFoundException e) {
                            return SafeExceptionRethrower.safeRethrow(e);
                        }
                    } else {
                        inputStream = (InputStream) multiPart.getContent();
                    }
                    try {
                        multipartFile = new MockMultipartFile(controlName, fileName, mimeType, inputStream);
                    } catch (IOException e) {
                        return SafeExceptionRethrower.safeRethrow(e);
                    }
                } else { // String
                    multipartFile = new MockMultipartFile(controlName, fileName, mimeType, ((String) multiPart.getContent()).getBytes());
                }
                multiPartRequest.file(multipartFile);
            }
        }

        if (requestBody != null) {
            if (requestBody instanceof byte[]) {
                request.content((byte[]) requestBody);
            } else if (requestBody instanceof File) {
                byte[] bytes = BodyHelper.toByteArray((File) requestBody);
                request.content(bytes);
            } else {
                request.content(requestBody.toString());
            }
        }

        logRequestIfApplicable(method, baseUri, path, pathParams);

        return performRequest(request);
    }

    private void setContentTypeToApplicationFormUrlEncoded(MockHttpServletRequestBuilder request) {
        MediaType mediaType = MediaType.parseMediaType(HeaderHelper.buildApplicationFormEncodedContentType(config, APPLICATION_FORM_URLENCODED_VALUE));
        request.contentType(mediaType);
        List<Header> newHeaders = new ArrayList<Header>(headers.asList());
        newHeaders.add(new Header(CONTENT_TYPE, mediaType.toString()));
        headers = new Headers(newHeaders);
    }

    private boolean isInMultiPartMode(MockHttpServletRequestBuilder request) {
        return request instanceof MockMultipartHttpServletRequestBuilder;
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
        requestLoggingFilter.filter(reqSpec, null, new FilterContextImpl(uri, originalUriPath, uriPath, uri, uri, new Object[0], method.toString(), null, Collections.<Filter>emptyList().iterator(), new HashMap<String, Object>()));
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
        return new MockMvcRequestSenderImpl(mockMvc, params, queryParams, formParams,
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
        return new MockMvcRequestSenderImpl(mockMvc, params, queryParams, formParams,
                attributes, config, requestBody, headers, cookies, sessionAttributes, multiParts, requestLoggingFilter, resultHandlers, requestPostProcessors, interceptor,
                basePath, responseSpecification, authentication, logRepository, true);
    }

    private HttpMethod toValidHttpMethod(String method) {
        String httpMethodAsString = notNull(trimToNull(method), "HTTP Method");
        HttpMethod httpMethod = HttpMethod.resolve(httpMethodAsString.toUpperCase());
        if (httpMethod == null) {
            throw new IllegalArgumentException("HTTP method '" + method + "' is not supported by MockMvc");
        }
        return httpMethod;
    }
}
