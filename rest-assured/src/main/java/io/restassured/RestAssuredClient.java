/*
 * Copyright 2026 the original author or authors.
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

package io.restassured;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Method;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.TestSpecificationImpl;
import io.restassured.internal.UriValidator;
import io.restassured.internal.client.ClientContext;
import io.restassured.internal.client.ClientDefaults;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.http.client.HttpClientInstances;
import io.restassured.internal.support.CommonVersionCheck;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Sends requests like the static API of {@link RestAssured}, with defaults of its own. A client never reads the static
 * fields of <code>RestAssured</code>, so tests that run in parallel can each use their own base URI, port, config,
 * filters and specifications.
 * <h2>Creating a client</h2>
 * <pre>
 * RestAssuredClient api = RestAssuredClient.builder()
 *         .baseUri("http://localhost").port(8081).basePath("/api")
 *         .authentication(basic("user", "secret"))
 *         .filters(new RequestLoggingFilter())
 *         .build();
 * </pre>
 * The {@link Builder} starts from the values that {@link RestAssured#reset()} sets. It has a method for each static
 * field and static setter of <code>RestAssured</code> except <code>reset()</code>, without a <code>set</code> prefix.
 * {@link RestAssured#client()} creates a client from the current values of the static fields instead.
 * <h2>Sending requests</h2>
 * A client has the methods of <code>RestAssured</code> that start a request, and works like them:
 * <pre>
 * api.given().queryParam("status", "open").when().get("/orders").then().statusCode(200);
 * api.get("/orders/{id}", 42).then().body("id", equalTo(42));
 * </pre>
 * The authentication schemes, such as {@link RestAssured#basic(String, String)}, and {@link RestAssured#withArgs(Object, Object...)}
 * stay static on <code>RestAssured</code>.
 * <h2>Changing a client</h2>
 * A client can't be changed once it's built. {@link #toBuilder()} gives a builder with its values, which builds a new,
 * independent client:
 * <pre>
 * RestAssuredClient admin = api.toBuilder().authentication(basic("admin", "secret")).build();
 * </pre>
 * <h2>Closing a client</h2>
 * A client owns the HTTP clients that send its requests, and the cookies it keeps with
 * <code>httpClientConfig().reuseHttpClientInstance().keepResponseCookies(true)</code>. With http-client-apache5, it keeps one
 * HTTP client, and so one connection pool, for each distinct setting across its requests. {@link #close()} releases them:
 * <pre>
 * try (RestAssuredClient client = RestAssured.client().toBuilder().port(8081).build()) {
 *     client.get("/health").then().statusCode(200);
 * }
 * </pre>
 * or with JUnit 5:
 * <pre>
 * static RestAssuredClient api;
 *
 * &#64;BeforeAll static void createClient() { api = RestAssuredClient.builder().port(8081).build(); }
 * &#64;AfterAll static void closeClient() { api.close(); }
 * </pre>
 * After <code>close()</code>, the methods that start a request, and sending a specification that the client created
 * before, throw an {@link IllegalStateException}. Requests in flight complete, and responses already received stay
 * readable. A client that is never closed leaves its HTTP clients to the garbage collector.
 * <h2>Thread safety</h2>
 * <ul>
 * <li>A client can be used from many threads at once, including {@link #close()}.</li>
 * <li>{@link #given()} returns a new specification each time. Don't change one specification from several threads at
 * once. Sending one specification, or one built with {@link io.restassured.builder.RequestSpecBuilder}, from many
 * threads is safe.</li>
 * <li>The filters of the client, including those of its default request specification, are used by all its requests
 * and must be thread safe.</li>
 * <li>The cookies that a client keeps with <code>reuseHttpClientInstance().keepResponseCookies(true)</code> are shared by its
 * requests from all threads.</li>
 * </ul>
 * <h2>Static settings that still apply</h2>
 * A specification built with {@link io.restassured.builder.RequestSpecBuilder} or
 * {@link io.restassured.builder.ResponseSpecBuilder} contains static settings of <code>RestAssured</code> from when the
 * builder was created, such as the filters, the config, the request specification, the root path and the default
 * parser, which then apply to the requests of the client it's used with. The extra requests of form authentication and CSRF are sent with the
 * static settings.
 */
