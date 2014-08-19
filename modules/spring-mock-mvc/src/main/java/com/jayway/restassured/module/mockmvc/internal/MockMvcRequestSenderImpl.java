
package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.authentication.NoAuthScheme;
import com.jayway.restassured.builder.MultiPartSpecBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.filter.FilterContextImpl;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.internal.log.LogRepository;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import com.jayway.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSender;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.ResponseSpecification;
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

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.*;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static com.jayway.restassured.module.mockmvc.internal.ConfigConverter.convertToRestAssuredConfig;
import static com.jayway.restassured.module.mockmvc.internal.SpringSecurityClassPathChecker.isSpringSecurityInClasspath;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

class MockMvcRequestSenderImpl implements MockMvcRequestSender {
    private final MockMvc mockMvc;
    private final Map<String, Object> params;
    private final Map<String, Object> queryParams;
    private final Map<String, Object> formParams;
    private final Map<String, Object> attributes;
    private final RestAssuredMockMvcConfig config;
    private final Object requestBody;
    private final String requestContentType;
    private final Headers headers;
    private final Cookies cookies;
    private final List<MockMvcMultiPart> multiParts;
    private final RequestLoggingFilter requestLoggingFilter;
    private final List<ResultHandler> resultHandlers;
    private final MockHttpServletRequestBuilderInterceptor interceptor;
    private final String basePath;
    private final ResponseSpecification responseSpecification;
    private final Object authentication;
    private final LogRepository logRepository;

