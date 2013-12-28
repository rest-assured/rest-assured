
package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.authentication.NoAuthScheme;
import com.jayway.restassured.builder.MultiPartSpecBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.internal.filter.FilterContextImpl;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc;
import com.jayway.restassured.response.*;
import com.jayway.restassured.specification.RequestSender;
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
import java.util.*;

import static com.jayway.restassured.module.mockmvc.internal.ConfigConverter.convertToRestAssuredConfig;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

class MockMvcRequestSender implements RequestSender {
    private final MockMvc mockMvc;
    private final Map<String, Object> params;
    private final Map<String, Object> queryParams;
    private final RestAssuredConfigMockMvc config;
    private final Object requestBody;
    private final String requestContentType;
    private final Headers headers;
    private final Cookies cookies;
    private final List<MockMvcMultiPart> multiParts;
    private final RequestLoggingFilter requestLoggingFilter;
    private final List<ResultHandler> resultHandlers;

    MockMvcRequestSender(MockMvc mockMvc, Map<String, Object> params, Map<String, Object> queryParams, RestAssuredConfigMockMvc config, Object requestBody,
                         String requestContentType, Headers headers, Cookies cookies, List<MockMvcMultiPart> multiParts,
                         RequestLoggingFilter requestLoggingFilter, List<ResultHandler> resultHandlers) {
        this.mockMvc = mockMvc;
        this.params = params;
        this.queryParams = queryParams;
        this.config = config;
        this.requestBody = requestBody;
        this.requestContentType = requestContentType;
        this.headers = headers;
        this.cookies = cookies;
        this.multiParts = multiParts;
        this.requestLoggingFilter = requestLoggingFilter;
        this.resultHandlers = resultHandlers;
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

    private Response performRequest(MockHttpServletRequestBuilder requestBuilder) {
        MockHttpServletResponse response;
        RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();
        if (mockMvc == null) {
            throw new IllegalStateException("You haven't configured a MockMVC instance. You can do this statically\n\nRestAssured.mockMvc = ..\nRestAssured.standaloneSetup(..);\nRestAssured.webAppContextSetup(..);\n\nor using the DSL:\n\ngiven().\n\t\tmockMvc(..). ..\n");
        }
        try {
            ResultActions perform = mockMvc.perform(requestBuilder);
            if (!resultHandlers.isEmpty()) {
                for (ResultHandler resultHandler : resultHandlers) {
                    perform.andDo(resultHandler);
                }
            }
            MvcResult mvcResult = perform.andReturn();
            response = mvcResult.getResponse();
            restAssuredResponse.setConfig(convertToRestAssuredConfig(config));
            restAssuredResponse.setContent(response.getContentAsString());
            restAssuredResponse.setContentType(response.getContentType());
            restAssuredResponse.setHasExpectations(false);
            restAssuredResponse.setStatusCode(response.getStatus());
            restAssuredResponse.setResponseHeaders(assembleHeaders(response));
            restAssuredResponse.setRpr(new ResponseParserRegistrar());
            restAssuredResponse.setStatusLine(assembleStatusLine(response, mvcResult.getResolvedException()));
        } catch (Exception e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
        return restAssuredResponse;
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

    private Response sendRequest(HttpMethod method, String path, Object[] pathParams) {
        if (requestBody != null && !multiParts.isEmpty()) {
            throw new IllegalStateException("You cannot specify a request body and a multi-part body in the same request. Perhaps you want to change the body to a multi part?");
        }

        MockHttpServletRequestBuilder request;
        if (multiParts.isEmpty()) {
            request = MockMvcRequestBuilders.request(method, path, pathParams);
        } else if (method != POST) {
            throw new IllegalArgumentException("Currently multi-part file data uploading only works for " + POST);
        } else {
            request = MockMvcRequestBuilders.fileUpload(path, pathParams);
        }

        if (!params.isEmpty()) {
            for (Map.Entry<String, Object> listEntry : params.entrySet()) {
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
                request.param(listEntry.getKey(), stringValues);
            }

            boolean isInMultiPartMode = request instanceof MockMultipartHttpServletRequestBuilder;
            if (method == POST && !isInMultiPartMode) {
                request.contentType(APPLICATION_FORM_URLENCODED);
            }
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

    private void logRequestIfApplicable(HttpMethod method, String path, Object[] pathParams) {
        if (requestLoggingFilter == null) {
            return;
        }

        RequestSpecificationImpl reqSpec = new RequestSpecificationImpl("", 8080, path, new NoAuthScheme(), Collections.<Filter>emptyList(), requestContentType, null, true, convertToRestAssuredConfig(config));
        if (params != null) {
            for (Map.Entry<String, Object> stringListEntry : params.entrySet()) {
                Object value = stringListEntry.getValue();
                Collection<Object> values;
                if (value instanceof Collection) {
                    values = (Collection<Object>) value;
                } else {
                    values = new ArrayList<Object>();
                    values.add(value);
                }

                for (Object theValue : values) {
                    reqSpec.param(stringListEntry.getKey(), theValue);
                }
            }
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

    public Response get(String path, Object... pathParams) {
        return sendRequest(GET, path, pathParams);
    }

    public Response get(String path, Map<String, ?> pathParams) {
        return get(path, mapToArray(pathParams));
    }

    public Response post(String path, Object... pathParams) {
        return sendRequest(POST, path, pathParams);
    }

    public Response post(String path, Map<String, ?> pathParams) {
        return post(path, mapToArray(pathParams));
    }

    public Response put(String path, Object... pathParams) {
        return sendRequest(PUT, path, pathParams);
    }

    public Response put(String path, Map<String, ?> pathParams) {
        return put(path, mapToArray(pathParams));
    }

    public Response delete(String path, Object... pathParams) {
        return sendRequest(DELETE, path, pathParams);
    }

    public Response delete(String path, Map<String, ?> pathParams) {
        return delete(path, mapToArray(pathParams));
    }

    public Response head(String path, Object... pathParams) {
        return sendRequest(HEAD, path, pathParams);
    }

    public Response head(String path, Map<String, ?> pathParams) {
        return head(path, mapToArray(pathParams));
    }

    public Response patch(String path, Object... pathParams) {
        return sendRequest(PATCH, path, pathParams);
    }

    public Response patch(String path, Map<String, ?> pathParams) {
        return patch(path, mapToArray(pathParams));
    }

    public Response options(String path, Object... pathParams) {
        return sendRequest(OPTIONS, path, pathParams);
    }

    public Response options(String path, Map<String, ?> pathParams) {
        return options(path, mapToArray(pathParams));
    }

    public Response get(URI uri) {
        return get(uri.toString());
    }

    public Response post(URI uri) {
        return post(uri.toString());
    }

    public Response put(URI uri) {
        return put(uri.toString());
    }

    public Response delete(URI uri) {
        return delete(uri.toString());
    }

    public Response head(URI uri) {
        return head(uri.toString());
    }

    public Response patch(URI uri) {
        return patch(uri.toString());
    }

    public Response options(URI uri) {
        return options(uri.toString());
    }

    public Response get(URL url) {
        return get(url.toString());
    }

    public Response post(URL url) {
        return post(url.toString());
    }

    public Response put(URL url) {
        return put(url.toString());
    }

    public Response delete(URL url) {
        return delete(url.toString());
    }

    public Response head(URL url) {
        return head(url.toString());
    }

    public Response patch(URL url) {
        return patch(url.toString());
    }

    public Response options(URL url) {
        return options(url.toString());
    }

    public Response get() {
        return get("");
    }

    public Response post() {
        return post("");
    }

    public Response put() {
        return put("");
    }

    public Response delete() {
        return delete("");
    }

    public Response head() {
        return head("");
    }

    public Response patch() {
        return patch("");
    }

    public Response options() {
        return options("");
    }
}