public final class RestAssuredClient implements AutoCloseable {

    static {
        // First, so that an older rest-assured-common gives a message instead of a NoClassDefFoundError
        CommonVersionCheck.verify();
    }

    private final Supplier<ClientDefaults> defaults;
    private final ClientContext context;

    // The default client of the static API reads the static fields at each call
    RestAssuredClient(Supplier<ClientDefaults> defaults, ClientContext context) {
        this.defaults = defaults;
        this.context = context;
    }

    /**
     * @return A builder that starts from the values that {@link RestAssured#reset()} sets
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A builder with the values of this client. The client it builds is independent of this one, and isn't
     * closed when this one is.
     */
    public Builder toBuilder() {
        return new Builder(defaults.get());
    }

    /**
     * Starts a request, like {@link RestAssured#given()}. Each call returns a new specification.
     *
     * @return A request specification with the defaults of this client
     */
    public RequestSpecification given() {
        return testSpecification().getRequestSpecification();
    }

    /**
     * Starts a request, like {@link RestAssured#with()}, the same as {@link #given()}.
     *
     * @return A request specification with the defaults of this client
     */
    public RequestSpecification with() {
        return given();
    }

    /**
     * Starts a request without parameters, headers and the like, like {@link RestAssured#when()}.
     *
     * @return A request sender with the defaults of this client
     */
    public RequestSender when() {
        return testSpecification().getRequestSpecification();
    }

    /**
     * Starts a request with the response expectations, like {@link RestAssured#expect()}.
     *
     * @return A response specification with the defaults of this client
     */
    public ResponseSpecification expect() {
        return testSpecification().getResponseSpecification();
    }

    /**
     * Starts a request with a request specification, like {@link RestAssured#given(RequestSpecification)}: the same as
     * <code>given().spec(requestSpecification)</code>.
     *
     * @return A request specification with the defaults of this client, merged with <code>requestSpecification</code>
     */
    public RequestSpecification given(RequestSpecification requestSpecification) {
        return given().spec(requestSpecification);
    }

    /**
     * Sends with a request and a response specification as they are, like
     * {@link RestAssured#given(RequestSpecification, ResponseSpecification)}: the defaults of this client don't apply,
     * only its HTTP clients and the defaults of <code>then()</code> do.
     *
     * @return A request sender
     */
    public RequestSender given(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        throwIfClosed();
        return new TestSpecificationImpl(requestSpecification, responseSpecification, context);
    }

