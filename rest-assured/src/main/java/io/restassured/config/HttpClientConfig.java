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

package io.restassured.config;

import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.HttpClientProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.DefaultHttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static java.util.Arrays.asList;

/**
 * Configure the Apache HTTP Client parameters.
 * <p>Note that you can't configure the redirect settings from this config. Please use {@link RedirectConfig} for this purpose.</p>
 * <p/>
 * The following parameters are applied per default:
 * <table border=1>
 * <tr>
 * <th>Parameter name</th><th>Parameter value</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link ClientPNames#COOKIE_POLICY}</td><td>{@link CookiePolicy#IGNORE_COOKIES}</td><td>Don't automatically set response cookies in subsequent requests.</td>
 * </tr>
 * <tr>
 * <td>{@link CookieSpecPNames#DATE_PATTERNS}</td><td>[EEE, dd-MMM-yyyy HH:mm:ss z, EEE, dd MMM yyyy HH:mm:ss z]</td><td>Defines valid date patterns to be used for parsing non-standard
 * <code>expires</code> attribute.</td>
 * <p/>
 * </tr>
 * </table>
 * <p>
 * You can also specify a http client factory that is used to create the http client instances that REST Assured uses ({@link #httpClientFactory(HttpClientConfig.HttpClientFactory)}).
 * By default the {@link DefaultHttpClient} is used. It's also possible to specify whether or not this instance should be reused in multiple requests. By default the http client instance is not reused.
 * </p>
 *
 * @see org.apache.http.client.params.ClientPNames
 * @see org.apache.http.client.params.CookiePolicy
 * @see org.apache.http.params.CoreProtocolPNames
 */
    public class HttpClientConfig implements Config {

    private static final boolean SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT = false;
    private static final HttpClient NO_HTTP_CLIENT = null;
    private static final HttpClientFactory DEFAULT_HTTP_CLIENT_FACTORY = new HttpClientFactory() {
        public HttpClient createHttpClient() {
            return new DefaultHttpClient();
        }
    };

    private final boolean shouldReuseHttpClientInstance;
    private final Map<String, ?> httpClientParams;
    private final HttpMultipartMode httpMultipartMode;
    private final HttpClientFactory httpClientFactory;
    private final boolean isUserConfigured;
    private final Object client;
    private final List<ClientCustomizer> customizers;
    private final Object httpClientLock = new Object();
    private volatile HttpClient httpClient;

    /**
     * Creates a new  HttpClientConfig instance with the <code>{@value org.apache.http.client.params.ClientPNames#COOKIE_POLICY}</code> parameter set to <code>{@value org.apache.http.client.params.CookiePolicy#IGNORE_COOKIES}</code>.
     */
    public HttpClientConfig() {
        this.httpClientFactory = defaultHttpClientFactory();


        this.httpClientParams = new HashMap<String, Object>() {
            {
                put(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
                put(CookieSpecPNames.DATE_PATTERNS, asList("EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z"));
            }
        };
        this.httpMultipartMode = HttpMultipartMode.STRICT;
        this.shouldReuseHttpClientInstance = SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT;
        this.httpClient = null;
        this.isUserConfigured = false;
        this.client = null;
        this.customizers = List.of();
    }

    private HttpClientConfig(HttpClientFactory httpClientFactory, Map<String, ?> httpClientParams, HttpMultipartMode httpMultipartMode,
                             boolean shouldReuseHttpClientInstance, HttpClient abstractHttpClient, boolean isUserConfigured,
                             Object client, List<ClientCustomizer> customizers) {
        notNull(httpClientParams, "httpClientParams");
        notNull(httpMultipartMode, "httpMultipartMode");
        notNull(httpClientFactory, "Http Client factory");
        this.shouldReuseHttpClientInstance = shouldReuseHttpClientInstance;
        this.httpClientFactory = httpClientFactory;
        this.httpClientParams = new HashMap<String, Object>(httpClientParams);
        this.httpMultipartMode = httpMultipartMode;
        this.httpClient = abstractHttpClient;
        this.isUserConfigured = isUserConfigured;
        this.client = client;
        this.customizers = List.copyOf(customizers);
    }

    /**
     * Creates a new  HttpClientConfig instance with the parameters defined by the <code>httpClientParams</code>.
     *
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the settings on HttpClientConfig,
     * for example {@link #connectTimeout(Duration)}, {@link #readTimeout(Duration)} or {@link #keepResponseCookies(boolean)},
     * and {@link RedirectConfig} for redirects.
     */
    @Deprecated
    public HttpClientConfig(Map<String, ?> httpClientParams) {
        this(defaultHttpClientFactory(), httpClientParams, HttpMultipartMode.STRICT, SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT, NO_HTTP_CLIENT, true, null, List.of());
    }

    /**
     * @return The configured parameters
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the getters on HttpClientConfig,
     * for example {@link #connectTimeout()}, {@link #readTimeout()} or {@link #keepsResponseCookies()}.
     */
    @Deprecated
    public Map<String, ?> params() {
        return Collections.unmodifiableMap(httpClientParams);
    }

    /**
     * @return The same HttpClientConfig instance. Only here for syntactic sugar.
     */
    public HttpClientConfig and() {
        return this;
    }

    /**
     * Instruct REST Assured to reuse the configured http client instance for multiple requests. By default REST Assured
     * will create a new {@link org.apache.http.client.HttpClient} instance for each request. Note that for this to work
     * the configuration must be defined statically, for example:
     * <p/>
     * <pre>
     * RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());
     * </pre>
     * <p>
     * The reused instance is reconfigured by every request that uses it (parameters, interceptors, credentials, SSL and
     * proxy settings), so it must not be used by requests that run at the same time, even if the
     * {@link HttpClientFactory} returns a client with a thread-safe (pooling) connection manager.
     * </p>
     *
     * @return An updated HttpClientConfig
     * @see #httpClientFactory(HttpClientConfig.HttpClientFactory)
     */
    public HttpClientConfig reuseHttpClientInstance() {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, true, httpClient, true, client, customizers);
    }

    /**
     * Instruct REST Assured <i>not</i> to reuse the configured http client instance for multiple requests. This is the default behavior.
     *
     * @return An updated HttpClientConfig
     * @see #reuseHttpClientInstance()
     */
    public HttpClientConfig dontReuseHttpClientInstance() {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, false, NO_HTTP_CLIENT, true, client, customizers);
    }

    /**
     * If this method returns <code>true</code> then REST Assured will reuse the same {@link org.apache.http.client.HttpClient} instance created
     * by the {@link #httpClientInstance()} method for all requests. If <code>false</code> is returned then REST Assured creates a new instance for each request.
     * <p>
     * By default <code>false</code> is returned.
     * </p>
     * Note that for this to work the configuration must be defined statically, for example:
     * <pre>
     * RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());
     * </pre>
     *
     * @return <code>true</code> if the same HTTP Client instance should be reused between several requests, <code>false</code> otherwise.
     */
    public boolean isConfiguredToReuseTheSameHttpClientInstance() {
        return shouldReuseHttpClientInstance;
    }

    /**
     * Set a http client parameter.
     *
     * @param parameterName  The name of the parameter
     * @param parameterValue The value of the parameter (may be null)
     * @param <T>            The parameter type
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the settings on HttpClientConfig,
     * for example {@link #connectTimeout(Duration)}, {@link #readTimeout(Duration)} or {@link #keepResponseCookies(boolean)},
     * and {@link RedirectConfig} for redirects.
     */
    @Deprecated
    public <T> HttpClientConfig setParam(String parameterName, T parameterValue) {
        notNull(parameterName, "Parameter name");
        final Map<String, Object> newParams = new HashMap<String, Object>(httpClientParams);
        newParams.put(parameterName, parameterValue);
        return new HttpClientConfig(httpClientFactory, newParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true, client, customizers);
    }

    /**
     * Replaces the currently configured parameters with the ones supplied by <code>httpClientParams</code>. This method is the same as {@link #setParams(java.util.Map)}.
     *
     * @param httpClientParams The parameters to set.
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the settings on HttpClientConfig,
     * for example {@link #connectTimeout(Duration)}, {@link #readTimeout(Duration)} or {@link #keepResponseCookies(boolean)},
     * and {@link RedirectConfig} for redirects.
     */
    @Deprecated
    public HttpClientConfig withParams(Map<String, ?> httpClientParams) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true, client, customizers);
    }

    /**
     * Replaces the currently configured parameters with the ones supplied by <code>httpClientParams</code>. This method is the same as {@link #withParams(java.util.Map)}.
     *
     * @param httpClientParams The parameters to set.
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the settings on HttpClientConfig,
     * for example {@link #connectTimeout(Duration)}, {@link #readTimeout(Duration)} or {@link #keepResponseCookies(boolean)},
     * and {@link RedirectConfig} for redirects.
     */
    @Deprecated
    public HttpClientConfig setParams(Map<String, ?> httpClientParams) {
        return withParams(httpClientParams);
    }

    /**
     * Add the given parameters to an already configured number of parameters.
     *
     * @param httpClientParams The parameters.
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 parameters are removed in REST Assured 8.0.0. Use the settings on HttpClientConfig,
     * for example {@link #connectTimeout(Duration)}, {@link #readTimeout(Duration)} or {@link #keepResponseCookies(boolean)},
     * and {@link RedirectConfig} for redirects.
     */
    @Deprecated
    public HttpClientConfig addParams(Map<String, ?> httpClientParams) {
        notNull(httpClientParams, "httpClientParams");
        final Map<String, Object> newParams = new HashMap<String, Object>(this.httpClientParams);
        newParams.putAll(httpClientParams);
        return new HttpClientConfig(httpClientFactory, newParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true, client, customizers);
    }

    /**
     * Set the http client factory that Rest Assured should use when making request. For each request REST Assured will invoke the factory to get the a the HttpClient instance.
     *
     * @param httpClientFactory The http client factory to use.
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0. For timeouts, cookies and redirects use the
     * settings on HttpClientConfig and {@link RedirectConfig#followRedirectsForAllMethods(boolean)}, and use
     * <code>customizeClient(..)</code> in 7.0.0 to customize the client that REST Assured creates.
     */
    @Deprecated
    public HttpClientConfig httpClientFactory(HttpClientFactory httpClientFactory) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true, client, customizers);
    }

    /**
     * @return The configured http client that will create an {@link org.apache.http.client.HttpClient} instances that's used by REST Assured when making a request.
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0, and the HTTP client becomes internal to the module that provides it.
     */
    @Deprecated
    public HttpClient httpClientInstance() {
        if (isConfiguredToReuseTheSameHttpClientInstance()) {
            // Double-checked locking (httpClient is volatile) so that threads racing the first request share one instance
            HttpClient client = httpClient;
            if (client == NO_HTTP_CLIENT) {
                synchronized (httpClientLock) {
                    client = httpClient;
                    if (client == NO_HTTP_CLIENT) {
                        client = httpClientFactory.createHttpClient();
                        httpClient = client;
                    }
                }
            }
            return client;
        }
        return httpClientFactory.createHttpClient();
    }

    /**
     * Specify the HTTP Multipart mode when sending multi-part data.
     *
     * @param httpMultipartMode The multi-part mode to set.
     * @return An updated HttpClientConfig
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0. Use {@link #multipartMode(MultipartMode)} instead.
     */
    @Deprecated
    public HttpClientConfig httpMultipartMode(HttpMultipartMode httpMultipartMode) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, httpClient, true, client, customizers);
    }

    /**
     * Choose the HTTP client that sends the requests, from one of REST Assured's client modules, which must be on the
     * classpath. Without a choice, REST Assured uses the client named by the system property
     * <code>restassured.http.client</code>, else <code>io.rest-assured:http-client-apache5</code> if it's on the classpath,
     * else the default client.
     *
     * @param client The client
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig client(HttpClientType client) {
        notNull(client, "client");
        return withClient(client);
    }

    /**
     * Choose the HTTP client that sends the requests by its name, such as <code>jdk</code>, <code>apache5</code> or the
     * name of a client from another library that provides an {@link HttpClientProvider}. The client must be on the classpath.
     *
     * @param name The name of the client
     * @return An updated HttpClientConfig
     * @see #client(HttpClientType)
     */
    public HttpClientConfig client(String name) {
        notNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of the HTTP client can't be blank");
        }
        return withClient(name.trim());
    }

    /**
     * Choose the HTTP client that sends the requests by the class of its {@link HttpClientProvider}, which REST Assured
     * creates with its no-argument constructor.
     *
     * @param providerClass The class of the provider
     * @return An updated HttpClientConfig
     * @see #client(HttpClientType)
     */
    public HttpClientConfig client(Class<? extends HttpClientProvider> providerClass) {
        notNull(providerClass, "providerClass");
        return withClient(providerClass);
    }

    /**
     * The chosen HTTP client as it was given to <code>client(..)</code>, which is one of:
     * <ul>
     * <li>an {@link HttpClientType}, from {@link #client(HttpClientType)}</li>
     * <li>a <code>String</code>, the trimmed name from {@link #client(String)}</li>
     * <li>a <code>Class&lt;? extends HttpClientProvider&gt;</code>, from {@link #client(Class)}</li>
     * </ul>
     *
     * @return The chosen HTTP client, or <code>null</code> if none was chosen
     */
    public Object client() {
        return client;
    }

    /**
     * Customize the HTTP client that REST Assured creates, through the builder of the client's library, for example
     * <code>customizeClient(java.net.http.HttpClient.Builder.class, builder -&gt; builder.executor(executor))</code>
     * for the JDK client. The customizers run in the order they're added. A request fails if the builder type isn't
     * the one of the client REST Assured uses.
     *
     * @param builderType The type of the builder, which the client's module documents
     * @param customizer  What to do with the builder
     * @param <B>         The type of the builder
     * @return An updated HttpClientConfig
     */
    public <B> HttpClientConfig customizeClient(Class<B> builderType, Consumer<? super B> customizer) {
        notNull(builderType, "builderType");
        notNull(customizer, "customizer");
        List<ClientCustomizer> newCustomizers = new ArrayList<>(customizers);
        newCustomizers.add(new ClientCustomizer(builderType, customizer));
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, httpClient, true, client, newCustomizers);
    }

    /**
     * @return The customizers added with {@link #customizeClient(Class, Consumer)}, in order
     */
    public List<ClientCustomizer> customizers() {
        return customizers;
    }

    private HttpClientConfig withClient(Object client) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, httpClient, true, client, customizers);
    }

    /**
     * @return A static way to create a new HttpClientConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static HttpClientConfig httpClientConfig() {
        return new HttpClientConfig();
    }

    /**
     * @return The http multi-part mode.
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0. Use {@link #multipartMode()} instead.
     */
    @Deprecated
    public HttpMultipartMode httpMultipartMode() {
        return httpMultipartMode;
    }

    /**
     * Specify how multi-part requests are written. The default is {@link MultipartMode#STRICT}.
     *
     * @param multipartMode The multi-part mode to use.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig multipartMode(MultipartMode multipartMode) {
        notNull(multipartMode, "multipartMode");
        return httpMultipartMode(HttpMultipartMode.valueOf(multipartMode.name()));
    }

    /**
     * @return How multi-part requests are written.
     */
    public MultipartMode multipartMode() {
        return MultipartMode.valueOf(httpMultipartMode.name());
    }

    /**
     * Set how long to wait for a connection to the server to be established. By default there's no limit.
     *
     * @param connectTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig connectTimeout(Duration connectTimeout) {
        return setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, toMillisAsInt(connectTimeout, "connectTimeout"));
    }

    /**
     * @return How long to wait for a connection to the server to be established, or <code>null</code> if it's not set.
     */
    public Duration connectTimeout() {
        return durationParam(CoreConnectionPNames.CONNECTION_TIMEOUT);
    }

    /**
     * Set how long to wait for data from the server, measured as the longest time between two packets of the response.
     * By default there's no limit.
     *
     * @param readTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig readTimeout(Duration readTimeout) {
        return setParam(CoreConnectionPNames.SO_TIMEOUT, toMillisAsInt(readTimeout, "readTimeout"));
    }

    /**
     * @return How long to wait for data from the server, or <code>null</code> if it's not set.
     */
    public Duration readTimeout() {
        return durationParam(CoreConnectionPNames.SO_TIMEOUT);
    }

    /**
     * Set how long to wait for a connection from the connection pool. It only matters when the HTTP client has a
     * pooling connection manager, which the default client doesn't have.
     *
     * @param connectionRequestTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig connectionRequestTimeout(Duration connectionRequestTimeout) {
        // HttpClient 4 reads this parameter as a long
        return setParam(ClientPNames.CONN_MANAGER_TIMEOUT, toMillis(connectionRequestTimeout, "connectionRequestTimeout"));
    }

    /**
     * @return How long to wait for a connection from the connection pool, or <code>null</code> if it's not set.
     */
    public Duration connectionRequestTimeout() {
        return durationParam(ClientPNames.CONN_MANAGER_TIMEOUT);
    }

    /**
     * Set whether the cookies a response sets are sent in the requests that follow it, for example when a redirect is
     * followed, or across requests when the HTTP client instance is reused ({@link #reuseHttpClientInstance()}).
     * The default is <code>false</code>. Use {@link io.restassured.filter.cookie.CookieFilter} to keep cookies across
     * requests in general.
     *
     * @param keepResponseCookies <code>true</code> to keep the cookies that responses set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig keepResponseCookies(boolean keepResponseCookies) {
        return setParam(ClientPNames.COOKIE_POLICY, keepResponseCookies ? CookieSpecs.DEFAULT : CookiePolicy.IGNORE_COOKIES);
    }

    /**
     * @return <code>true</code> if the cookies a response sets are sent in the requests that follow it.
     */
    public boolean keepsResponseCookies() {
        Object cookiePolicy = httpClientParams.get(ClientPNames.COOKIE_POLICY);
        return !CookiePolicy.IGNORE_COOKIES.equals(cookiePolicy);
    }

    /**
     * Set the date patterns, in {@link java.text.SimpleDateFormat} syntax, used to parse an <code>expires</code>
     * attribute of a response cookie that doesn't follow the standard format. The default patterns are
     * <code>EEE, dd-MMM-yyyy HH:mm:ss z</code> and <code>EEE, dd MMM yyyy HH:mm:ss z</code>. They're only used for
     * the cookies kept with {@link #keepResponseCookies(boolean)}, not by {@link io.restassured.filter.cookie.CookieFilter}
     * or when you read the cookies of a response.
     *
     * @param cookieDatePatterns The date patterns.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig cookieDatePatterns(String... cookieDatePatterns) {
        notNull(cookieDatePatterns, "cookieDatePatterns");
        return setParam(CookieSpecPNames.DATE_PATTERNS, new ArrayList<>(asList(cookieDatePatterns)));
    }

    /**
     * @return The date patterns used to parse a non-standard <code>expires</code> attribute of a response cookie.
     */
    public List<String> cookieDatePatterns() {
        Object cookieDatePatterns = httpClientParams.get(CookieSpecPNames.DATE_PATTERNS);
        if (!(cookieDatePatterns instanceof Collection)) {
            return Collections.emptyList();
        }
        List<String> patterns = new ArrayList<>();
        for (Object pattern : (Collection<?>) cookieDatePatterns) {
            patterns.add(String.valueOf(pattern));
        }
        return Collections.unmodifiableList(patterns);
    }

    /**
     * Set whether requests with a body send <code>Expect: 100-continue</code> and wait for the server to accept the
     * request before sending the body. The default is <code>false</code>.
     *
     * @param expectContinue <code>true</code> to use <code>Expect: 100-continue</code>.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig expectContinue(boolean expectContinue) {
        return setParam(CoreProtocolPNames.USE_EXPECT_CONTINUE, expectContinue);
    }

    /**
     * @return <code>true</code> if requests with a body send <code>Expect: 100-continue</code>.
     */
    public boolean expectsContinue() {
        Object expectContinue = httpClientParams.get(CoreProtocolPNames.USE_EXPECT_CONTINUE);
        return expectContinue instanceof Boolean ? (Boolean) expectContinue : Boolean.parseBoolean(String.valueOf(expectContinue));
    }

    private static int toMillisAsInt(Duration duration, String name) {
        long millis = toMillis(duration, name);
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " can be at most " + Integer.MAX_VALUE + " milliseconds, was " + duration);
        }
        return (int) millis;
    }

    private static long toMillis(Duration duration, String name) {
        notNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " can't be negative, was " + duration);
        }
        // Round up, since 0 means no limit
        return duration.isZero() ? 0 : Math.max(1, duration.toMillis());
    }

    private Duration durationParam(String name) {
        Object value = httpClientParams.get(name);
        if (value instanceof Number) {
            return Duration.ofMillis(((Number) value).longValue());
        } else if (value instanceof String) {
            return Duration.ofMillis(Long.parseLong(((String) value).trim()));
        }
        return null;
    }

    private static HttpClientFactory defaultHttpClientFactory() {
        return DEFAULT_HTTP_CLIENT_FACTORY;
    }

    /**
     * @return <code>true</code> if the http clients are created by REST Assured's default http client factory, <code>false</code>
     * if a custom factory has been configured using {@link #httpClientFactory(HttpClientFactory)}.
     */
    public boolean usesDefaultHttpClientFactory() {
        return httpClientFactory == DEFAULT_HTTP_CLIENT_FACTORY;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    /**
     * A factory for creating and configuring a custom http client instance that will be used by REST Assured.
     *
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0. For timeouts, cookies and redirects use the
     * settings on HttpClientConfig and {@link RedirectConfig#followRedirectsForAllMethods(boolean)}, and use
     * <code>customizeClient(..)</code> in 7.0.0 to customize the client that REST Assured creates.
     */
    @Deprecated
    public interface HttpClientFactory {
        /**
         * Create an instance of {@link HttpClient} that'll be used by REST Assured when making requests. By default
         * REST Assured creates a {@link DefaultHttpClient}.
         * <p>
         * <b>Important: Version 1.9.0 of REST Assured ONLY supports instances of {@link org.apache.http.impl.client.AbstractHttpClient}</b>. The API is
         * how ever prepared for future upgrades.
         * </p>
         *
         * @return An instance of {@link HttpClient}.
         */
        HttpClient createHttpClient();
    }
}