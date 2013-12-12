package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

class MockMvcRequestSender implements RequestSender {
    private final MockMvc mockMvc;

    private final MultiValueMap<String, Object> params;

    MockMvcRequestSender(MockMvc mockMvc, MultiValueMap<String, Object> params) {
        this.mockMvc = mockMvc;
        this.params = params;
    }

    private String applyParamsAsQueryParameters(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append(path);
        if (!params.isEmpty()) {
            builder.append("?");
        }
        builder.append(paramsToString(params));
        return builder.toString();
    }

    private String paramsToString(MultiValueMap<String, Object> map) {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<Object>> entry : map.entrySet()) {
            List<Object> values = entry.getValue();
            for (Object value : values) {
                builder.append(entry.getKey()).append("=").append(value.toString()).append("&");
            }
        }
        builder.replace(builder.length() - 1, builder.length(), "");
        return builder.toString();
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
            response = mockMvc.perform(requestBuilder).andReturn().getResponse();
            restAssuredResponse.setConfig(new RestAssuredConfig());
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

    public Response get(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.get(applyParamsAsQueryParameters(path), pathParams));
    }


    public Response get(String path, Map<String, ?> pathParams) {
        return get(path, mapToArray(pathParams));
    }

    public Response post(String path, Object... pathParams) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(path, pathParams).contentType(APPLICATION_FORM_URLENCODED);
        for (Map.Entry<String, List<Object>> listEntry : params.entrySet()) {
            List<Object> values = listEntry.getValue();
            String[] stringValues = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                  stringValues[i] = values.get(i).toString();

            }
            builder.param(listEntry.getKey(), stringValues);
        }
        return performRequest(builder);
    }

    public Response post(String path, Map<String, ?> pathParams) {
        return post(path, mapToArray(pathParams));
    }

    public Response put(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.put(applyParamsAsQueryParameters(path), pathParams));
    }

    public Response put(String path, Map<String, ?> pathParams) {
        return put(path, mapToArray(pathParams));
    }

    public Response delete(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.delete(applyParamsAsQueryParameters(path), pathParams));
    }

    public Response delete(String path, Map<String, ?> pathParams) {
        return delete(path, mapToArray(pathParams));
    }

    public Response head(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.request(HttpMethod.HEAD, applyParamsAsQueryParameters(path), pathParams));
    }

    public Response head(String path, Map<String, ?> pathParams) {
        return head(path, mapToArray(pathParams));
    }

    public Response patch(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.request(HttpMethod.PATCH, applyParamsAsQueryParameters(path), pathParams));
    }

    public Response patch(String path, Map<String, ?> pathParams) {
        return patch(path, mapToArray(pathParams));
    }

    public Response options(String path, Object... pathParams) {
        return performRequest(MockMvcRequestBuilders.options(applyParamsAsQueryParameters(path), pathParams));
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
