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
 * Configure the HTTP client that sends the requests: which client it is ({@link #client(HttpClientType)}), its
 * timeouts, whether it keeps the cookies of the responses, <code>Expect: 100-continue</code>, how multi-part requests
 * are written, and the settings of the client itself ({@link #customizeClient(Class, Consumer)}).
 * <p>Note that you can't configure the redirect settings from this config. Please use {@link RedirectConfig} for this purpose.</p>
 * <p>
 * The settings are kept as the string parameters of Apache HttpClient 4, which {@link #params()} returns, so that
 * {@link #setParam(String, Object)} still works until REST Assured 8.0.0. By default responses cookies aren't kept
 * (<code>http.protocol.cookie-policy</code> is <code>ignoreCookies</code>), and the cookie date patterns
 * (<code>http.protocol.cookie-datepatterns</code>) are <code>EEE, dd-MMM-yyyy HH:mm:ss z</code> and
 * <code>EEE, dd MMM yyyy HH:mm:ss z</code>.
 * </p>
 */
public class HttpClientConfig implements Config {

    // The names and values of the Apache HttpClient 4 parameters that the settings are kept as
    private static final String CONNECTION_TIMEOUT = "http.connection.timeout";
    private static final String SO_TIMEOUT = "http.socket.timeout";
    private static final String CONN_MANAGER_TIMEOUT = "http.conn-manager.timeout";
    private static final String COOKIE_POLICY = "http.protocol.cookie-policy";
    private static final String IGNORE_COOKIES = "ignoreCookies";
    private static final String DEFAULT_COOKIE_SPEC = "default";
    private static final String DATE_PATTERNS = "http.protocol.cookie-datepatterns";
    private static final String USE_EXPECT_CONTINUE = "http.protocol.expect-continue";

    // Shared by the copies that keep the reused HTTP client instances and kept cookies, null if they aren't reused
    private final Object reuseIdentity;
    private final Map<String, ?> httpClientParams;
    private final MultipartMode multipartMode;
    private final boolean isUserConfigured;
    private final Object client;
    private final List<ClientCustomizer> customizers;

    /**
     * Creates a new HttpClientConfig instance that doesn't keep the cookies of the responses.
     */
    public HttpClientConfig() {
        Map<String, Object> defaultParams = new HashMap<>();
        defaultParams.put(COOKIE_POLICY, IGNORE_COOKIES);
        defaultParams.put(DATE_PATTERNS, asList("EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z"));
        this.httpClientParams = defaultParams;
        this.multipartMode = MultipartMode.STRICT;
        this.reuseIdentity = null;
        this.isUserConfigured = false;
        this.client = null;
        this.customizers = List.of();
    }

    private HttpClientConfig(Map<String, ?> httpClientParams, MultipartMode multipartMode, Object reuseIdentity,
                             boolean isUserConfigured, Object client, List<ClientCustomizer> customizers) {
        notNull(httpClientParams, "httpClientParams");
        notNull(multipartMode, "multipartMode");
        this.reuseIdentity = reuseIdentity;
        this.httpClientParams = new HashMap<String, Object>(httpClientParams);
        this.multipartMode = multipartMode;
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
        this(httpClientParams, MultipartMode.STRICT, null, true, null, List.of());
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
     * Instruct REST Assured to reuse the HTTP client instances for multiple requests that use this config, so that
     * they share connections, and, with {@link #keepResponseCookies(boolean)}, the cookies of the responses. A client
     * that is shared by all requests anyway, such as the JDK HTTP client, is shared in the same way. The copies of this
     * config that {@link #client(HttpClientType)}, {@link #customizeClient(Class, Consumer)} and
     * {@link #multipartMode(MultipartMode)} return share them too, while a copy with other settings, such as a
     * timeout, starts anew, as in REST Assured 6. Note that for this to work the configuration must be defined
     * statically, for example:
     * <pre>
     * RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());
     * </pre>
     *
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig reuseHttpClientInstance() {
        return new HttpClientConfig(httpClientParams, multipartMode, reuseIdentity == null ? new Object() : reuseIdentity, true, client, customizers);
    }

    /**
     * Instruct REST Assured <i>not</i> to reuse the configured http client instance for multiple requests. This is the default behavior.
     *
     * @return An updated HttpClientConfig
     * @see #reuseHttpClientInstance()
     */
    public HttpClientConfig dontReuseHttpClientInstance() {
        return new HttpClientConfig(httpClientParams, multipartMode, null, true, client, customizers);
    }

    /**
     * If this method returns <code>true</code> then REST Assured reuses the HTTP client instances for the requests that
     * use this config (see {@link #reuseHttpClientInstance()}).
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
        return reuseIdentity != null;
    }

    /**
     * Used by REST Assured to keep the HTTP client instances and cookies of {@link #reuseHttpClientInstance()}.
     *
     * @return The same object for this config and the copies of it that share its reused HTTP client instances and
     * kept cookies, or <code>null</code> if they aren't reused
     */
    public Object reuseIdentity() {
        return reuseIdentity;
    }

    // As with HttpClient 4, where the reused client was created from the parameters
    private Object newReuseIdentity() {
        return reuseIdentity == null ? null : new Object();
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
        return new HttpClientConfig(newParams, multipartMode, newReuseIdentity(), true, client, customizers);
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
        return new HttpClientConfig(httpClientParams, multipartMode, newReuseIdentity(), true, client, customizers);
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
        return new HttpClientConfig(newParams, multipartMode, newReuseIdentity(), true, client, customizers);
    }

    /**
     * Choose the HTTP client that sends the requests, from one of REST Assured's client modules, which must be on the
     * classpath. Without a choice, REST Assured uses the client named by the system property
     * <code>restassured.http.client</code>, else <code>io.rest-assured:http-client-apache5</code> if it's on the classpath,
     * else the JDK HTTP client of <code>io.rest-assured:http-client-jdk</code>, the default.
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
        return new HttpClientConfig(httpClientParams, multipartMode, reuseIdentity, true, client, newCustomizers);
    }

    /**
     * @return The customizers added with {@link #customizeClient(Class, Consumer)}, in order
     */
    public List<ClientCustomizer> customizers() {
        return customizers;
    }

    private HttpClientConfig withClient(Object client) {
        return new HttpClientConfig(httpClientParams, multipartMode, reuseIdentity, true, client, customizers);
    }

    /**
     * @return A static way to create a new HttpClientConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static HttpClientConfig httpClientConfig() {
        return new HttpClientConfig();
    }

    /**
     * Specify how multi-part requests are written. The default is {@link MultipartMode#STRICT}.
     *
     * @param multipartMode The multi-part mode to use.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig multipartMode(MultipartMode multipartMode) {
        notNull(multipartMode, "multipartMode");
        return new HttpClientConfig(httpClientParams, multipartMode, reuseIdentity, true, client, customizers);
    }

    /**
     * @return How multi-part requests are written.
     */
    public MultipartMode multipartMode() {
        return multipartMode;
    }

    /**
     * Set how long to wait for a connection to the server to be established. By default there's no limit.
     *
     * @param connectTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig connectTimeout(Duration connectTimeout) {
        return setParam(CONNECTION_TIMEOUT, toMillisAsInt(connectTimeout, "connectTimeout"));
    }

    /**
     * @return How long to wait for a connection to the server to be established, or <code>null</code> if it's not set.
     */
    public Duration connectTimeout() {
        return durationParam(CONNECTION_TIMEOUT);
    }

    /**
     * Set how long to wait for data from the server, measured as the longest time between two packets of the response.
     * By default there's no limit.
     *
     * @param readTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig readTimeout(Duration readTimeout) {
        return setParam(SO_TIMEOUT, toMillisAsInt(readTimeout, "readTimeout"));
    }

    /**
     * @return How long to wait for data from the server, or <code>null</code> if it's not set.
     */
    public Duration readTimeout() {
        return durationParam(SO_TIMEOUT);
    }

    /**
     * Set how long to wait for a connection from the connection pool of the HTTP client. The JDK HTTP client, the
     * default, doesn't limit its connections, so this has no effect on it.
     *
     * @param connectionRequestTimeout The timeout. {@link Duration#ZERO} means no limit.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig connectionRequestTimeout(Duration connectionRequestTimeout) {
        return setParam(CONN_MANAGER_TIMEOUT, toMillis(connectionRequestTimeout, "connectionRequestTimeout"));
    }

    /**
     * @return How long to wait for a connection from the connection pool, or <code>null</code> if it's not set.
     */
    public Duration connectionRequestTimeout() {
        return durationParam(CONN_MANAGER_TIMEOUT);
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
        return setParam(COOKIE_POLICY, keepResponseCookies ? DEFAULT_COOKIE_SPEC : IGNORE_COOKIES);
    }

    /**
     * @return <code>true</code> if the cookies a response sets are sent in the requests that follow it.
     */
    public boolean keepsResponseCookies() {
        Object cookiePolicy = httpClientParams.get(COOKIE_POLICY);
        return !IGNORE_COOKIES.equals(cookiePolicy);
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
        return setParam(DATE_PATTERNS, new ArrayList<>(asList(cookieDatePatterns)));
    }

    /**
     * @return The date patterns used to parse a non-standard <code>expires</code> attribute of a response cookie.
     */
    public List<String> cookieDatePatterns() {
        Object cookieDatePatterns = httpClientParams.get(DATE_PATTERNS);
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
        return setParam(USE_EXPECT_CONTINUE, expectContinue);
    }

    /**
     * @return <code>true</code> if requests with a body send <code>Expect: 100-continue</code>.
     */
    public boolean expectsContinue() {
        Object expectContinue = httpClientParams.get(USE_EXPECT_CONTINUE);
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

    public boolean isUserConfigured() {
        return isUserConfigured;
    }
}