    MockMvcRequestSenderImpl(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> queryParams, Map<String, Object> formParams, Map<String, Object> attributes,
                             RestAssuredMockMvcConfig config, Object requestBody, String requestContentType, Headers headers, Cookies cookies,
                             List<MockMvcMultiPart> multiParts, RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers,
                             MockHttpServletRequestBuilderInterceptor interceptor, String basePath, ResponseSpecification responseSpecification,
                             Object authentication, LogRepository logRepository) {
        this.mockMvc = mockMvc;
        this.params = params;
        this.queryParams = queryParams;
        this.formParams = formParams;
        this.attributes = attributes;
        this.config = config;
        this.requestBody = requestBody;
        this.requestContentType = requestContentType;
        this.headers = headers;
        this.cookies = cookies;
        this.multiParts = multiParts;
        this.requestLoggingFilter = requestLoggingFilter;
        this.resultHandlers = resultHandlers;
        this.interceptor = interceptor;
        this.basePath = basePath;
        this.responseSpecification = responseSpecification;
        this.authentication = authentication;
        this.logRepository = logRepository;
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

    private MockMvcResponse performRequest(MockHttpServletRequestBuilder requestBuilder) {
        MockHttpServletResponse response;
        if (mockMvc == null) {
            throw new IllegalStateException("You haven't configured a MockMVC instance. You can do this statically\n\nRestAssured.mockMvc = ..\nRestAssured.standaloneSetup(..);\nRestAssured.webAppContextSetup(..);\n\nor using the DSL:\n\ngiven().\n\t\tmockMvc(..). ..\n");
        }

        if (interceptor != null) {
            interceptor.intercept(requestBuilder);
        }

        if (isSpringSecurityInClasspath() && authentication instanceof org.springframework.security.core.Authentication) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) authentication);
        } else if (authentication instanceof Principal) {
            requestBuilder.principal((Principal) authentication);
        }

        MockMvcRestAssuredResponseImpl restAssuredResponse;
        try {
            ResultActions perform = mockMvc.perform(requestBuilder);
            if (!resultHandlers.isEmpty()) {
                for (ResultHandler resultHandler : resultHandlers) {
                    perform.andDo(resultHandler);
                }
            }
            MvcResult mvcResult = perform.andReturn();
            response = mvcResult.getResponse();
            restAssuredResponse = new MockMvcRestAssuredResponseImpl(perform, logRepository);
            restAssuredResponse.setConfig(convertToRestAssuredConfig(config));
            restAssuredResponse.setContent(response.getContentAsString());
            restAssuredResponse.setContentType(response.getContentType());
            restAssuredResponse.setHasExpectations(false);
            restAssuredResponse.setStatusCode(response.getStatus());
            restAssuredResponse.setResponseHeaders(assembleHeaders(response));
            restAssuredResponse.setRpr(getRpr());
            restAssuredResponse.setStatusLine(assembleStatusLine(response, mvcResult.getResolvedException()));

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

    private ResponseParserRegistrar getRpr() {
        if (responseSpecification != null && responseSpecification instanceof ResponseSpecificationImpl) {
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

    private Object[] mapToArray(Map<String, ?> map) {
        if (map == null) {
            return new Object[0];
        }
        return map.values().toArray(new Object[map.values().size()]);
    }

    private MockMvcResponse sendRequest(HttpMethod method, String path, Object[] pathParams) {
        notNull(path, "Path");
        if (requestBody != null && !multiParts.isEmpty()) {
            throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request. Perhaps you want to change the body to a multi part?");
        }

        if (isNotBlank(basePath)) {
            path = mergeAndRemoveDoubleSlash(basePath, path);
        }

        final MockHttpServletRequestBuilder request;
        if (multiParts.isEmpty()) {
            request = MockMvcRequestBuilders.request(method, path, pathParams);
        } else if (method != POST) {
            throw new IllegalArgumentException("Currently multi-part file data uploading only works for " + POST);
        } else {
            request = MockMvcRequestBuilders.fileUpload(path, pathParams);
        }

        if (!params.isEmpty()) {
            new ParamApplier(params) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    request.param(paramName, paramValues);
                }
            }.applyParams();

            if (method == POST && !isInMultiPartMode(request)) {
                request.contentType(APPLICATION_FORM_URLENCODED);
            }
        }

        if (!queryParams.isEmpty()) {
            new ParamApplier(queryParams) {
                @Override
                protected void applyParam(String paramName, String[] paramValues) {
                    // Spring MVC cannot distinguish query from params afaik.
                    request.param(paramName, paramValues);
                }
            }.applyParams();
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
            if (!isInMultiPartMode) {
                request.contentType(APPLICATION_FORM_URLENCODED);
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

        if (StringUtils.isNotBlank(requestContentType)) {
            request.contentType(MediaType.parseMediaType(requestContentType));
        }

        if (headers.exist()) {
            for (Header header : headers) {
                request.header(header.getName(), header.getValue());
            }
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
                request.cookie(servletCookie);
            }
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
            } else {
                request.content(requestBody.toString());
            }
        }

        logRequestIfApplicable(method, path, pathParams);

        return performRequest(request);
    }

    private boolean isInMultiPartMode(MockHttpServletRequestBuilder request) {
        return request instanceof MockMultipartHttpServletRequestBuilder;
    }

    private void logRequestIfApplicable(HttpMethod method, String path, Object[] pathParams) {
        if (requestLoggingFilter == null) {
            return;
        }

        final RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("", 8080, path, new NoAuthScheme(), Collections.<Filter>emptyList(),
                requestContentType, null, true, convertToRestAssuredConfig(config), logRepository, null);
        if (params != null) {
            new ParamLogger(params) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.param(paramName, paramValue);
                }
            }.logParams();
        }

        if (queryParams != null) {
            new ParamLogger(queryParams) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.queryParam(paramName, paramValue);
                }
            }.logParams();
        }

        if (formParams != null) {
            new ParamLogger(formParams) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.formParam(paramName, paramValue);
                }
            }.logParams();
        }

        if (headers != null) {
            for (Header header : headers) {
                reqSpec.header(header);
            }
        }

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                reqSpec.cookie(cookie);
            }
        }

        if (requestBody != null) {
            if (requestBody instanceof byte[]) {
                reqSpec.body((byte[]) requestBody);
            } else {
                reqSpec.body(requestBody);
            }
        }

        if (multiParts != null) {
            for (MockMvcMultiPart multiPart : multiParts) {
                reqSpec.multiPart(new MultiPartSpecBuilder(multiPart.getContent()).
                        controlName(multiPart.getControlName()).
                        fileName(multiPart.getFileName()).
                        mimeType(multiPart.getMimeType()).
                        build());
            }
        }

        requestLoggingFilter.filter(reqSpec, null, new FilterContextImpl(path, path, Method.valueOf(method.toString()), null, Collections.<Filter>emptyList()));
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

    private abstract static class ParamApplier {
        private Map<String, Object> map;

        protected ParamApplier(Map<String, Object> parameters) {
            this.map = parameters;
        }

        public void applyParams() {
            for (Map.Entry<String, Object> listEntry : map.entrySet()) {
                Object value = listEntry.getValue();
                String[] stringValues;
                if (value instanceof Collection) {
                    Collection col = (Collection) value;
                    stringValues = new String[col.size()];
                    int index = 0;
                    for (Object val : col) {
                        stringValues[index] = val == null ? null : val.toString();
                        index++;
                    }
                } else {
                    stringValues = new String[1];
                    stringValues[0] = value == null ? null : value.toString();
                }
                applyParam(listEntry.getKey(), stringValues);
            }
        }

        protected abstract void applyParam(String paramName, String[] paramValues);
    }

    private abstract static class ParamLogger {
        private Map<String, Object> map;

        protected ParamLogger(Map<String, Object> parameters) {
            this.map = parameters;
        }

        public void logParams() {
            for (Map.Entry<String, Object> stringListEntry : map.entrySet()) {
                Object value = stringListEntry.getValue();
                Collection<Object> values;
                if (value instanceof Collection) {
                    values = (Collection<Object>) value;
                } else {
                    values = new ArrayList<Object>();
                    values.add(value);
                }

                for (Object theValue : values) {
                    logParam(stringListEntry.getKey(), theValue);
                }
            }
        }

        protected abstract void logParam(String paramName, Object paramValue);
    }
}