    /**
     * Sends a GET request, like {@link RestAssured#get(String, Object...)}.
     */
    public Response get(String path, Object... pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Sends a GET request, like {@link RestAssured#get(String, Map)}.
     */
    public Response get(String path, Map<String, ?> pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Sends a POST request, like {@link RestAssured#post(String, Object...)}.
     */
    public Response post(String path, Object... pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Sends a POST request, like {@link RestAssured#post(String, Map)}.
     */
    public Response post(String path, Map<String, ?> pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Sends a PUT request, like {@link RestAssured#put(String, Object...)}.
     */
    public Response put(String path, Object... pathParams) {
        return given().put(path, pathParams);
    }

    /**
     * Sends a DELETE request, like {@link RestAssured#delete(String, Object...)}.
     */
    public Response delete(String path, Object... pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Sends a DELETE request, like {@link RestAssured#delete(String, Map)}.
     */
    public Response delete(String path, Map<String, ?> pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Sends a HEAD request, like {@link RestAssured#head(String, Object...)}.
     */
    public Response head(String path, Object... pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Sends a HEAD request, like {@link RestAssured#head(String, Map)}.
     */
    public Response head(String path, Map<String, ?> pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Sends a PATCH request, like {@link RestAssured#patch(String, Object...)}.
     */
    public Response patch(String path, Object... pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Sends a PATCH request, like {@link RestAssured#patch(String, Map)}.
     */
    public Response patch(String path, Map<String, ?> pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Sends an OPTIONS request, like {@link RestAssured#options(String, Object...)}.
     */
    public Response options(String path, Object... pathParams) {
        return given().options(path, pathParams);
    }

    /**
     * Sends an OPTIONS request, like {@link RestAssured#options(String, Map)}.
     */
    public Response options(String path, Map<String, ?> pathParams) {
        return given().options(path, pathParams);
    }

    /**
     * Sends a QUERY request, like {@link RestAssured#query(String, Object...)}.
     */
    public Response query(String path, Object... pathParams) {
        return given().query(path, pathParams);
    }

    /**
     * Sends a QUERY request, like {@link RestAssured#query(String, Map)}.
     */
    public Response query(String path, Map<String, ?> pathParams) {
        return given().query(path, pathParams);
    }

    /**
     * Sends a GET request, like {@link RestAssured#get(URI)}.
     */
    public Response get(URI uri) {
        return given().get(uri);
    }

    /**
     * Sends a POST request, like {@link RestAssured#post(URI)}.
     */
    public Response post(URI uri) {
        return given().post(uri);
    }

    /**
     * Sends a PUT request, like {@link RestAssured#put(URI)}.
     */
    public Response put(URI uri) {
        return given().put(uri);
    }

    /**
     * Sends a DELETE request, like {@link RestAssured#delete(URI)}.
     */
    public Response delete(URI uri) {
        return given().delete(uri);
    }

    /**
     * Sends a HEAD request, like {@link RestAssured#head(URI)}.
     */
    public Response head(URI uri) {
        return given().head(uri);
    }

    /**
     * Sends a PATCH request, like {@link RestAssured#patch(URI)}.
     */
    public Response patch(URI uri) {
        return given().patch(uri);
    }

    /**
     * Sends an OPTIONS request, like {@link RestAssured#options(URI)}.
     */
    public Response options(URI uri) {
        return given().options(uri);
    }

    /**
     * Sends a QUERY request, like {@link RestAssured#query(URI)}.
     */
    public Response query(URI uri) {
        return given().query(uri);
    }

    /**
     * Sends a GET request, like {@link RestAssured#get(URL)}.
     */
    public Response get(URL url) {
        return given().get(url);
    }

    /**
     * Sends a POST request, like {@link RestAssured#post(URL)}.
     */
    public Response post(URL url) {
        return given().post(url);
    }

    /**
     * Sends a PUT request, like {@link RestAssured#put(URL)}.
     */
    public Response put(URL url) {
        return given().put(url);
    }

    /**
     * Sends a DELETE request, like {@link RestAssured#delete(URL)}.
     */
    public Response delete(URL url) {
        return given().delete(url);
    }

    /**
     * Sends a HEAD request, like {@link RestAssured#head(URL)}.
     */
    public Response head(URL url) {
        return given().head(url);
    }

    /**
     * Sends a PATCH request, like {@link RestAssured#patch(URL)}.
     */
    public Response patch(URL url) {
        return given().patch(url);
    }

    /**
     * Sends an OPTIONS request, like {@link RestAssured#options(URL)}.
     */
    public Response options(URL url) {
        return given().options(url);
    }

    /**
     * Sends a QUERY request, like {@link RestAssured#query(URL)}.
     */
    public Response query(URL url) {
        return given().query(url);
    }

    /**
     * Sends a GET request to the base URI, like {@link RestAssured#get()}.
     */
    public Response get() {
        return given().get();
    }

    /**
     * Sends a POST request to the base URI, like {@link RestAssured#post()}.
     */
    public Response post() {
        return given().post();
    }

    /**
     * Sends a PUT request to the base URI, like {@link RestAssured#put()}.
     */
    public Response put() {
        return given().put();
    }

    /**
     * Sends a DELETE request to the base URI, like {@link RestAssured#delete()}.
     */
    public Response delete() {
        return given().delete();
    }

    /**
     * Sends a HEAD request to the base URI, like {@link RestAssured#head()}.
     */
    public Response head() {
        return given().head();
    }

    /**
     * Sends a PATCH request to the base URI, like {@link RestAssured#patch()}.
     */
    public Response patch() {
        return given().patch();
    }

    /**
     * Sends an OPTIONS request to the base URI, like {@link RestAssured#options()}.
     */
    public Response options() {
        return given().options();
    }

    /**
     * Sends a QUERY request to the base URI, like {@link RestAssured#query()}.
     */
    public Response query() {
        return given().query();
    }

    /**
     * Sends a request to the base URI, like {@link RestAssured#request(Method)}.
     */
    public Response request(Method method) {
        return given().request(method);
    }

    /**
     * Sends a request to the base URI, like {@link RestAssured#request(String)}.
     */
    public Response request(String method) {
        return given().request(method);
    }

    /**
     * Sends a request, like {@link RestAssured#request(Method, String, Object...)}.
     */
    public Response request(Method method, String path, Object... pathParams) {
        return given().request(method, path, pathParams);
    }

    /**
     * Sends a request, like {@link RestAssured#request(String, String, Object...)}.
     */
    public Response request(String method, String path, Object... pathParams) {
        return given().request(method, path, pathParams);
    }

    /**
     * Sends a request, like {@link RestAssured#request(Method, URI)}.
     */
    public Response request(Method method, URI uri) {
        return given().request(method, uri);
    }

    /**
     * Sends a request, like {@link RestAssured#request(Method, URL)}.
     */
    public Response request(Method method, URL url) {
        return given().request(method, url);
    }

    /**
     * Sends a request, like {@link RestAssured#request(String, URI)}.
     */
    public Response request(String method, URI uri) {
        return given().request(method, uri);
    }

    /**
     * Sends a request, like {@link RestAssured#request(String, URL)}.
     */
    public Response request(String method, URL url) {
        return given().request(method, url);
    }

    /**
     * @return The config of this client, with its session id as session id value if it has one, like
     * {@link RestAssured#config()}
     */
    public RestAssuredConfig config() {
        return defaults.get().configWithSessionId();
    }

    /**
     * @return The default filters of this client (read-only)
     */
    public List<Filter> filters() {
        return Collections.unmodifiableList(defaults.get().filters());
    }

    /**
     * Closes the HTTP clients of this client, now or, when a response still uses one, once the response is read or its
     * stream closed, and drops the cookies it kept. Starting or sending a request with this client fails from then on.
     * Only the first call counts.
     */
    @Override
    public void close() {
        context.instances().close();
    }

    private TestSpecificationImpl testSpecification() {
        throwIfClosed();
        return defaults.get().testSpecification(context);
    }

    private void throwIfClosed() {
        context.instances().throwIfClosed();
    }

    /**
     * Builds a {@link RestAssuredClient}. It starts from the values that {@link RestAssured#reset()} sets, and has a method
     * for each static field and static setter of {@link RestAssured}. As with those, the last value set wins: for example
     * {@link #relaxedHTTPSValidation()} followed by {@link #config(RestAssuredConfig)} leaves the SSL config of that config.
     * A builder isn't thread safe. It can build several clients, which are independent of each other and of the builder.
     */
    public static final class Builder {
        private String baseUri = RestAssured.DEFAULT_URI;
        private int port = RestAssured.UNDEFINED_PORT;
        private String basePath = RestAssured.DEFAULT_PATH;
        private boolean urlEncodingEnabled = RestAssured.DEFAULT_URL_ENCODING_ENABLED;
        private AuthenticationScheme authentication = RestAssured.DEFAULT_AUTH;
        private RestAssuredConfig config = new RestAssuredConfig();
        private String rootPath = RestAssured.DEFAULT_BODY_ROOT_PATH;
        // Copies that nothing changes, so the clients built share them
        private RequestSpecificationImpl requestSpecification;
        private ResponseSpecificationImpl responseSpecification;
        private Parser defaultParser;
        private String sessionId = RestAssured.DEFAULT_SESSION_ID_VALUE;
        private ProxySpecification proxy;
        private List<Filter> filters = new ArrayList<>();
        private final ResponseParserRegistrar parsers;

        private Builder() {
            parsers = new ResponseParserRegistrar();
        }

        // Copies the specifications, which may be the static ones that users change
        Builder(ClientDefaults defaults) {
            baseUri = defaults.baseUri();
            port = defaults.port();
            basePath = defaults.basePath();
            urlEncodingEnabled = defaults.urlEncodingEnabled();
            authentication = defaults.authentication();
            config = defaults.config();
            rootPath = defaults.rootPath();
            requestSpecification(defaults.requestSpecification());
            responseSpecification(defaults.responseSpecification());
            defaultParser = defaults.defaultParser();
            sessionId = defaults.sessionId();
            proxy = defaults.proxy();
            filters.addAll(defaults.filters());
            parsers = new ResponseParserRegistrar(defaults.parsers());
        }

        /**
         * @param baseUri The base URI, such as <code>http://localhost</code>, like {@link RestAssured#baseURI}
         */
        public Builder baseUri(String baseUri) {
            this.baseUri = notNull(baseUri, "Base URI");
            return this;
        }

        /**
         * @param port The port, like {@link RestAssured#port}
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * @param basePath The path that the path of each request is appended to, like {@link RestAssured#basePath}
         */
        public Builder basePath(String basePath) {
            this.basePath = notNull(basePath, "Base path");
            return this;
        }

        /**
         * @param urlEncodingEnabled Whether REST Assured URL encodes the URI and parameters, like {@link RestAssured#urlEncodingEnabled}
         */
        public Builder urlEncodingEnabled(boolean urlEncodingEnabled) {
            this.urlEncodingEnabled = urlEncodingEnabled;
            return this;
        }

        /**
         * @param authentication The authentication scheme, such as {@link RestAssured#basic(String, String)}, like
         *                       {@link RestAssured#authentication}
         */
        public Builder authentication(AuthenticationScheme authentication) {
            this.authentication = notNull(authentication, AuthenticationScheme.class);
            return this;
        }

        /**
         * @param config The config, like {@link RestAssured#config}
         */
        public Builder config(RestAssuredConfig config) {
            this.config = notNull(config, RestAssuredConfig.class);
            return this;
        }

        /**
         * @param rootPath The root path of the body expectations, like {@link RestAssured#rootPath}
         */
        public Builder rootPath(String rootPath) {
            this.rootPath = notNull(rootPath, "Root path");
            return this;
        }

        /**
         * Sets the request specification that each <code>given()</code> starts from, like
         * {@link RestAssured#requestSpecification}. The builder takes a copy, so changing <code>requestSpecification</code>
         * later doesn't change the client.
         *
         * @param requestSpecification A specification made by {@link io.restassured.builder.RequestSpecBuilder} or
         *                             <code>given()</code>, or <code>null</code> for none
         */
        public Builder requestSpecification(RequestSpecification requestSpecification) {
            this.requestSpecification = requestSpecification == null ? null : RequestSpecificationImpl.copyOf(requestSpecification);
            return this;
        }

        /**
         * Sets the response specification that each request is validated with, like
         * {@link RestAssured#responseSpecification}. The builder takes a copy, so changing
         * <code>responseSpecification</code> later doesn't change the client.
         *
         * @param responseSpecification A specification made by {@link io.restassured.builder.ResponseSpecBuilder} or
         *                              <code>expect()</code>, or <code>null</code> for none
         */
        public Builder responseSpecification(ResponseSpecification responseSpecification) {
            this.responseSpecification = responseSpecification == null ? null : ResponseSpecificationImpl.copyOf(responseSpecification);
            return this;
        }

        /**
         * @param defaultParser The parser for responses whose content type has none, like {@link RestAssured#defaultParser},
         *                      or <code>null</code> for none
         */
        public Builder defaultParser(Parser defaultParser) {
            this.defaultParser = defaultParser;
            return this;
        }

        /**
         * Registers a parser for a content type, like {@link RestAssured#registerParser(String, Parser)}.
         */
        public Builder registerParser(String contentType, Parser parser) {
            parsers.registerParser(contentType, parser);
            return this;
        }

        /**
         * Unregisters the parser of a content type, like {@link RestAssured#unregisterParser(String)}.
         */
        public Builder unregisterParser(String contentType) {
            parsers.unregisterParser(contentType);
            return this;
        }

        /**
         * @param sessionId The session id value that each request sends, like {@link RestAssured#sessionId}, or
         *                  <code>null</code> for none
         */
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @param proxy The proxy, like {@link RestAssured#proxy(ProxySpecification)}, or <code>null</code> for none
         */
        public Builder proxy(ProxySpecification proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Uses a proxy on a host and port, like {@link RestAssured#proxy(String, int)}.
         */
        public Builder proxy(String host, int port) {
            return proxy(ProxySpecification.host(host).withPort(port));
        }

        /**
         * Uses a proxy on a host on port 8888, or at a URI, like {@link RestAssured#proxy(String)}.
         */
        public Builder proxy(String hostOrUri) {
            return UriValidator.isUri(hostOrUri) ? proxy(URI.create(hostOrUri)) : proxy(ProxySpecification.host(hostOrUri));
        }

        /**
         * Uses a proxy on a port of localhost, like {@link RestAssured#proxy(int)}.
         */
        public Builder proxy(int port) {
            return proxy(ProxySpecification.port(port));
        }

        /**
         * Uses a proxy on a host and port with a scheme (http or https), like {@link RestAssured#proxy(String, int, String)}.
         */
        public Builder proxy(String host, int port, String scheme) {
            return proxy(new ProxySpecification(host, port, scheme));
        }

        /**
         * Uses a proxy at a URI, like {@link RestAssured#proxy(URI)}.
         */
        public Builder proxy(URI uri) {
            notNull(uri, "Proxy URI");
            return proxy(new ProxySpecification(uri.getHost(), uri.getPort(), uri.getScheme()));
        }

        /**
         * Adds filters that apply to each request, like {@link RestAssured#filters(List)}.
         */
        public Builder filters(List<Filter> filters) {
            Validate.notNull(filters, "Filter list cannot be null");
            this.filters.addAll(filters);
            return this;
        }

        /**
         * Adds filters that apply to each request, like {@link RestAssured#filters(Filter, Filter...)}.
         */
        public Builder filters(Filter filter, Filter... additionalFilters) {
            return filters(asList(filter, additionalFilters));
        }

        /**
         * Replaces the filters that apply to each request, like {@link RestAssured#replaceFiltersWith(List)}.
         */
        public Builder replaceFiltersWith(List<Filter> filters) {
            Validate.notNull(filters, "Filter list cannot be null");
            this.filters = new ArrayList<>(filters);
            return this;
        }

        /**
         * Replaces the filters that apply to each request, like {@link RestAssured#replaceFiltersWith(Filter, Filter...)}.
         */
        public Builder replaceFiltersWith(Filter filter, Filter... additionalFilters) {
            return replaceFiltersWith(asList(filter, additionalFilters));
        }

        private static List<Filter> asList(Filter filter, Filter... additionalFilters) {
            Validate.notNull(filter, "Filter cannot be null");
            List<Filter> list = new ArrayList<>();
            list.add(filter);
            if (additionalFilters != null) {
                Collections.addAll(list, additionalFilters);
            }
            return list;
        }

        /**
         * Trusts every server certificate and host name, with the protocol SSL, like
         * {@link RestAssured#useRelaxedHTTPSValidation()}. Replaces the SSL config.
         */
        public Builder relaxedHTTPSValidation() {
            return relaxedHTTPSValidation("SSL");
        }

        /**
         * Trusts every server certificate and host name, with a protocol, like
         * {@link RestAssured#useRelaxedHTTPSValidation(String)}. Replaces the SSL config.
         */
        public Builder relaxedHTTPSValidation(String protocol) {
            config = config.sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation(protocol));
            return this;
        }

        /**
         * Logs the request and the response when validation fails, with {@link LogDetail#ALL}, like
         * {@link RestAssured#enableLoggingOfRequestAndResponseIfValidationFails()}.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails() {
            return enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
        }

        /**
         * Logs the request and the response when validation fails, like
         * {@link RestAssured#enableLoggingOfRequestAndResponseIfValidationFails(LogDetail)}. Replaces the log config, and
         * also applies to the request specification set so far.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
            config = config.logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
            if (requestSpecification != null) {
                // Changes a copy, since the clients built before share the specification
                RequestSpecificationImpl specification = RequestSpecificationImpl.copyOf(requestSpecification);
                RestAssuredConfig specificationConfig = specification.getConfig();
                specification.config(specificationConfig == null ? config : specificationConfig.logConfig(
                        specificationConfig.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail)));
                requestSpecification = specification;
            }
            return this;
        }

        /**
         * Uses a key store, like {@link RestAssured#keyStore(String, String)}.
         */
        public Builder keyStore(String pathToJks, String password) {
            Validate.notEmpty(password, "Password cannot be empty");
            config = config.sslConfig(config.getSSLConfig().keyStore(pathToJks, password));
            return this;
        }

        /**
         * Uses a key store on the file system, like {@link RestAssured#keyStore(File, String)}.
         */
        public Builder keyStore(File pathToJks, String password) {
            Validate.notNull(pathToJks, "Path to JKS on the file system cannot be null");
            config = config.sslConfig(config.getSSLConfig().keyStore(pathToJks, password));
            return this;
        }

        /**
         * Uses the key store at <code>${user.home}/.keystore</code>, like {@link RestAssured#keyStore(String)}.
         */
        public Builder keyStore(String password) {
            config = config.sslConfig(config.getSSLConfig().keyStore(password));
            return this;
        }

        /**
         * Uses a trust store, like {@link RestAssured#trustStore(String, String)}.
         */
        public Builder trustStore(String pathToJks, String password) {
            Validate.notEmpty(password, "Password cannot be empty");
            config = config.sslConfig(config.getSSLConfig().trustStore(pathToJks, password));
            return this;
        }

        /**
         * Uses a trust store on the file system, like {@link RestAssured#trustStore(File, String)}.
         */
        public Builder trustStore(File pathToJks, String password) {
            Validate.notNull(pathToJks, "Path to JKS on the file system cannot be null");
            config = config.sslConfig(config.getSSLConfig().trustStore(pathToJks, password));
            return this;
        }

        /**
         * Uses a loaded trust store, like {@link RestAssured#trustStore(KeyStore)}. Replaces the SSL config.
         */
        public Builder trustStore(KeyStore truststore) {
            Validate.notNull(truststore, "Truststore cannot be null");
            config = config.sslConfig(SSLConfig.sslConfig().trustStore(truststore));
            return this;
        }

        /**
         * @return A new client with the values of this builder
         */
        public RestAssuredClient build() {
            ClientDefaults defaults = new ClientDefaults(baseUri, port, basePath, urlEncodingEnabled, authentication, config, rootPath,
                    requestSpecification, responseSpecification, defaultParser, sessionId, proxy, Collections.unmodifiableList(new ArrayList<>(filters)),
                    new ResponseParserRegistrar(parsers));
            ResponseDefaults responseDefaults = new ResponseDefaults(defaults.rootPath(), defaults.responseSpecification());
            return new RestAssuredClient(() -> defaults, new ClientContext(HttpClientInstances.owned(), () -> responseDefaults));
        }
    }
}
