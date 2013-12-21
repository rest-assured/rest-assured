package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.response.*;
import com.jayway.restassured.specification.RequestSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

class MockMvcRequestSender implements RequestSender {
    private final MockMvc mockMvc;
    private final MultiValueMap<String, Object> params;
    private final RestAssuredConfig config;
    private final Object requestBody;
    private final String requestContentType;
    private final Headers headers;
    private final Cookies cookies;

    MockMvcRequestSender(MockMvc mockMvc, MultiValueMap<String, Object> params, RestAssuredConfig config, Object requestBody,
                         String requestContentType, Headers headers, Cookies cookies) {
        this.mockMvc = mockMvc;
        this.params = params;
        this.config = config;
        this.requestBody = requestBody;
        this.requestContentType = requestContentType;
        this.headers = headers;
        this.cookies = cookies;
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
        try {
            ResultActions perform = mockMvc.perform(requestBuilder);
            MvcResult mvcResult = perform.andReturn();
            response = mvcResult.getResponse();
            restAssuredResponse.setConfig(config);
            restAssuredResponse.setContent(response.getContentAsString());
            restAssuredResponse.setContentType(response.getContentType());
            restAssuredResponse.setHasExpectations(false);
            restAssuredResponse.setStatusCode(response.getStatus());
            restAssuredResponse.setResponseHeaders(assembleHeaders(response));
            restAssuredResponse.setRpr(new ResponseParserRegistrar());
            restAssuredResponse.setStatusLine(assembleStatusLine(response));
        } catch (Exception e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
        return restAssuredResponse;
    }

    private String assembleStatusLine(MockHttpServletResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append(response.getStatus());
        if (isNotBlank(response.getErrorMessage())) {
            builder.append(" ").append(response.getErrorMessage());
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
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, path, pathParams);
        if (!params.isEmpty()) {
            for (Map.Entry<String, List<Object>> listEntry : params.entrySet()) {
                List<Object> values = listEntry.getValue();
                String[] stringValues = new String[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    stringValues[i] = values.get(i).toString();

                }
                request.param(listEntry.getKey(), stringValues);
            }

            if (method == POST) {
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

        if (requestBody != null) {
            if (requestBody instanceof byte[]) {
                request.content((byte[]) requestBody);
            } else {
                request.content(requestBody.toString());
            }
        }

        return performRequest(request);
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
