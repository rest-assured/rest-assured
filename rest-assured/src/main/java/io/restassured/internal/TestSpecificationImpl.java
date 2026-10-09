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


package io.restassured.internal;

import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * A test io.restassured.specification contains a {@link ResponseSpecification} and a {@link RequestSpecification}. It's
 * mainly used when you have long specifications, e.g.
 * <pre>
 * RequestSpecification requestSpecification = with().parameters("firstName", "John", "lastName", "Doe");
 * ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
 *  given(requestSpecification, responseSpecification).get("/greet");
 * </pre>
 * <p>
 * This will perform a GET request to "/greet" and verify it according to the <code>responseSpecification</code>.
 */
public class TestSpecificationImpl implements RequestSender {
    private final RequestSpecification requestSpecification;
    private final ResponseSpecification responseSpecification;

    public TestSpecificationImpl(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        notNull(requestSpecification, "requestSpecification");
        notNull(responseSpecification, "responseSpecification");

        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
        ((ResponseSpecificationImpl) responseSpecification).setRequestSpecification(requestSpecification);
        ((RequestSpecificationImpl) requestSpecification).setResponseSpecification(responseSpecification);
    }

    /**
     * {@inheritDoc}
     */
    public Response get(String path, Object... pathParams) {
        return requestSpecification.get(path, pathParams);
    }

    /**
     * {@inheritDoc}
     */
    public Response post(String path, Object... pathParams) {
        return requestSpecification.post(path, pathParams);
    }

    /**
     * {@inheritDoc}
     */
    public Response put(String path, Object... pathParams) {
        return requestSpecification.put(path, pathParams);
    }

    /**
     * {@inheritDoc}
     */
    public Response delete(String path, Object... pathParams) {
        return requestSpecification.delete(path, pathParams);
    }

    /**
     * {@inheritDoc}
     */
    public Response head(String path, Object... pathParams) {
        return requestSpecification.head(path, pathParams);
    }

    /**
     * {@inheritDoc}
     */
    public Response patch(String path, Object... pathParams) {
        return requestSpecification.patch(path, pathParams);
    }

    public Response options(String path, Object... pathParams) {
        return requestSpecification.options(path, pathParams);
    }

    public Response query(String path, Object... pathParams) {
        return requestSpecification.query(path, pathParams);
    }

    public Response get(URI uri) {
        return get(notNull(uri, "URI").toString());
    }

    public Response post(URI uri) {
        return post(notNull(uri, "URI").toString());
    }

    public Response put(URI uri) {
        return put(notNull(uri, "URI").toString());
    }

    public Response delete(URI uri) {
        return delete(notNull(uri, "URI").toString());
    }

    public Response head(URI uri) {
        return head(notNull(uri, "URI").toString());
    }

    public Response patch(URI uri) {
        return patch(notNull(uri, "URI").toString());
    }

    public Response options(URI uri) {
        return options(notNull(uri, "URI").toString());
    }

    public Response query(URI uri) {
        return query(notNull(uri, "URI").toString());
    }

    public Response get(URL url) {
        return get(notNull(url, "URL").toString());
    }

    public Response post(URL url) {
        return post(notNull(url, "URL").toString());
    }

    public Response put(URL url) {
        return put(notNull(url, "URL").toString());
    }

    public Response delete(URL url) {
        return delete(notNull(url, "URL").toString());
    }

    public Response head(URL url) {
        return head(notNull(url, "URL").toString());
    }

    public Response patch(URL url) {
        return patch(notNull(url, "URL").toString());
    }

    public Response options(URL url) {
        return options(notNull(url, "URL").toString());
    }

    public Response query(URL url) {
        return query(notNull(url, "URL").toString());
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

    public Response query() {
        return query("");
    }

    public Response request(Method method) {
        return request(notNull(method, Method.class).toString());
    }

    public Response request(String method) {
        return request(method, "");
    }

    public Response request(Method method, String path, Object... pathParams) {
        return request(notNull(method, Method.class).toString(), path, pathParams);
    }

    public Response request(String method, String path, Object... pathParams) {
        return requestSpecification.request(method, path, pathParams);
    }

    public Response request(Method method, URI uri) {
        return requestSpecification.request(method, uri);
    }

    public Response request(Method method, URL url) {
        return requestSpecification.request(method, url);
    }

    public Response request(String method, URI uri) {
        return requestSpecification.request(method, uri);
    }

    public Response request(String method, URL url) {
        return requestSpecification.request(method, url);
    }

    public Response get(String path, Map<String, ?> pathParams) {
        return requestSpecification.get(path, pathParams);
    }

    public Response post(String path, Map<String, ?> pathParams) {
        return requestSpecification.post(path, pathParams);
    }

    public Response put(String path, Map<String, ?> pathParams) {
        return requestSpecification.put(path, pathParams);
    }

    public Response delete(String path, Map<String, ?> pathParams) {
        return requestSpecification.delete(path, pathParams);
    }

    public Response head(String path, Map<String, ?> pathParams) {
        return requestSpecification.head(path, pathParams);
    }

    public Response patch(String path, Map<String, ?> pathParams) {
        return requestSpecification.patch(path, pathParams);
    }

    public Response options(String path, Map<String, ?> pathParams) {
        return requestSpecification.options(path, pathParams);
    }

    public Response query(String path, Map<String, ?> pathParams) {
        return requestSpecification.query(path, pathParams);
    }

    public RequestSpecification getRequestSpecification() {
        return requestSpecification;
    }

    public ResponseSpecification getResponseSpecification() {
        return responseSpecification;
    }
}
