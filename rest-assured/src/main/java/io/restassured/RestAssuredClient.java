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
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.TestSpecificationImpl;
import io.restassured.internal.UriValidator;
import io.restassured.internal.client.ClientDefaults;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.support.CommonVersionCheck;
import io.restassured.parsing.Parser;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Defaults of its own for the requests of {@link RestAssured}: a base URI, port, config, filters, specifications and so
 * on. A client never reads the static fields of <code>RestAssured</code>, so tests that run in parallel can each use
 * their own settings. A client sends nothing by itself. Requests start with {@link RestAssured#given()} as usual, and
 * it picks the client that they use.
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
 * <h2>Using a client</h2>
 * A request uses the first of these that applies:
 * <ol>
 * <li>A client named in {@link RestAssured#given(RestAssuredClient)}, for example for a second service, or for a request
 * on another thread:
 * <pre>
 * given(orders).queryParam("status", "open").when().get("/orders").then().statusCode(200);
 * </pre></li>
 * <li>The client bound to the current thread with {@link RestAssured#useClient(RestAssuredClient)}, for example for
 * tests that run in parallel and each have their own settings. <code>given()</code>, <code>when()</code>,
 * <code>get(..)</code> and the other static methods that start or send a request then use it, until
 * {@link RestAssured#resetClient()} or {@link RestAssured#reset()} unbinds it:
 * <pre>
 * &#64;BeforeEach void useOwnPort() { RestAssured.useClient(RestAssuredClient.builder().port(port).build()); }
 * &#64;AfterEach void resetRestAssured() { RestAssured.reset(); }
 *
 * &#64;Test void shows_an_order() {
 *     get("/orders/{id}", 42).then().body("id", equalTo(42));
 * }
 * </pre></li>
 * <li>The static settings of <code>RestAssured</code>, as without a client.</li>
 * </ol>
 * The binding is for the current thread only. A request on another thread, for example in a condition that Awaitility
 * polls on its own thread by default, uses the static settings unless it names the client with
 * <code>given(client)</code> (or Awaitility polls with <code>pollInSameThread()</code>).
 * <h2>Changing a client</h2>
 * A client can't be changed once it's built. {@link #toBuilder()} gives a builder with its values, which builds a new,
 * independent client:
 * <pre>
 * RestAssuredClient admin = api.toBuilder().authentication(basic("admin", "secret")).build();
 * </pre>
 * <h2>Connections</h2>
 * A client is an immutable set of settings. It owns no HTTP clients or cookies, so there is nothing to close: its
 * requests take their connections and HTTP clients by the same rules as those of the static API. For example, with
 * http-client-apache5 each request gets a new HTTP client unless the config has
 * <code>httpClientConfig().reuseHttpClientInstance()</code>, which keeps one pooled HTTP client, and with
 * <code>keepResponseCookies(true)</code> the cookies, for that config and its copies. A client that has such a config
 * from {@link RestAssured#client()} or the static config shares them with the static API.
 * <h2>Thread safety</h2>
 * <ul>
 * <li>A client can be used from many threads at once.</li>
 * <li><code>given(client)</code> returns a new specification each time. Don't change one specification from several
 * threads at once. Sending one specification, or one built with {@link io.restassured.builder.RequestSpecBuilder}, from
 * many threads is safe.</li>
 * <li>The filters of the client, including those of its default request specification, are used by all its requests
 * and must be thread safe.</li>
 * <li>The cookies kept with <code>reuseHttpClientInstance().keepResponseCookies(true)</code> are shared by the requests
 * of that config from all threads.</li>
 * </ul>
 * <h2>Static settings that still apply</h2>
 * A specification built with {@link io.restassured.builder.RequestSpecBuilder} or
 * {@link io.restassured.builder.ResponseSpecBuilder} contains static settings of <code>RestAssured</code> from when the
 * builder was created, such as the filters, the config, the request specification, the root path and the default
 * parser, which then apply to the requests of the client it's used with. The extra requests of form authentication and
 * CSRF are sent with the client bound to the thread, if there is one, and otherwise with the static settings.
 */
public final class RestAssuredClient {

    static {
        // First, so that an older rest-assured-common gives a message instead of a NoClassDefFoundError
        CommonVersionCheck.verify();
    }

    private final Supplier<ClientDefaults> defaults;
    private final Supplier<ResponseDefaults> responseDefaults;

    // The default client of the static API reads the static fields at each call
    RestAssuredClient(Supplier<ClientDefaults> defaults, Supplier<ResponseDefaults> responseDefaults) {
        this.defaults = defaults;
        this.responseDefaults = responseDefaults;
    }

    /**
     * @return A builder that starts from the values that {@link RestAssured#reset()} sets
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A builder with the values of this client. The client it builds is independent of this one.
     */
    public Builder toBuilder() {
        return new Builder(defaults.get());
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

    // Starts a request of RestAssured.given(), given(client), when() or expect()
    TestSpecificationImpl testSpecification() {
        return defaults.get().testSpecification(responseDefaults);
    }

    // Sends the specifications of RestAssured.given(req, resp) as they are, with the then() defaults of this client
    TestSpecificationImpl testSpecification(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        return new TestSpecificationImpl(requestSpecification, responseSpecification, responseDefaults);
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
            return new RestAssuredClient(() -> defaults, () -> responseDefaults);
        }
    }
}
