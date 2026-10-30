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

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationContext;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.FormAuthScheme;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.CsrfConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.MultipartMode;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.OrderedFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.http.Method;
import io.restassured.internal.MapCreator.CollisionStrategy;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.common.util.GroovyStyleToString;
import io.restassured.internal.filter.CsrfFilter;
import io.restassured.internal.filter.FilterContextImpl;
import io.restassured.internal.filter.FormAuthFilter;
import io.restassured.internal.filter.SendRequestFilter;
import io.restassured.internal.http.BoundaryExtractor;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.http.ContentDecoding;
import io.restassured.internal.http.ContentTypeExtractor;
import io.restassured.internal.http.EncoderRegistry;
import io.restassured.internal.http.RequestBodyEncoder;
import io.restassured.internal.http.StringHashMap;
import io.restassured.internal.http.URIBuilder;
import io.restassured.internal.http.auth.ChallengeHandler;
import io.restassured.internal.http.auth.CoreAuthenticationContext;
import io.restassured.internal.http.client.ClientSettingsFactory;
import io.restassured.internal.http.client.HttpClientInstances;
import io.restassured.internal.http.client.HttpClientParams;
import io.restassured.internal.http.client.HttpClientProviders;
import io.restassured.internal.http.client.HttpClientProviders.Selection;
import io.restassured.internal.http.client.ProviderTransport;
import io.restassured.internal.http.client.TransportExceptions;
import io.restassured.internal.http.client.UnsupportedFeatures;
import io.restassured.internal.http.cookie.CookieStore;
import io.restassured.internal.http.cookie.KeptCookieSpec;
import io.restassured.internal.http.cookie.KeptCookies;
import io.restassured.internal.http.exchange.Exchange;
import io.restassured.internal.http.exchange.Exchange.Exchanged;
import io.restassured.internal.http.exchange.RedirectSettings;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.internal.multipart.MultiPartSpecificationImpl;
import io.restassured.internal.multipart.MultipartWriter;
import io.restassured.internal.support.ParameterUpdater;
import io.restassured.internal.support.PathSupport;
import io.restassured.internal.util.ExceptionUnwrapper;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.AuthenticationSpecification;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RedirectSpecification;
import io.restassured.specification.RequestLogSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.spi.AuthFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static io.restassured.http.ContentType.ANY;
import static io.restassured.http.ContentType.BINARY;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static io.restassured.http.ContentType.URLENC;
import static io.restassured.http.Method.DELETE;
import static io.restassured.http.Method.GET;
import static io.restassured.http.Method.HEAD;
import static io.restassured.http.Method.OPTIONS;
import static io.restassured.http.Method.PATCH;
import static io.restassured.http.Method.POST;
import static io.restassured.http.Method.PUT;
import static io.restassured.http.Method.QUERY;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.serialization.SerializationSupport.isParameterSerializableCandidate;
import static io.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;
import static io.restassured.internal.serialization.SerializationSupport.unwrapJsonStringLiteral;
import static io.restassured.internal.support.PathSupport.isFullyQualified;
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash;
import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * The request specification used by REST Assured (and, for logging, by the Spring modules).
 */
@SuppressWarnings("FieldMayBeFinal")
public class RequestSpecificationImpl implements FilterableRequestSpecification {
    private static final int DEFAULT_HTTP_TEST_PORT = 8080;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DOUBLE_SLASH = "//";
    private static final String LOCALHOST = "localhost";
    private static final String CHARSET = "charset";
    private static final String ACCEPT_HEADER_NAME = "Accept";
    private static final String SSL = "SSL";
    private static final String MULTIPART = "multipart";
    private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH = MULTIPART + "/";
    private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS = MULTIPART + "+";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEMPLATE_START = "{";
    private static final String TEMPLATE_END = "}";
    private static final Pattern PATH_TEMPLATE = Pattern.compile(".*\\{\\w+\\}.*");
    private static final Pattern PATH_PLACEHOLDER = Pattern.compile("\\{([^/]*?)\\}");
    private static final Pattern QUERY_PLACEHOLDER = Pattern.compile("\\{([^&]*?)\\}");
    private static final String BOUNDARY_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

    private String baseUri;
    private String path = "";
    private String method;
    private String basePath;
    // The key is the placeholder name and the value the (serialized) path parameter value.
    // If the key is null it means that it's a redundant path param that cannot be mapped to a placeholder.
    // If the value is null it means that the parameter has been removed (but we keep it to retain order).
    private List<Entry<String, String>> unnamedPathParamsTuples = new ArrayList<>();
    private AuthenticationScheme defaultAuthScheme;
    private int port;
    // The parameter maps hold Strings, NoParameterValue instances and lists of these
    private Map<String, Object> requestParameters = new LinkedHashMap<>();
    private Map<String, Object> queryParameters = new LinkedHashMap<>();
    private Map<String, Object> formParameters = new LinkedHashMap<>();
    private Map<String, Object> namedPathParameters = new LinkedHashMap<>();
    private RedirectSettings.Overrides redirects = new RedirectSettings.Overrides();
    private AuthenticationScheme authenticationScheme = new NoAuthScheme();
    private FilterableResponseSpecification responseSpecification;
    private Headers requestHeaders = new Headers(new ArrayList<>());
    private Cookies cookies = new Cookies(new ArrayList<>());
    private Object requestBody;
    private List<Filter> filters = new ArrayList<>();
    private boolean urlEncodingEnabled;
    private RestAssuredConfig restAssuredConfig;
    private List<MultiPartInternal> multiParts = new ArrayList<>();
    private ParameterUpdater parameterUpdater = new ParameterUpdater(this::serializeIfNeeded);
    private ProxySpecification proxySpecification = null;

    private LogRepository logRepository;

    private boolean allowContentType;

    private boolean addCsrfFilter;

    // Starts a request with the defaults of the client whose given() created this specification. Never merged.
    private final Supplier<RequestSpecification> newRequestOfClient;

    // The defaults of then() of the client whose given() created this specification. Never merged.
    private final Supplier<ResponseDefaults> responseDefaults;

    // Set on this specification, directly or by merging a specification that set them
    private final Set<MergeableValue> explicitlySetValues = EnumSet.noneOf(MergeableValue.class);

    public RequestSpecificationImpl(String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme, List<Filter> filters,
                                    RequestSpecification defaultSpec, boolean urlEncode, RestAssuredConfig restAssuredConfig, LogRepository logRepository,
                                    ProxySpecification proxySpecification, boolean allowContentType, boolean addCsrfFilter) {
        this(baseURI, requestPort, basePath, defaultAuthScheme, filters, defaultSpec, urlEncode, restAssuredConfig, logRepository, proxySpecification,
                allowContentType, addCsrfFilter, RestAssured::given, ResponseDefaults::ofStaticFields);
    }

    /**
     * @param newRequestOfClient Starts a request with the defaults of the client whose <code>given()</code> creates this
     *                           specification, see {@link #newRequestOfClient()}
     * @param responseDefaults   Gives the defaults of <code>then()</code> of the client whose <code>given()</code> creates
     *                           this specification when it's called
     */
    public RequestSpecificationImpl(String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme, List<Filter> filters,
                                    RequestSpecification defaultSpec, boolean urlEncode, RestAssuredConfig restAssuredConfig, LogRepository logRepository,
                                    ProxySpecification proxySpecification, boolean allowContentType, boolean addCsrfFilter,
                                    Supplier<RequestSpecification> newRequestOfClient, Supplier<ResponseDefaults> responseDefaults) {
        notNull(baseURI, "baseURI");
        notNull(basePath, "basePath");
        notNull(defaultAuthScheme, "defaultAuthScheme");
        notNull(filters, "Filters");
        this.baseUri = baseURI;
        this.basePath = basePath;
        this.defaultAuthScheme = defaultAuthScheme;
        this.filters.addAll(filters);
        this.urlEncodingEnabled = urlEncode;
        port(requestPort);
        this.restAssuredConfig = restAssuredConfig;
        if (defaultSpec != null) {
            spec(defaultSpec);
        }
        this.logRepository = logRepository;
        this.proxySpecification = proxySpecification;
        this.allowContentType = allowContentType;
        this.addCsrfFilter = addCsrfFilter;
        this.newRequestOfClient = notNull(newRequestOfClient, "newRequestOfClient");
        this.responseDefaults = notNull(responseDefaults, "responseDefaults");
        // The values given to the constructor, and those of the default specification, are defaults
        explicitlySetValues.clear();
    }

    // A copy that can be changed without changing other. The values that aren't copied are immutable or replaced, never
    // changed, such as the headers and cookies. The response specification is set by the caller.
    private RequestSpecificationImpl(RequestSpecificationImpl other) {
        this.baseUri = other.baseUri;
        this.path = other.path;
        this.method = other.method;
        this.basePath = other.basePath;
        this.unnamedPathParamsTuples = new ArrayList<>(other.unnamedPathParamsTuples);
        this.defaultAuthScheme = other.defaultAuthScheme;
        this.port = other.port;
        this.requestParameters = copyParameters(other.requestParameters);
        this.queryParameters = copyParameters(other.queryParameters);
        this.formParameters = copyParameters(other.formParameters);
        this.namedPathParameters = copyParameters(other.namedPathParameters);
        this.redirects = other.redirects.copy();
        this.authenticationScheme = other.authenticationScheme;
        this.requestHeaders = other.requestHeaders;
        this.cookies = other.cookies;
        this.requestBody = other.requestBody;
        this.filters = new ArrayList<>(other.filters);
        this.urlEncodingEnabled = other.urlEncodingEnabled;
        this.restAssuredConfig = other.restAssuredConfig;
        this.multiParts = new ArrayList<>(other.multiParts);
        this.proxySpecification = other.proxySpecification;
        this.logRepository = other.logRepository == null ? null : new LogRepository(other.logRepository);
        this.allowContentType = other.allowContentType;
        this.addCsrfFilter = other.addCsrfFilter;
        this.newRequestOfClient = other.newRequestOfClient;
        this.responseDefaults = other.responseDefaults;
        this.explicitlySetValues.addAll(other.explicitlySetValues);
    }

    /**
     * @return A copy of <code>specification</code> that can be changed without changing it, without its response
     * specification, for a {@link io.restassured.RestAssuredClient} to hold
     * @throws IllegalArgumentException If it isn't REST Assured's own implementation
     */
    public static RequestSpecificationImpl copyOf(RequestSpecification specification) {
        return new RequestSpecificationImpl(asImpl(specification, "use"));
    }

    private static RequestSpecificationImpl asImpl(RequestSpecification specification, String action) {
        if (!(specification instanceof RequestSpecificationImpl)) {
            throw new IllegalArgumentException("Cannot " + action + " a request specification of type " + specification.getClass().getName()
                    + ", it must be of type " + RequestSpecificationImpl.class.getName() + ".");
        }
        return (RequestSpecificationImpl) specification;
    }

    /**
     * @return A copy of the parameters, with the values that are lists copied too, since {@link ParameterUpdater} adds to them
     */
    public static Map<String, Object> copyParameters(Map<String, ?> parameters) {
        Map<String, Object> copy = new LinkedHashMap<>();
        parameters.forEach((name, value) -> copy.put(name, value instanceof Collection ? new ArrayList<>((Collection<?>) value) : value));
        return copy;
    }

    /**
     * @return A copy of this specification to send, with a copy of its response specification. Sending changes the
     * copies only, so this specification and its response specification can be sent again, and from many threads.
     */
    private RequestSpecificationImpl copyForSending() {
        RequestSpecificationImpl request = new RequestSpecificationImpl(this);
        ResponseSpecificationImpl response = new ResponseSpecificationImpl((ResponseSpecificationImpl) responseSpecification);
        request.responseSpecification = response;
        response.setRequestSpecification(request);
        response.setLogRepository(request.logRepository);
        // Each config type that the request configures replaces the one of the response specification's config, which is
        // RestAssured.config unless it's from the request already. The response specification's own config
        // (ResponseSpecBuilder.setConfig(..)) is laid over this when validating.
        response.setConfig(SpecificationMerger.mergeConfig(response.getConfig(), request.restAssuredConfig()));
        return request;
    }

    /**
     * @return A copy of this specification that sends with <code>responseSpecification</code>, for
     * {@link TestSpecificationImpl}
     */
    RequestSpecificationImpl pairedWith(ResponseSpecification responseSpecification) {
        RequestSpecificationImpl request = new RequestSpecificationImpl(this);
        request.responseSpecification = (FilterableResponseSpecification) responseSpecification;
        return request;
    }

    @Override
    public RequestSpecification when() {
        return this;
    }

    @Override
    public RequestSpecification given() {
        return this;
    }

    @Override
    public RequestSpecification that() {
        return this;
    }

    @Override
    public ResponseSpecification response() {
        assertNotBuiltByRequestSpecBuilder();
        return responseSpecification;
    }

    @Override
    public Response get(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(GET, path, pathParams));
    }

    @Override
    public Response post(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(POST, path, pathParams));
    }

    @Override
    public Response put(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(PUT, path, pathParams));
    }

    @Override
    public Response delete(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(DELETE, path, pathParams));
    }

    @Override
    public Response head(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(HEAD, path, pathParams));
    }

    @Override
    public Response patch(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(PATCH, path, pathParams));
    }

    @Override
    public Response options(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(OPTIONS, path, pathParams));
    }

    @Override
    public Response query(String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(QUERY, path, pathParams));
    }

    @Override
    public Response get(URI uri) {
        return get(notNull(uri, "URI").toString());
    }

    @Override
    public Response post(URI uri) {
        return post(notNull(uri, "URI").toString());
    }

    @Override
    public Response put(URI uri) {
        return put(notNull(uri, "URI").toString());
    }

    @Override
    public Response delete(URI uri) {
        return delete(notNull(uri, "URI").toString());
    }

    @Override
    public Response head(URI uri) {
        return head(notNull(uri, "URI").toString());
    }

    @Override
    public Response patch(URI uri) {
        return patch(notNull(uri, "URI").toString());
    }

    @Override
    public Response options(URI uri) {
        return options(notNull(uri, "URI").toString());
    }

    @Override
    public Response query(URI uri) {
        return query(notNull(uri, "URI").toString());
    }

    @Override
    public Response get(URL url) {
        return get(notNull(url, "URL").toString());
    }

    @Override
    public Response post(URL url) {
        return post(notNull(url, "URL").toString());
    }

    @Override
    public Response put(URL url) {
        return put(notNull(url, "URL").toString());
    }

    @Override
    public Response delete(URL url) {
        return delete(notNull(url, "URL").toString());
    }

    @Override
    public Response head(URL url) {
        return head(notNull(url, "URL").toString());
    }

    @Override
    public Response patch(URL url) {
        return patch(notNull(url, "URL").toString());
    }

    @Override
    public Response options(URL url) {
        return options(notNull(url, "URL").toString());
    }

    @Override
    public Response query(URL url) {
        return query(notNull(url, "URL").toString());
    }

    @Override
    public Response get() {
        return get("");
    }

    @Override
    public Response post() {
        return post("");
    }

    @Override
    public Response put() {
        return put("");
    }

    @Override
    public Response delete() {
        return delete("");
    }

    @Override
    public Response head() {
        return head("");
    }

    @Override
    public Response patch() {
        return patch("");
    }

    @Override
    public Response options() {
        return options("");
    }

    @Override
    public Response query() {
        return query("");
    }

    @Override
    public Response request(Method method) {
        return request(notNull(method, Method.class).name());
    }

    @Override
    public Response request(String method) {
        return request(method, "");
    }

    @Override
    public Response request(Method method, String path, Object... pathParams) {
        return request(notNull(method, Method.class).name(), path, pathParams);
    }

    @Override
    public Response request(String method, String path, Object... pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> applyPathParamsAndSendRequest(method, path, pathParams));
    }

    // Quirk kept from the Groovy implementation: "URI.class" resolved to getURI().class there, so a null URI is reported
    // as "String cannot be null" by the request(..) and proxy(URI) methods.
    @Override
    public Response request(Method method, URI uri) {
        return request(method, notNull(uri, String.class).toString());
    }

    @Override
    public Response request(Method method, URL url) {
        return request(method, notNull(url, URL.class).toString());
    }

    @Override
    public Response request(String method, URI uri) {
        return request(method, notNull(uri, String.class).toString());
    }

    @Override
    public Response request(String method, URL url) {
        return request(method, notNull(url, URL.class).toString());
    }

    @Override
    public Response get(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(GET, path, pathParamsMap);
    }

    @Override
    public Response post(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(POST, path, pathParamsMap);
    }

    @Override
    public Response put(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(PUT, path, pathParamsMap);
    }

    @Override
    public Response delete(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(DELETE, path, pathParamsMap);
    }

    @Override
    public Response head(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(HEAD, path, pathParamsMap);
    }

    @Override
    public Response patch(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(PATCH, path, pathParamsMap);
    }

    @Override
    public Response options(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(OPTIONS, path, pathParamsMap);
    }

    @Override
    public Response query(String path, Map<String, ?> pathParamsMap) {
        return sendWithPathParams(QUERY, path, pathParamsMap);
    }

    @Override
    public RequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        notNull(firstParameterName, "firstParameterName");
        notNull(firstParameterValue, "firstParameterValue");
        return params(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs));
    }

    @Override
    public RequestSpecification params(Map<String, ?> parametersMap) {
        notNull(parametersMap, "parametersMap");
        parameterUpdater.updateParameters(restAssuredConfig().getParamConfig().requestParamsUpdateStrategy(), objects(parametersMap), requestParameters);
        return this;
    }

    @Override
    public RequestSpecification param(String parameterName, Object... parameterValues) {
        notNull(parameterName, "parameterName");
        parameterUpdater.updateZeroToManyParameters(restAssuredConfig().getParamConfig().requestParamsUpdateStrategy(), requestParameters, parameterName, parameterValues);
        return this;
    }

    @Override
    public FilterableRequestSpecification removeParam(String parameterName) {
        notNull(parameterName, "parameterName");
        requestParameters.remove(parameterName);
        return this;
    }

    @Override
    public RequestSpecification param(String parameterName, Collection<?> parameterValues) {
        notNull(parameterValues, "parameterValues");
        return param(parameterName, parameterValues.toArray());
    }

    @Override
    public RequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
        notNull(parameterName, "parameterName");
        notNull(parameterValues, "parameterValues");
        parameterUpdater.updateCollectionParameter(restAssuredConfig().getParamConfig().queryParamsUpdateStrategy(), queryParameters, parameterName, objects(parameterValues));
        return this;
    }

    @Override
    public FilterableRequestSpecification removeQueryParam(String parameterName) {
        notNull(parameterName, "parameterName");
        queryParameters.remove(parameterName);
        return this;
    }

    @Override
    public FilterableRequestSpecification removeHeader(String headerName) {
        notNull(headerName, "headerName");
        List<Header> headersLeftAfterRemove = new ArrayList<>();
        for (Header header : getHeaders()) {
            if (!headerName.equalsIgnoreCase(header.getName())) {
                headersLeftAfterRemove.add(header);
            }
        }
        this.requestHeaders = new Headers(headersLeftAfterRemove);
        return this;
    }

    @Override
    public FilterableRequestSpecification removeCookie(String cookieName) {
        notNull(cookieName, "cookieName");
        this.cookies = new Cookies(cookiesWithOtherNameThan(cookieName));
        return this;
    }

    @Override
    public FilterableRequestSpecification removeCookie(Cookie cookie) {
        notNull(cookie, "cookie");
        removeCookie(cookie.getName());
        return this;
    }

    @Override
    public FilterableRequestSpecification replaceHeader(String headerName, String newValue) {
        notNull(headerName, "headerName");
        removeHeader(headerName);
        List<Header> headerList = new ArrayList<>(this.requestHeaders.asList());
        headerList.add(new Header(headerName, newValue));
        this.requestHeaders = new Headers(headerList);
        return this;
    }

    @Override
    public FilterableRequestSpecification replaceCookie(String cookieName, String value) {
        notNull(cookieName, "cookieName");
        removeCookie(cookieName);
        cookie(cookieName, value);
        return this;
    }

    @Override
    public FilterableRequestSpecification replaceCookie(Cookie cookie) {
        notNull(cookie, "cookie");
        removeCookie(cookie.getName());
        this.cookie(cookie);
        return this;
    }

    @Override
    public FilterableRequestSpecification replaceHeaders(Headers headers) {
        notNull(headers, "headers");
        this.requestHeaders = new Headers(headers.asList());
        return this;
    }

    @Override
    public FilterableRequestSpecification replaceCookies(Cookies cookies) {
        notNull(cookies, "cookies");
        this.cookies = new Cookies(cookies.asList());
        return this;
    }

    @Override
    public FilterableRequestSpecification removeHeaders() {
        this.requestHeaders = new Headers(new ArrayList<>());
        return this;
    }

    @Override
    public FilterableRequestSpecification removeCookies() {
        this.cookies = new Cookies(new ArrayList<>());
        return this;
    }

    @Override
    public RequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        notNull(firstParameterName, "firstParameterName");
        notNull(firstParameterValue, "firstParameterValue");
        return queryParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs));
    }

    @Override
    public RequestSpecification queryParams(Map<String, ?> parametersMap) {
        notNull(parametersMap, "parametersMap");
        parameterUpdater.updateParameters(restAssuredConfig().getParamConfig().queryParamsUpdateStrategy(), objects(parametersMap), queryParameters);
        return this;
    }

    @Override
    public RequestSpecification queryParam(String parameterName, Object... parameterValues) {
        notNull(parameterName, "parameterName");
        parameterUpdater.updateZeroToManyParameters(restAssuredConfig().getParamConfig().queryParamsUpdateStrategy(), queryParameters, parameterName, parameterValues);
        return this;
    }

    @Override
    public RequestSpecification formParam(String parameterName, Collection<?> parameterValues) {
        notNull(parameterName, "parameterName");
        notNull(parameterValues, "parameterValues");
        parameterUpdater.updateCollectionParameter(restAssuredConfig().getParamConfig().formParamsUpdateStrategy(), formParameters, parameterName, objects(parameterValues));
        return this;
    }

    @Override
    public FilterableRequestSpecification removeFormParam(String parameterName) {
        notNull(parameterName, "parameterName");
        formParameters.remove(parameterName);
        return this;
    }

    @Override
    public RequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        notNull(firstParameterName, "firstParameterName");
        notNull(firstParameterValue, "firstParameterValue");
        return formParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs));
    }

    @Override
    public RequestSpecification formParams(Map<String, ?> parametersMap) {
        notNull(parametersMap, "parametersMap");
        parameterUpdater.updateParameters(restAssuredConfig().getParamConfig().formParamsUpdateStrategy(), objects(parametersMap), formParameters);
        return this;
    }

    @Override
    public RequestSpecification formParam(String parameterName, Object... additionalParameterValues) {
        notNull(parameterName, "parameterName");
        parameterUpdater.updateZeroToManyParameters(restAssuredConfig().getParamConfig().formParamsUpdateStrategy(), formParameters, parameterName, additionalParameterValues);
        return this;
    }

    @Override
    public RequestSpecification urlEncodingEnabled(boolean isEnabled) {
        this.urlEncodingEnabled = isEnabled;
        explicitlySetValues.add(MergeableValue.URL_ENCODING_ENABLED);
        return this;
    }

    @Override
    public RequestSpecification pathParam(String parameterName, Object parameterValue) {
        notNull(parameterName, "parameterName");
        notNull(parameterValue, "parameterValue");
        parameterUpdater.updateStandardParameter(REPLACE, namedPathParameters, parameterName, parameterValue);
        return this;
    }

    @Override
    public RequestSpecification pathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        notNull(firstParameterName, "firstParameterName");
        notNull(firstParameterValue, "firstParameterValue");
        return pathParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs));
    }

    @Override
    public RequestSpecification pathParams(Map<String, ?> parameterNameValuePairs) {
        notNull(parameterNameValuePairs, "parameterNameValuePairs");
        parameterUpdater.updateParameters(REPLACE, objects(parameterNameValuePairs), namedPathParameters);
        return this;
    }

    @Override
    public FilterableRequestSpecification removePathParam(String parameterName) {
        notNull(parameterName, "parameterName");
        removeNamedPathParam(parameterName);
        removeUnnamedPathParam(parameterName);
        return this;
    }

    @Override
    public FilterableRequestSpecification removeNamedPathParam(String parameterName) {
        notNull(parameterName, "parameterName");
        namedPathParameters.remove(parameterName);
        return this;
    }

    @Override
    public FilterableRequestSpecification removeUnnamedPathParam(String parameterName) {
        notNull(parameterName, "parameterName");
        for (int i = 0; i < unnamedPathParamsTuples.size(); i++) {
            if (parameterName.equals(unnamedPathParamsTuples.get(i).getKey())) {
                removeUnnamedPathParamAtIndex(i);
                break;
            }
        }
        return this;
    }

    @Override
    public FilterableRequestSpecification removeUnnamedPathParamByValue(String parameterValue) {
        notNull(parameterValue, "parameterValue");
        for (int i = 0; i < unnamedPathParamsTuples.size(); i++) {
            if (parameterValue.equals(unnamedPathParamsTuples.get(i).getValue())) {
                removeUnnamedPathParamAtIndex(i);
                break;
            }
        }
        return this;
    }

    @Override
    public RequestSpecification config(RestAssuredConfig config) {
        this.restAssuredConfig = config;
        if (responseSpecification != null) {
            ((ResponseSpecificationImpl) responseSpecification).setConfig(config);
        }
        return this;
    }

    @Override
    public RequestSpecification keyStore(String pathToJks, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password));
        return this;
    }

    @Override
    public RequestSpecification keyStore(File pathToJks, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password));
        return this;
    }

    @Override
    public RequestSpecification trustStore(String path, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password));
        return this;
    }

    @Override
    public RequestSpecification trustStore(File path, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password));
        return this;
    }

    @Override
    public RequestSpecification trustStore(KeyStore trustStore) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(trustStore));
        return this;
    }

    @Override
    public RequestSpecification keyStore(KeyStore keyStore) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(keyStore));
        return this;
    }

    @Override
    public RequestSpecification relaxedHTTPSValidation() {
        return relaxedHTTPSValidation(SSL);
    }

    @Override
    public RequestSpecification relaxedHTTPSValidation(String protocol) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.relaxedHTTPSValidation(protocol));
        return this;
    }

    @Override
    public RequestSpecification filter(Filter filter) {
        notNull(filter, "Filter");
        filters.add(filter);
        return this;
    }

    @Override
    public RequestSpecification filters(List<Filter> filters) {
        notNull(filters, "Filters");
        this.filters.addAll(filters);
        return this;
    }

    @Override
    public RequestSpecification filters(Filter filter, Filter... additionalFilter) {
        notNull(filter, "Filter");
        this.filters.add(filter);
        if (additionalFilter != null) {
            Collections.addAll(this.filters, additionalFilter);
        }
        return this;
    }

    @Override
    public RequestLogSpecification log() {
        RequestLogSpecificationImpl logSpecification = new RequestLogSpecificationImpl();
        logSpecification.setRequestSpecification(this);
        logSpecification.setLogRepository(logRepository);
        return logSpecification;
    }

    @Override
    public RequestSpecification and() {
        return this;
    }

    @Override
    public RequestSpecification request() {
        return this;
    }

    @Override
    public RequestSpecification with() {
        return this;
    }

    @Override
    public ResponseSpecification then() {
        assertNotBuiltByRequestSpecBuilder();
        return responseSpecification;
    }

    @Override
    public ResponseSpecification expect() {
        assertNotBuiltByRequestSpecBuilder();
        return responseSpecification;
    }

    @Override
    public AuthenticationSpecification auth() {
        return new AuthenticationSpecificationImpl(this);
    }

    @Override
    public RequestSpecification csrf(String csrfTokenPath) {
        this.restAssuredConfig = restAssuredConfig().csrfConfig(csrfConfig().csrfTokenPath(csrfTokenPath));
        return this;
    }

    @Override
    public RequestSpecification csrf(String csrfTokenPath, String csrfInputFieldName) {
        this.restAssuredConfig = restAssuredConfig().csrfConfig(csrfConfig().csrfTokenPath(csrfTokenPath).csrfInputFieldName(csrfInputFieldName));
        return this;
    }

    @Override
    public RequestSpecification disableCsrf() {
        setAddCsrfFilter(false);
        return noFiltersOfType(CsrfFilter.class);
    }

    public AuthenticationSpecification authentication() {
        return auth();
    }

    @Override
    public RequestSpecification port(int port) {
        if (port < 1 && port != RestAssured.UNDEFINED_PORT) {
            throw new IllegalArgumentException("Port must be greater than 0");
        }
        setRequestPort(port);
        return this;
    }

    @Override
    public RequestSpecification body(String body) {
        notNull(body, "body");
        setRequestBody(body);
        return this;
    }

    @Override
    public RequestSpecification baseUri(String baseUri) {
        notNull(baseUri, "Base URI");
        setBaseUri(baseUri);
        return this;
    }

    @Override
    public RequestSpecification basePath(String basePath) {
        notNull(basePath, "Base Path");
        setBasePath(basePath);
        return this;
    }

    @Override
    public RequestSpecification proxy(String host, int port) {
        return proxy(ProxySpecification.host(host).withPort(port));
    }

    @Override
    public RequestSpecification proxy(String host) {
        if (UriValidator.isUri(host)) {
            return proxy(toURI(host));
        } else {
            return proxy(ProxySpecification.host(host));
        }
    }

    @Override
    public RequestSpecification proxy(int port) {
        return proxy(ProxySpecification.port(port));
    }

    @Override
    public RequestSpecification proxy(String host, int port, String scheme) {
        // An IPv6 address is bracketed, as proxy(URI) has it, and any other host is kept as it is, even one that a URI
        // doesn't allow, such as my_proxy
        boolean ipv6Address = host != null && host.contains(":") && !host.startsWith("[");
        return proxy(new ProxySpecification(ipv6Address ? "[" + host + "]" : host, port, scheme));
    }

    @Override
    public RequestSpecification proxy(URI uri) {
        notNull(uri, String.class);
        return proxy(new ProxySpecification(uri.getHost(), uri.getPort(), uri.getScheme()));
    }

    @Override
    public RequestSpecification proxy(ProxySpecification proxySpecification) {
        notNull(proxySpecification, ProxySpecification.class);
        setProxySpecification(proxySpecification);
        return this;
    }

    @Override
    public RequestSpecification body(byte[] body) {
        notNull(body, "body");
        setRequestBody(body);
        return this;
    }

    @Override
    public RequestSpecification body(File body) {
        notNull(body, "body");
        setRequestBody(body);
        return this;
    }

    @Override
    public RequestSpecification body(InputStream body) {
        notNull(body, "body");
        setRequestBody(body);
        return this;
    }

    @Override
    public RequestSpecification body(Object object) {
        notNull(object, "object");
        if (!isSerializableCandidate(object)) {
            return body(object.toString());
        }

        setRequestBody(ObjectMapping.serialize(object, getRequestContentType(), findEncoderCharsetOrReturnDefault(getRequestContentType()), null, objectMappingConfig(), restAssuredConfig().getEncoderConfig()));
        return this;
    }

    @Override
    public RequestSpecification body(Object object, ObjectMapper mapper) {
        notNull(object, "object");
        notNull(mapper, "Object mapper");
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setObject(object);
        ctx.setCharset(findEncoderCharsetOrReturnDefault(getRequestContentType()));
        ctx.setContentType(getRequestContentType());
        setRequestBody(mapper.serialize(ctx));
        return this;
    }

    @Override
    public RequestSpecification body(Object object, ObjectMapperType mapperType) {
        notNull(object, "object");
        notNull(mapperType, "Object mapper type");
        setRequestBody(ObjectMapping.serialize(object, getRequestContentType(), findEncoderCharsetOrReturnDefault(getRequestContentType()), mapperType, objectMappingConfig(), restAssuredConfig().getEncoderConfig()));
        return this;
    }

    @Override
    public RequestSpecification contentType(ContentType contentType) {
        notNull(contentType, ContentType.class);
        setAllowContentType(true);
        return header(CONTENT_TYPE, contentType);
    }

    @Override
    public RequestSpecification contentType(String contentType) {
        notNull(contentType, "Content-Type header cannot be null");
        setAllowContentType(true);
        return header(CONTENT_TYPE, contentType);
    }

    @Override
    public RequestSpecification noContentType() {
        setAllowContentType(false);
        return removeHeader(CONTENT_TYPE);
    }

    @Override
    public RequestSpecification accept(ContentType contentType) {
        notNull(contentType, "Accept header");
        return accept(contentType.getAcceptHeader());
    }

    @Override
    public RequestSpecification accept(String mediaTypes) {
        notNull(mediaTypes, "Accept header media range");
        return header(ACCEPT_HEADER_NAME, mediaTypes);
    }

    @Override
    public RequestSpecification headers(Map<String, ?> headers) {
        notNull(headers, "headers");
        List<Header> headerList = new ArrayList<>();
        if (this.requestHeaders.exist()) {
            headerList.addAll(this.requestHeaders.asList());
        }
        // Groovy callers may pass a map with GString keys, which Groovy converted to String
        for (Entry<?, ?> entry : ((Map<?, ?>) headers).entrySet()) {
            String headerName = castToString(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof List) {
                for (Object val : (List<?>) value) {
                    headerList.add(new Header(headerName, serializeIfNeeded(val)));
                }
            } else {
                headerList.add(new Header(headerName, serializeIfNeeded(value)));
            }
        }
        this.requestHeaders = new Headers(removeMergedHeadersIfNeeded(headerList));
        return this;
    }

    @Override
    public RequestSpecification headers(Headers headers) {
        notNull(headers, "headers");
        if (headers.exist()) {
            List<Header> headerList = new ArrayList<>();
            if (this.requestHeaders.exist()) {
                headerList.addAll(this.requestHeaders.asList());
            }

            headerList.addAll(headers.asList());
            this.requestHeaders = new Headers(removeMergedHeadersIfNeeded(headerList));
        }
        return this;
    }

    private List<Header> removeMergedHeadersIfNeeded(List<Header> headerList) {
        List<Header> headers = new ArrayList<>();
        for (Header header : headerList) {
            String headerName = header.getName();
            if (restAssuredConfig().getHeaderConfig().shouldOverwriteHeaderWithName(headerName)) {
                headers.removeIf(it -> headerName.equalsIgnoreCase(it.getName()));
            }
            headers.add(header);
        }
        return headers;
    }

    @Override
    public RequestSpecification header(String headerName, Object headerValue, Object... additionalHeaderValues) {
        notNull(headerName, "Header name");
        notNull(headerValue, "Header value");

        List<Header> headerList = new ArrayList<>();
        headerList.add(new Header(headerName, serializeIfNeeded(headerValue)));
        if (additionalHeaderValues != null) {
            for (Object additionalHeaderValue : additionalHeaderValues) {
                headerList.add(new Header(headerName, serializeIfNeeded(additionalHeaderValue)));
            }
        }

        return headers(new Headers(headerList));
    }

    @Override
    public RequestSpecification header(Header header) {
        notNull(header, "Header");

        return headers(new Headers(Collections.singletonList(header)));
    }

    @Override
    public RequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs) {
        return headers(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstHeaderName, firstHeaderValue, headerNameValuePairs));
    }

    @Override
    public RequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
        return cookies(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstCookieName, firstCookieValue, cookieNameValuePairs));
    }

    @Override
    public RequestSpecification cookies(Map<String, ?> cookies) {
        notNull(cookies, "cookies");
        List<Cookie> cookieList = new ArrayList<>();
        if (this.cookies.exist()) {
            cookieList.addAll(this.cookies.asList());
        }
        for (Entry<?, ?> entry : ((Map<?, ?>) cookies).entrySet()) {
            cookieList.add(new Cookie.Builder(castToString(entry.getKey()), cookieMapValue(entry.getValue())).build());
        }
        this.cookies = new Cookies(cookieList);
        return this;
    }

    /**
     * A String (or GString) value is used as is and a null value means a cookie without value. Other values are serialized
     * like the value of {@link #cookie(String, Object, Object...)}; Groovy failed to create a cookie from them.
     */
    private String cookieMapValue(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        } else if (value instanceof CharSequence && !isSerializableCandidate(value)) {
            return value.toString();
        }
        return serializeIfNeeded(value);
    }

    @Override
    public RequestSpecification cookies(Cookies cookies) {
        notNull(cookies, "cookies");
        if (cookies.exist()) {
            List<Cookie> cookieList = new ArrayList<>();
            if (this.cookies.exist()) {
                cookieList.addAll(this.cookies.asList());
            }

            cookieList.addAll(cookies.asList());
            this.cookies = new Cookies(cookieList);
        }
        return this;
    }

    @Override
    public RequestSpecification cookie(String cookieName, Object value, Object... additionalValues) {
        notNull(cookieName, "Cookie name");
        List<Cookie> cookieList = new ArrayList<>();
        cookieList.add(new Cookie.Builder(cookieName, cookieValue(value)).build());
        if (additionalValues != null) {
            for (Object additionalValue : additionalValues) {
                cookieList.add(new Cookie.Builder(cookieName, cookieValue(additionalValue)).build());
            }
        }

        return cookies(new Cookies(cookieList));
    }

    // A null value means a cookie without value
    private String cookieValue(Object value) {
        return value == null ? null : serializeIfNeeded(value);
    }

    @Override
    public RequestSpecification cookie(Cookie cookie) {
        notNull(cookie, "Cookie");
        return cookies(new Cookies(Collections.singletonList(cookie)));
    }

    @Override
    public RequestSpecification cookie(String cookieName) {
        return cookie(cookieName, null);
    }

    @Override
    public RedirectSpecification redirects() {
        return new RedirectSpecificationImpl(this, redirects);
    }

    @Override
    public RequestSpecification spec(RequestSpecification requestSpecificationToMerge) {
        SpecificationMerger.merge(this, requestSpecificationToMerge == null ? null : asImpl(requestSpecificationToMerge, "merge"));
        return this;
    }

    public RequestSpecification specification(RequestSpecification requestSpecificationToMerge) {
        return spec(requestSpecificationToMerge);
    }

    @Override
    public RequestSpecification sessionId(String sessionIdValue) {
        String sessionIdName = getConfig() == null ? SessionConfig.DEFAULT_SESSION_ID_NAME : getConfig().getSessionConfig().sessionIdName();
        return sessionId(sessionIdName, sessionIdValue);
    }

    @Override
    public RequestSpecification sessionId(String sessionIdName, String sessionIdValue) {
        notNull(sessionIdName, "Session id name");
        notNull(sessionIdValue, "Session id value");
        if (cookies.hasCookieWithName(sessionIdName)) {
            List<Cookie> allOtherCookies = cookiesWithOtherNameThan(sessionIdName);
            allOtherCookies.add(new Cookie.Builder(sessionIdName, sessionIdValue).build());
            this.cookies = new Cookies(allOtherCookies);
        } else {
            cookie(sessionIdName, sessionIdValue);
        }
        return this;
    }

    private List<Cookie> cookiesWithOtherNameThan(String cookieName) {
        List<Cookie> cookiesWithOtherName = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (!cookieName.equalsIgnoreCase(cookie.getName())) {
                cookiesWithOtherName.add(cookie);
            }
        }
        return cookiesWithOtherName;
    }

    @Override
    public RequestSpecification multiPart(MultiPartSpecification multiPartSpec) {
        notNull(multiPartSpec, "Multi-part specification");
        String mimeType = multiPartSpec.getMimeType();
        Object content;
        if (multiPartSpec.getContent() instanceof File || multiPartSpec.getContent() instanceof InputStream || multiPartSpec.getContent() instanceof byte[]) {
            content = multiPartSpec.getContent();
        } else {
            // Objects ought to be serialized
            if (mimeType == null) {
                mimeType = ANY.matches(getRequestContentType()) ? JSON.toString() : getRequestContentType();
            }
            content = serializeIfNeeded(multiPartSpec.getContent(), mimeType);
        }

        final String controlName;
        if (multiPartSpec instanceof MultiPartSpecificationImpl && !((MultiPartSpecificationImpl) multiPartSpec).isControlNameSpecifiedExplicitly()) {
            // We use the default control name if it was not explicitly specified in the multi-part spec
            controlName = restAssuredConfig().getMultiPartConfig().defaultControlName();
        } else {
            controlName = multiPartSpec.getControlName();
        }

        final String fileName;
        if (multiPartSpec instanceof MultiPartSpecificationImpl && !((MultiPartSpecificationImpl) multiPartSpec).isFileNameSpecifiedExplicitly()) {
            // We use the default file name if it was not explicitly specified in the multi-part spec
            fileName = restAssuredConfig().getMultiPartConfig().defaultFileName();
        } else {
            fileName = multiPartSpec.getFileName();
        }

        Map<String, String> headers = multiPartSpec.getHeaders();

        MultiPartInternal multiPart = new MultiPartInternal();
        multiPart.setControlName(controlName);
        multiPart.setContent(content);
        multiPart.setFileName(fileName);
        multiPart.setCharset(multiPartSpec.getCharset());
        multiPart.setMimeType(mimeType);
        multiPart.setHeaders(headers);
        multiParts.add(multiPart);
        return this;
    }

    @Override
    public RequestSpecification multiPart(String controlName, File file) {
        multiParts.add(newMultiPart(controlName, file, null, file.getName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(File file) {
        multiParts.add(newMultiPart(restAssuredConfig().getMultiPartConfig().defaultControlName(), file, null, file.getName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String controlName, File file, String mimeType) {
        multiParts.add(newMultiPart(controlName, file, mimeType, file.getName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String controlName, Object object) {
        String mimeType = ANY.matches(getRequestContentType()) ? JSON.toString() : getRequestContentType();
        // Groovy chose the overload by the runtime type of the object
        if (object instanceof File) {
            return multiPart(controlName, (File) object, mimeType);
        } else if (object instanceof String) {
            return multiPart(controlName, (String) object, mimeType);
        } else if (object instanceof byte[]) {
            return multiPart(controlName, (byte[]) object);
        }
        return multiPart(controlName, object, mimeType);
    }

    @Override
    public RequestSpecification multiPart(String controlName, Object object, String mimeType) {
        String possiblySerializedObject = serializeIfNeeded(object, mimeType);
        multiParts.add(newMultiPart(controlName, possiblySerializedObject, mimeType, restAssuredConfig().getMultiPartConfig().defaultFileName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String controlName, String filename, Object object, String mimeType) {
        String possiblySerializedObject = serializeIfNeeded(object, mimeType);
        multiParts.add(newMultiPart(controlName, possiblySerializedObject, mimeType, filename));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, byte[] bytes) {
        multiParts.add(newMultiPart(name, bytes, null, restAssuredConfig().getMultiPartConfig().defaultFileName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String fileName, byte[] bytes) {
        multiParts.add(newMultiPart(name, bytes, null, fileName));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String fileName, byte[] bytes, String mimeType) {
        multiParts.add(newMultiPart(name, bytes, mimeType, fileName));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String fileName, InputStream stream) {
        multiParts.add(newMultiPart(name, stream, null, fileName));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String fileName, InputStream stream, String mimeType) {
        multiParts.add(newMultiPart(name, stream, mimeType, fileName));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String contentBody) {
        multiParts.add(newMultiPart(name, contentBody, null, restAssuredConfig().getMultiPartConfig().defaultFileName()));
        return this;
    }

    public RequestSpecification multiPart(String name, NoParameterValue contentBody) {
        multiParts.add(newMultiPart(name, contentBody, null, restAssuredConfig().getMultiPartConfig().defaultFileName()));
        return this;
    }

    @Override
    public RequestSpecification multiPart(String name, String contentBody, String mimeType) {
        multiParts.add(newMultiPart(name, contentBody, mimeType, restAssuredConfig().getMultiPartConfig().defaultFileName()));
        return this;
    }

    private static MultiPartInternal newMultiPart(String controlName, Object content, String mimeType, String fileName) {
        MultiPartInternal multiPart = new MultiPartInternal();
        multiPart.setControlName(controlName);
        multiPart.setContent(content);
        if (mimeType != null) {
            multiPart.setMimeType(mimeType);
        }
        multiPart.setFileName(fileName);
        return multiPart;
    }

    /**
     * Creates the filter context for the next filter. Called by {@link FilterContextImpl}.
     */
    public FilterContextImpl newFilterContext(Object assertionClosure, Iterator<Filter> filters, Map<String, Object> properties) {
        if (path != null && path.endsWith("?")) {
            throw new IllegalArgumentException("Request URI cannot end with ?");
        }

        // Set default accept header if undefined
        if (!getHeaders().hasHeaderWithName(ACCEPT_HEADER_NAME)) {
            header(ACCEPT_HEADER_NAME, ANY.getAcceptHeader());
        }

        String tempContentType = defineRequestContentTypeAsString(method);
        if (tempContentType != null) {
            header(CONTENT_TYPE, tempContentType);
        }

        List<String> unnamedPathParamValues = definedUnnamedPathParamValues();
        String uri = partiallyApplyPathParams(path, true, unnamedPathParamValues);
        String requestUriForLogging = generateRequestUriForLogging(uri, method);

        return new FilterContextImpl(requestUriForLogging, getUserDefinedPath(), getDerivedPath(uri), uri, path, unnamedPathParamValues.toArray(), method, assertionClosure, filters, properties);
    }

    private String generateRequestUriForLogging(String uri, String method) {
        String targetUri;
        Map<String, Object> allQueryParams = new LinkedHashMap<>();

        if (uri.contains("?")) {
            String uriToUse;
            if (isFullyQualified(uri)) {
                uriToUse = uri;
            } else {
                uriToUse = getTargetPath(uri);
            }

            targetUri = substringBefore(uriToUse, "?");
            String queryParamsDefinedInPath = substringAfter(uri, "?");

            // Add query parameters defined in path to the allQueryParams map
            if (!isBlank(queryParamsDefinedInPath)) {
                String[] splittedQueryParams = split(queryParamsDefinedInPath, "&");
                for (String queryNameWithPotentialValue : splittedQueryParams) {
                    String[] splitted = split(queryNameWithPotentialValue, "=", 2);
                    boolean queryParamHasValueDefined = splitted.length > 1 || queryNameWithPotentialValue.contains("=");
                    if (queryParamHasValueDefined) {
                        // Handles the special case where the query param is defined with an empty value
                        String value = splitted.length == 1 ? "" : splitted[1];
                        allQueryParams.put(splitted[0], value);
                    } else {
                        allQueryParams.put(splitted[0], new NoParameterValue());
                    }
                }
            }
        } else {
            targetUri = uri;
        }

        try {
            URI actualUri = URIBuilder.convertToURI(assembleCompleteTargetPath(targetUri));
            URIBuilder uriBuilder = new URIBuilder(actualUri, this.urlEncodingEnabled, encoderConfig());

            if (!POST.name().equalsIgnoreCase(method) && !requestParameters.isEmpty()) {
                allQueryParams.putAll(requestParameters);
            }

            if (!queryParameters.isEmpty()) {
                allQueryParams.putAll(queryParameters);
            }

            if (GET.name().equalsIgnoreCase(method) && !formParameters.isEmpty()) {
                allQueryParams.putAll(formParameters);
            }

            if (!allQueryParams.isEmpty()) {
                uriBuilder.addQueryParams(allQueryParams);
            }

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    // Called by SendRequestFilter
    public Response sendRequest(String path, Object assertionClosure, FilterableRequestSpecification requestSpecification, Map<String, Object> filterContextProperties)
            throws IOException, URISyntaxException {
        notNull(path, "Path");
        path = extractRequestParamsIfNeeded(path);
        String method = requestSpecification.getMethod();
        String targetUri = getTargetURI(path);
        String targetPath = getTargetPath(path);

        assertCorrectNumberOfPathParams();

        ResponseSpecificationImpl responseSpecification = (ResponseSpecificationImpl) this.responseSpecification;
        ResponseSpecificationImpl.HamcrestAssertionClosure hamcrestAssertionClosure = (ResponseSpecificationImpl.HamcrestAssertionClosure) assertionClosure;
        RestAssuredConfig config = getConfig();
        RestAssuredConfig cfg = config != null ? config : new RestAssuredConfig();
        Selection client = HttpClientProviders.select(cfg.getHttpClientConfig());
        UnsupportedFeatures.verifyCustomizers(client, cfg.getHttpClientConfig().customizers());
        String defaultUserAgent = HttpClientParams.validate(cfg.getHttpClientConfig());
        EncoderConfig encoderConfig = config == null ? new EncoderConfig() : config.getEncoderConfig();
        Template template = new Template(toUri(targetUri), new StringHashMap<>(), newEncoders(encoderConfig));
        applyRestAssuredConfig(template.encoders());
        registerRestAssuredEncoders(template.encoders());
        setRequestHeaders(template.headers());

        if (cookies.exist()) {
            Map<Object, Object> httpHeaders = template.headers();
            Stream<String> cookiePairs = cookies.asList().stream().map(it -> it.getName() + "=" + (it.hasValue() ? it.getValue() : ""));
            Object cookieHeaderName = findHeaderName(httpHeaders, "Cookie");
            if (cookieHeaderName != null) {
                // A Cookie header set with header(..) goes first, in the same Cookie header
                Object headerValue = httpHeaders.remove(cookieHeaderName);
                Stream<?> headerValues = headerValue instanceof Collection ? ((Collection<?>) headerValue).stream() : Stream.of(headerValue);
                cookiePairs = Stream.concat(headerValues.map(String::valueOf), cookiePairs);
            }
            httpHeaders.put("Cookie", cookiePairs.collect(Collectors.joining("; ")));
        }

        // Allow returning a the response
        RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();
        restAssuredResponse.setLogRepository(logRepository);
        restAssuredResponse.setSessionIdName(cfg.getSessionConfig().sessionIdName());
        restAssuredResponse.setDecoderConfig(cfg.getDecoderConfig());
        restAssuredResponse.setConfig(cfg);
        restAssuredResponse.setFilterContextProperties(filterContextProperties);
        restAssuredResponse.setResponseDefaults(responseDefaults);
        responseSpecification.setRestAssuredResponse(restAssuredResponse);
        Object acceptContentType = hamcrestAssertionClosure.getResponseContentType();

        // Authenticates the request, so the request is built afterwards
        CoreAuthenticationContext authentication = new CoreAuthenticationContext(template.headers(), template.baseUri(), cfg.getOAuthConfig(), cfg.getSSLConfig());
        authenticate(authenticationScheme, authentication);
        HttpClientInstance transport = ProviderTransport.create(client, cfg, proxySpecification, authentication);
        try {
            ContentDecoding contentDecoding = new ContentDecoding(cfg.getDecoderConfig());
            RequestOptions options = ClientSettingsFactory.requestOptions(cfg.getHttpClientConfig(), authentication.ntlm());
            HttpRequest request;
            if (mayHaveBody(method)) {
                if (hasFormParams() && requestBody != null) {
                    throw new IllegalStateException("You can either send form parameters OR body content in " + method + ", not both!");
                }
                Object bodyContent = createFormParamBodyContent(assembleBodyContent(method));
                if (POST.name().equalsIgnoreCase(method)) {
                    request = newFormRequest(template, method, targetPath, queryParameters, bodyContent, acceptContentType, encoderConfig, contentDecoding, options);
                } else if (PATCH.name().equalsIgnoreCase(method)) {
                    request = newFormRequest(template, method, targetPath, mergeMapsAndRetainOrder(requestParameters, queryParameters), bodyContent,
                            acceptContentType, encoderConfig, contentDecoding, options);
                } else {
                    requestBody = bodyContent;
                    request = newRequest(template, method, targetPath, acceptContentType, encoderConfig, contentDecoding, options);
                }
            } else {
                request = newRequest(template, method, targetPath, acceptContentType, encoderConfig, contentDecoding, options);
            }

            Exchange exchange = new Exchange(transport, redirects.resolve(cfg.getRedirectConfig()),
                    authentication.credentials() == null ? null : new ChallengeHandler(authentication.credentials()),
                    authentication.oauthSigner(), keptCookies(cfg.getHttpClientConfig()), defaultUserAgent);
            Exchanged exchanged = exchange.send(request);
            restAssuredResponse.setEffectiveUri(exchanged.effectiveUri());
            RequestDispatcher.receive(exchanged.response(), contentDecoding, responseSpecification.hasBodyAssertionsDefined(), hamcrestAssertionClosure);
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        } finally {
            transport.close();
        }
        return restAssuredResponse;
    }

    /**
     * Turns the {@link AbstractMethodError} of a scheme that was compiled against REST Assured 6.x, and implements only
     * the authenticate(HTTPBuilder) method that REST Assured 7 removed, into an error that says what to do.
     */
    private static void authenticate(AuthenticationScheme scheme, AuthenticationContext context) {
        try {
            scheme.authenticate(context);
        } catch (AbstractMethodError e) {
            if (implementsAuthenticate(scheme.getClass())) {
                throw e;
            }
            throw new IllegalStateException("The authentication scheme " + scheme.getClass().getName() + " doesn't implement authenticate("
                    + AuthenticationContext.class.getName() + "), which REST Assured 7 calls instead of authenticate(HTTPBuilder). Implement "
                    + "authenticate(AuthenticationContext) and compile the scheme against REST Assured 7.", e);
        }
    }

    private static boolean implementsAuthenticate(Class<?> schemeClass) {
        try {
            return !Modifier.isAbstract(schemeClass.getMethod("authenticate", AuthenticationContext.class).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * @return The cookies that the responses of the request set, which are kept within its redirects and, with
     * {@link HttpClientConfig#reuseHttpClientInstance()}, across the requests of that config, or <code>null</code> if
     * the cookies of the responses aren't kept
     */
    private static KeptCookies keptCookies(HttpClientConfig httpClientConfig) {
        if (!httpClientConfig.keepsResponseCookies()) {
            return null;
        }
        CookieStore store = httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()
                ? HttpClientInstances.global().cookieStore(httpClientConfig) : new CookieStore();
        return new KeptCookies(store, new KeptCookieSpec(httpClientConfig.cookieDatePatterns()));
    }

    // A POST, or a PATCH with form params or multiparts, is sent URL encoded unless the request has a content type
    private HttpRequest newFormRequest(Template template, String method, String targetPath, Map<String, Object> query, Object bodyContent,
                                       Object acceptContentType, EncoderConfig encoderConfig, ContentDecoding contentDecoding,
                                       RequestOptions options) throws URISyntaxException {
        if (allowContentType) {
            String contentType = requestHeaders.getValue(CONTENT_TYPE);
            return newRequest(template, method, targetPath, query, acceptContentType, contentType == null ? URLENC.toString() : contentType, bodyContent,
                    encoderConfig, contentDecoding, options);
        }
        return newRequest(template, method, targetPath, query, ANY, null, bodyContent, encoderConfig, contentDecoding, options);
    }

    private HttpRequest newRequest(Template template, String method, String targetPath, Object acceptContentType, EncoderConfig encoderConfig,
                                   ContentDecoding contentDecoding, RequestOptions options) throws URISyntaxException {
        Map<String, Object> query = mergeMapsAndRetainOrder(requestParameters, queryParameters);
        if (method.equals(GET.name())) {
            query = mergeMapsAndRetainOrder(query, formParameters);
        }
        String requestContentType = allowContentType ? defineRequestContentTypeAsString(method) : null;
        return newRequest(template, method, targetPath, query, acceptContentType, requestContentType, requestBody, encoderConfig, contentDecoding, options);
    }

    private HttpRequest newRequest(Template template, String method, String targetPath, Map<String, Object> query, Object acceptContentType,
                                   String requestContentType, Object body, EncoderConfig encoderConfig, ContentDecoding contentDecoding,
                                   RequestOptions options) throws URISyntaxException {
        URIBuilder uri = new URIBuilder(template.baseUri(), urlEncodingEnabled, encoderConfig);
        uri.setPath(targetPath);
        uri.setQuery(query);
        // The headers of the request specification, the cookies and the headers of the authentication scheme
        return RequestDispatcher.newRequest(method, uri.toURI(), template.headers(), acceptContentType, requestContentType, body, allowContentType,
                requestHeaders.hasHeaderWithName("Accept"), template.encoders(), contentDecoding, options);
    }

    private static URI toUri(String targetUri) {
        try {
            return URIBuilder.convertToURI(targetUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // The encoders of the content types, with the content types that the encoder config maps onto another one
    private static EncoderRegistry newEncoders(EncoderConfig encoderConfig) {
        EncoderRegistry encoders = new EncoderRegistry();
        for (Entry<String, ContentType> entry : encoderConfig.contentEncoders().entrySet()) {
            encoders.putAt(entry.getKey(), encoders.getAt(entry.getValue().getContentTypeStrings()[0]));
        }
        return encoders;
    }

    /**
     * What every request of a send has in common: the URI without the path and query, the headers and the encoders
     */
    private record Template(URI baseUri, Map<Object, Object> headers, EncoderRegistry encoders) {
    }

    public void assertCorrectNumberOfPathParams() {
        // Path param size is named - (unnamed - named) since named path params may override unnamed if they target the same placeholder
        if (!getRedundantNamedPathParams().isEmpty() || !getRedundantUnnamedPathParamValues().isEmpty() || !getUndefinedPathParamPlaceholders().isEmpty()) {
            int pathParamPlaceholderSize = getPathParamPlaceholders().size();
            Map<String, String> namedPathParams = getNamedPathParams();
            int pathParamSize = namedPathParams.size();
            for (Entry<String, String> tuple : unnamedPathParamsTuples) {
                if (tuple.getValue() != null && !namedPathParams.containsKey(tuple.getValue())) {
                    pathParamSize++;
                }
            }

            Map<String, String> redundantNamedPathParams = getRedundantNamedPathParams();
            List<String> redundantUnnamedPathParamValues = getRedundantUnnamedPathParamValues();
            boolean hasRedundantNamedPathParams = !redundantNamedPathParams.isEmpty();
            boolean hasRedundantUnnamedPathParamValues = !redundantUnnamedPathParamValues.isEmpty();

            final String message;
            if (pathParamPlaceholderSize != pathParamSize) {
                message = "Invalid number of path parameters. Expected " + pathParamPlaceholderSize + ", was " + pathParamSize + ".";
            } else {
                message = "Path parameters were not correctly defined.";
            }

            String redundantMessage = "";
            if (hasRedundantNamedPathParams || hasRedundantUnnamedPathParamValues) {
                redundantMessage = " Redundant path parameters are: ";

                if (hasRedundantNamedPathParams) {
                    redundantMessage += redundantNamedPathParams.entrySet().stream().map(Object::toString).collect(Collectors.joining(", "));
                }
                if (hasRedundantNamedPathParams && hasRedundantUnnamedPathParamValues) {
                    redundantMessage += " and ";
                } else if (hasRedundantNamedPathParams) {
                    redundantMessage += ".";
                }
                if (hasRedundantUnnamedPathParamValues) {
                    redundantMessage += String.join(", ", redundantUnnamedPathParamValues) + ".";
                }
            }

            String undefinedMessage = "";
            if (!getUndefinedPathParamPlaceholders().isEmpty()) {
                undefinedMessage = " Undefined path parameters are: " + String.join(", ", getUndefinedPathParamPlaceholders()) + ".";
            }

            throw new IllegalArgumentException(message + redundantMessage + undefinedMessage);
        }
    }

    private void applyRestAssuredConfig(EncoderRegistry encoders) {
        if (restAssuredConfig != null) {
            encoders.setEncoderConfig(restAssuredConfig.getEncoderConfig());
            encoders.setObjectMapperConfig(restAssuredConfig.getObjectMapperConfig());
            applySessionConfig(restAssuredConfig.getSessionConfig());
        }
    }

    public void applySessionConfig(SessionConfig sessionConfig) {
        if (sessionConfig.isSessionIdValueDefined() && !cookies.hasCookieWithName(sessionConfig.sessionIdName())) {
            cookie(sessionConfig.sessionIdName(), sessionConfig.sessionIdValue());
        }
    }

    public Object assembleBodyContent(String httpMethod) {
        if (hasFormParams() && !GET.name().equalsIgnoreCase(httpMethod)) {
            if (POST.name().equalsIgnoreCase(httpMethod)) {
                return mergeMapsAndRetainOrder(requestParameters, formParameters);
            } else {
                return formParameters;
            }
        } else if (multiParts.isEmpty()) {
            return requestBody;
        } else {
            return new byte[0];
        }
    }

    public Map<String, Object> mergeMapsAndRetainOrder(Map<String, Object> map1, Map<String, Object> map2) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        newMap.putAll(map1);
        newMap.putAll(map2);
        return newMap;
    }

    private void setRequestHeaders(Map<Object, Object> httpHeaders) {
        for (Header header : requestHeaders) {
            String headerValue = header.getValue();
            // Headers whose names differ only in case are the same header and are sent under the name used first
            Object headerName = findHeaderName(httpHeaders, header.getName());
            if (headerName != null) {
                // Like Groovy's [current, headerValue].flatten()
                List<Object> values = new ArrayList<>();
                Object currentValue = httpHeaders.get(headerName);
                if (currentValue instanceof Collection) {
                    values.addAll((Collection<?>) currentValue);
                } else {
                    values.add(currentValue);
                }
                values.add(headerValue);
                httpHeaders.put(headerName, values);
            } else {
                httpHeaders.put(header.getName(), headerValue);
            }
        }
    }

    // Header names are case-insensitive, so the key of the header in the map may be spelled differently
    private static Object findHeaderName(Map<Object, Object> httpHeaders, String headerName) {
        return httpHeaders.keySet().stream().filter(it -> headerName.equalsIgnoreCase(String.valueOf(it))).findFirst().orElse(null);
    }

    private Object createFormParamBodyContent(Object bodyContent) {
        return bodyContent instanceof Map ? createFormParamBody(objects((Map<?, ?>) bodyContent)) : bodyContent;
    }

    private String getTargetPath(String path) {
        if (isFullyQualified(path)) {
            return toURL(path).getPath();
        }

        String baseUriPath = "";
        if (!(baseUri == null || baseUri.equals(""))) {
            URI uri = toURI(baseUri);
            baseUriPath = uri.getPath();
        }
        return mergeAndRemoveDoubleSlash(mergeAndRemoveDoubleSlash(baseUriPath, basePath), path);
    }

    private void registerRestAssuredEncoders(EncoderRegistry encoders) {
        // Multipart form-data
        if (multiParts.isEmpty()) {
            return;
        }

        if (hasFormParams()) {
            convertFormParamsToMultiPartParams();
        }

        String contentTypeAsString = getHeaders().getValue(CONTENT_TYPE);
        String ct = ContentTypeExtractor.getContentTypeWithoutCharset(contentTypeAsString);
        String ctLowerCase = ct == null ? null : ct.toLowerCase(Locale.ROOT);
        final String subType;
        if (ctLowerCase != null && ctLowerCase.startsWith(MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)) {
            subType = substringAfter(ct, MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH);
        } else if (ctLowerCase != null && ctLowerCase.contains(MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS)) {
            subType = substringBefore(substringAfter(ct, MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS), "+");
        } else {
            throw new IllegalArgumentException("Content-Type " + ct + " is not valid when using multiparts, it must start with \"" + MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH +
                    "\" or contain \"" + MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS + "\".");
        }

        String charsetFromContentType = CharsetExtractor.getCharsetFromContentType(contentTypeAsString);
        final String charsetToUse = isBlank(charsetFromContentType) ? restAssuredConfig().getMultiPartConfig().defaultCharset() : charsetFromContentType;
        String boundaryFromContentType = BoundaryExtractor.getBoundaryFromContentType(contentTypeAsString);
        // An empty boundary counts as missing (Groovy truth)
        String boundary = isEmpty(boundaryFromContentType) ? restAssuredConfig().getMultiPartConfig().defaultBoundary() : boundaryFromContentType;
        final String boundaryToUse = isEmpty(boundary) ? generateBoundary() : boundary;
        if (isEmpty(boundaryFromContentType)) {
            removeHeader(CONTENT_TYPE); // there should only be one
            contentType(contentTypeAsString + "; boundary=\"" + boundaryToUse + "\"");
        }

        final MultipartMode multipartMode = httpClientConfig().multipartMode();

        RequestBodyEncoder multiPartEncoder = (contentType, content) -> {
            MultipartWriter writer = new MultipartWriter(subType, charsetToUse, multipartMode, boundaryToUse);
            for (MultiPartInternal multiPart : multiParts) {
                writer.addPart(multiPart.getControlName(), multiPart.getContentBody(charsetToUse), multiPart.getHeaders());
            }
            return RequestBody.writer(writer::writeTo, writer.getContentLength(), writer.isRepeatable(), writer.getContentType());
        };
        encoders.putAt(ct, multiPartEncoder);
    }

    private static String generateBoundary() {
        Random rand = new Random();
        int length = rand.nextInt(11) + 30;
        StringBuilder boundary = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            boundary.append(BOUNDARY_ALPHABET.charAt(rand.nextInt(BOUNDARY_ALPHABET.length())));
        }
        return boundary.toString();
    }

    private void convertFormParamsToMultiPartParams() {
        Map<String, Object> allFormParams = mergeMapsAndRetainOrder(requestParameters, formParameters);
        for (Entry<String, Object> entry : allFormParams.entrySet()) {
            if (entry.getValue() instanceof List) {
                for (Object val : (List<?>) entry.getValue()) {
                    formParamToMultiPart(entry.getKey(), val);
                }
            } else {
                formParamToMultiPart(entry.getKey(), entry.getValue());
            }
        }
        requestParameters.clear();
        formParameters.clear();
    }

    /**
     * Picks the multiPart overload by the runtime type of the value, as Groovy did.
     */
    private void formParamToMultiPart(String name, Object value) {
        if (value instanceof File) {
            multiPart(name, (File) value);
        } else if (value instanceof NoParameterValue) {
            multiPart(name, (NoParameterValue) value);
        } else if (value instanceof String) {
            multiPart(name, (String) value);
        } else {
            multiPart(name, value);
        }
    }

    private boolean hasFormParams() {
        return !(requestParameters.isEmpty() && formParameters.isEmpty());
    }

    private boolean mayHaveBody(String method) {
        return POST.name().equals(method) || formParameters.size() > 0 || multiParts.size() > 0;
    }

    private String extractRequestParamsIfNeeded(String path) {
        if (path.contains("?")) {
            int indexOfQuestionMark = path.indexOf("?");
            String allParamAsString = path.substring(indexOfQuestionMark + 1);
            String[] keyValueParams = allParamAsString.split("&");
            for (String keyValueParam : keyValueParams) {
                String[] keyValue = split(keyValueParam, "=", 2);
                String theKey;
                Object theValue;
                if (keyValue.length < 1 || keyValue.length > 2) {
                    // The array is rendered the way Groovy renders it in a GString, e.g. [a=1, , b=2]
                    throw new IllegalArgumentException("Illegal parameters passed to REST Assured. Parameters was: " + GroovyStyleToString.toString(keyValueParams));
                } else if (keyValue.length == 1) {
                    theKey = keyValue[0];
                    theValue = keyValueParam.contains("=") ? "" : new NoParameterValue();
                } else {
                    theKey = keyValue[0];
                    theValue = keyValue[1];
                }
                queryParam(theKey, theValue);
            }
            path = path.substring(0, indexOfQuestionMark);
        }
        return path;
    }

    private String defineRequestContentTypeAsString(String method) {
        Object contentType = defineRequestContentType(method);
        return contentType == null ? null : contentType.toString();
    }

    /**
     * @return A String, a {@link ContentType} or null
     */
    private Object defineRequestContentType(String method) {
        Object contentType = getHeaders().getValue(CONTENT_TYPE);
        if (contentType == null) {
            if (multiParts.size() > 0) {
                contentType = MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH + restAssuredConfig().getMultiPartConfig().defaultSubtype();
            } else if (GET.name().equals(method) && !formParameters.isEmpty()) {
                contentType = URLENC;
            } else if (requestBody == null) {
                contentType = mayHaveBody(method) ? URLENC : null;
            } else if (requestBody instanceof byte[] || requestBody instanceof InputStream) {
                contentType = BINARY;
            } else {
                contentType = TEXT;
            }
        }

        if (shouldAppendCharsetToContentType(contentType)) {
            String charset = findEncoderCharsetOrReturnDefault(contentType.toString());
            if (contentType instanceof String) {
                contentType = contentType + "; " + CHARSET + "=" + charset;
            } else {
                contentType = ((ContentType) contentType).withCharset(charset);
            }
        }
        return contentType;
    }

    private boolean shouldAppendCharsetToContentType(Object contentType) {
        return contentType != null &&
                !(startsWith(contentType.toString(), MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH) || contains(contentType.toString(), MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS)) &&
                restAssuredConfig().getEncoderConfig().shouldAppendDefaultContentCharsetToContentTypeIfUndefined() && (
                isApplicationJsonContentTypeWithDefaultCharsetDefined(contentType) || !(containsIgnoreCase(contentType.toString(), CHARSET) || equalsIgnoreCase(contentType.toString(), APPLICATION_JSON)));
    }

    private boolean isApplicationJsonContentTypeWithDefaultCharsetDefined(Object contentType) {
        if (!startsWith(contentType.toString().toLowerCase(Locale.ROOT), APPLICATION_JSON)) {
            return false;
        }

        EncoderConfig encoderConfig = getConfig().getEncoderConfig();
        String contentTypeOrNull = encoderConfig.hasDefaultCharsetForContentType(contentType.toString()) ? encoderConfig.defaultCharsetForContentType(contentType.toString()) : null;
        return contentTypeOrNull != null && (!contentTypeOrNull.equals(StandardCharsets.UTF_8.name()) || encoderConfig.isUserConfigured());
    }

    private String getTargetURI(String path) {
        String uri;
        boolean pathHasScheme = isFullyQualified(path);
        if (pathHasScheme) {
            URL url = toURL(path);
            uri = getTargetUriFromUrl(url);
        } else if (isFullyQualified(baseUri)) {
            URL baseUriAsUrl = toURL(baseUri);
            uri = getTargetUriFromUrl(baseUriAsUrl);
        } else if (port != RestAssured.UNDEFINED_PORT) {
            uri = baseUri + ":" + port;
        } else {
            uri = String.valueOf(baseUri);
        }
        return uri;
    }

    private String getTargetUriFromUrl(URL url) {
        String protocol = url.getProtocol();
        boolean useDefaultHttps = false;
        if (this.port == RestAssured.UNDEFINED_PORT && protocol.equalsIgnoreCase("https")) {
            useDefaultHttps = true;
        }

        StringBuilder builder = new StringBuilder(protocol)
                .append("://")
                .append(url.getAuthority());

        boolean hasSpecifiedPortExplicitly = this.port != RestAssured.UNDEFINED_PORT;
        if (!hasPortDefined(url) && !useDefaultHttps) {
            if (hasSpecifiedPortExplicitly) {
                builder.append(":");
                builder.append(this.port);
            } else if (!isFullyQualified(url.toString()) || hasAuthorityEqualToLocalhost(url)) {
                builder.append(":");
                builder.append(DEFAULT_HTTP_TEST_PORT);
            }
        }
        return builder.toString();
    }

    private boolean hasAuthorityEqualToLocalhost(URL uri) {
        return uri.getAuthority().trim().equalsIgnoreCase(LOCALHOST);
    }

    private boolean hasPortDefined(URL uri) {
        return uri.getPort() != -1;
    }

    // Used for parameters, headers and cookies, where java.time values are sent as ISO-8601 and a value object serialized to
    // a quoted JSON string literal is sent unquoted. Only serialized values are unwrapped, so a quoted String such as an ETag is sent as is.
    // A null value becomes the String "null", as it did in Groovy.
    private String serializeIfNeeded(Object object) {
        return isParameterSerializableCandidate(object) ? unwrapJsonStringLiteral(serializeIfNeeded(object, getRequestContentType())) : String.valueOf(object);
    }

    private String serializeIfNeeded(Object object, String contentType) {
        return isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType, findEncoderCharsetOrReturnDefault(contentType), null, objectMappingConfig(), restAssuredConfig().getEncoderConfig()) : String.valueOf(object);
    }

    // A specification built by RequestSpecBuilder has no response specification until it's passed to
    // given(requestSpec, responseSpec). Otherwise it's meant to be merged into the specification that given() creates,
    // and sending from it directly would change it.
    private void assertNotBuiltByRequestSpecBuilder() {
        if (responseSpecification == null) {
            throw new IllegalStateException("A request specification built by RequestSpecBuilder can't send a request or hold response expectations itself. " +
                    "Use it with given().spec(specification) or given(specification), e.g. given().spec(specification).when().get(\"/path\").");
        }
    }

    // Sending works on copies, so it doesn't change this specification or its response specification (#945)
    private Response applyPathParamsAndSendRequest(String method, String path, Object... unnamedPathParams) {
        assertNotBuiltByRequestSpecBuilder();
        return copyForSending().send(method, path, unnamedPathParams);
    }

    private Response sendWithPathParams(Method method, String path, Map<String, ?> pathParams) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            assertNotBuiltByRequestSpecBuilder();
            RequestSpecificationImpl copy = copyForSending();
            copy.pathParams(pathParams);
            return copy.send(method.name(), path);
        });
    }

    private Response send(String method, String path, Object... unnamedPathParams) {
        notNull(path, "path");
        notNull(trimToNull(method), "Method");
        notNull(unnamedPathParams, "Path params");
        this.method = method.trim().toUpperCase(Locale.ROOT);
        this.path = path;
        List<Integer> nullParamIndices = new ArrayList<>();
        for (int i = 0; i < unnamedPathParams.length; i++) {
            if (unnamedPathParams[i] == null) {
                nullParamIndices.add(i);
            }
        }
        if (!nullParamIndices.isEmpty()) {
            boolean sizeOne = nullParamIndices.size() == 1;
            throw new IllegalArgumentException("Unnamed path parameter cannot be null (path parameter" + (sizeOne ? "" : "s") + " at " + (sizeOne ? "index" : "indices") + " " +
                    nullParamIndices.stream().map(String::valueOf).collect(Collectors.joining(",")) + " " + (sizeOne ? "is" : "are") + " null)");
        }

        buildUnnamedPathParameterTuples(unnamedPathParams);
        if (authenticationScheme instanceof NoAuthScheme && !(defaultAuthScheme instanceof NoAuthScheme)) {
            // Use default auth scheme
            authenticationScheme = defaultAuthScheme;
        }

        if (authenticationScheme instanceof FormAuthScheme) {
            // Form auth scheme is handled a bit differently than other auth schemes since it's implemented by a filter.
            FormAuthScheme formAuthScheme = (FormAuthScheme) authenticationScheme;
            filters.removeIf(it -> isInstanceOf(it, AuthFilter.class));
            FormAuthFilter formAuthFilter = new FormAuthFilter();
            formAuthFilter.setUserName(formAuthScheme.getUserName());
            formAuthFilter.setPassword(formAuthScheme.getPassword());
            formAuthFilter.setFormAuthConfig(formAuthScheme.getConfig());
            formAuthFilter.setSessionConfig(sessionConfig());
            formAuthFilter.setCsrfConfig(csrfConfig());
            filters.add(0, formAuthFilter);
        }

        if (addCsrfFilter) {
            CsrfFilter csrfFilter = new CsrfFilter();
            csrfFilter.setCsrfConfig(restAssuredConfig().getCsrfConfig());
            filters.add(csrfFilter);
        }
        // The log config of a response specification from ResponseSpecBuilder.setConfig(..) counts too, since logging if
        // validation fails starts with the request
        RestAssuredConfig specificationConfig = responseSpecification instanceof ResponseSpecificationImpl impl ? impl.getSpecificationConfig() : null;
        LogConfig logConfig = SpecificationMerger.mergeConfig(restAssuredConfig(), specificationConfig).getLogConfig();
        if (logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
            if (filters.stream().noneMatch(it -> isInstanceOf(it, RequestLoggingFilter.class))) {
                log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
            }
            if (filters.stream().noneMatch(it -> isInstanceOf(it, ResponseLoggingFilter.class))) {
                responseSpecification.log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
            }
        }
        restAssuredConfig = getConfig() != null ? getConfig() : new RestAssuredConfig();

        if (filters.stream().noneMatch(it -> isInstanceOf(it, ResponseLoggingFilter.class)) && responseSpecification != null && responseSpecification.getLogDetail() != null) {
            filters.add(new ResponseLoggingFilter(responseSpecification.getLogDetail(),
                    logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()));
        }

        // Sort filters by order (the sort is stable, so filters with the same order keep their order)
        List<Filter> sortedFilters = new ArrayList<>(filters);
        sortedFilters.sort(Comparator.comparingInt(RequestSpecificationImpl::getFilterOrder));
        filters = sortedFilters;

        // Add timing filter if it has not been added manually
        if (filters.stream().noneMatch(it -> isInstanceOf(it, TimingFilter.class))) {
            filters.add(new TimingFilter());
        }

        filters.add(new SendRequestFilter());
        ResponseSpecificationImpl responseSpecificationImpl = (ResponseSpecificationImpl) responseSpecification;
        FilterContextImpl ctx = newFilterContext(responseSpecificationImpl.getAssertionClosure(), filters.iterator(), new LinkedHashMap<>());
        Response response = ctx.next(this, responseSpecification);
        // So that then() also validates a response that a filter built, such as with ResponseBuilder, with the client's defaults
        if (response instanceof RestAssuredResponseOptionsImpl<?> built && built.getResponseDefaults() == null) {
            built.setResponseDefaults(responseDefaults);
        }
        responseSpecificationImpl.getAssertionClosure().validate(response);
        return response;
    }

    private Response applyPathParamsAndSendRequest(Method method, String path, Object... unnamedPathParams) {
        return applyPathParamsAndSendRequest(notNull(method, Method.class).name(), path, unnamedPathParams);
    }

    private static boolean isInstanceOf(Filter filter, Class<?> type) {
        // The filter list may contain nulls (they're skipped by the filter context)
        return filter != null && type.isAssignableFrom(filter.getClass());
    }

    public void buildUnnamedPathParameterTuples(Object... unnamedPathParameterValues) {
        if (unnamedPathParameterValues == null || unnamedPathParameterValues.length == 0) {
            this.unnamedPathParamsTuples = new ArrayList<>();
        } else {
            // Undefined placeholders since named path params have precedence over unnamed
            List<String> keys = getUndefinedPathParamPlaceholders();
            List<Entry<String, String>> list = new ArrayList<>();
            for (int i = 0; i < unnamedPathParameterValues.length; i++) {
                String val = serializeIfNeeded(unnamedPathParameterValues[i]);
                String key = i < keys.size() ? keys.get(i) : null;
                list.add(new SimpleEntry<>(key, val));
            }
            this.unnamedPathParamsTuples = list;
        }
    }

    public String partiallyApplyPathParams(String path, boolean encodePath, List<String> unnamedPathParams) {
        String host = getTargetURI(path);
        String targetPath = getTargetPath(path);

        String pathWithoutQueryParams = substringBefore(targetPath, "?");
        boolean shouldAppendSlashAfterEncoding = pathWithoutQueryParams.endsWith("/");
        // The last slash is removed later so we may need to add it again
        String queryParams = substringAfter(path, "?");

        PathParamFiller pathParamFiller = new PathParamFiller(unnamedPathParams);

        // If a path fragment contains double slash we need to replace it with something else to not mess up the path
        boolean hasPathParameterWithDoubleSlash = indexOf(pathWithoutQueryParams, DOUBLE_SLASH) != -1;

        String tempParams;
        if (hasPathParameterWithDoubleSlash) {
            tempParams = replace(pathWithoutQueryParams, DOUBLE_SLASH, "RA_double_slash__");
        } else {
            tempParams = pathWithoutQueryParams;
        }

        pathWithoutQueryParams = pathParamFiller.fill(split(tempParams, "/"), "/", encodePath);

        if (hasPathParameterWithDoubleSlash) {
            // Now get the double slash replacement back to normal double slashes
            pathWithoutQueryParams = replace(pathWithoutQueryParams, "RA_double_slash__", encode(DOUBLE_SLASH, EncodingTarget.QUERY));
        }

        if (shouldAppendSlashAfterEncoding) {
            pathWithoutQueryParams += "/";
        }

        if (PATH_TEMPLATE.matcher(queryParams).matches()) {
            // Note that we do NOT url encode query params here, that happens by UriBuilder at a later stage.
            queryParams = pathParamFiller.fill(split(queryParams, "&"), "&", false).substring(1);
            // 1 means that we remove first & since query parameters starts with ?
        }
        return host + (isEmpty(queryParams) ? pathWithoutQueryParams : pathWithoutQueryParams + "?" + queryParams);
    }

    /**
     * Replaces the placeholders of path fragments (or query parameters) with the named path parameter of the placeholder or
     * otherwise the next unnamed path parameter. The unnamed path parameters used and the usage of the named ones are shared
     * between the path and the query.
     */
    private class PathParamFiller {
        private final List<String> unnamedPathParams;
        private final int unnamedPathParamSize;
        private final Map<String, Integer> pathParamNameUsageCount = new HashMap<>();
        private int numberOfUnnamedPathParametersUsed = 0;

        PathParamFiller(List<String> unnamedPathParams) {
            this.unnamedPathParams = unnamedPathParams;
            this.unnamedPathParamSize = unnamedPathParams == null ? 0 : unnamedPathParams.size();
        }

        String fill(String[] subresources, String separator, boolean performEncode) {
            String acc = "";
            for (String subresource : subresources) {
                acc = fill(separator, performEncode, acc, subresource);
            }
            return acc;
        }

        private String fill(String separator, boolean performEncode, String acc, String subresource) {
            int indexOfStartBracket;
            int indexOfEndBracket = 0;
            while ((indexOfStartBracket = subresource.indexOf(TEMPLATE_START, indexOfEndBracket)) >= 0) {
                indexOfEndBracket = subresource.indexOf(TEMPLATE_END, indexOfStartBracket);
                if (indexOfEndBracket < 0) {
                    // An unclosed "{" is not a template, so the rest of the subresource is kept as literal text
                    break;
                }
                // 3 means "{" and "}" and at least one character
                if (subresource.length() >= 3) {
                    String pathParamValue;
                    String pathParamName = subresource.substring(indexOfStartBracket + 1, indexOfEndBracket);
                    // Get path parameter name, what's between the "{" and "}"
                    String value = findNamedPathParamValue(pathParamName, pathParamNameUsageCount);
                    if (value == null && numberOfUnnamedPathParametersUsed < unnamedPathParamSize) {
                        // A removed unnamed path parameter (null) is rendered as "null", as in Groovy
                        pathParamValue = String.valueOf(unnamedPathParams.get(numberOfUnnamedPathParametersUsed));
                        numberOfUnnamedPathParametersUsed += 1;
                    } else {
                        // We return the template again if no match found since we might be interested in partially applied path
                        pathParamValue = value == null ? TEMPLATE_START + pathParamName + TEMPLATE_END : value;
                    }

                    String pathToPrepend = "";
                    // If declared subresource has values before the first bracket then let's find it.
                    if (indexOfStartBracket != 0) {
                        pathToPrepend = subresource.substring(0, indexOfStartBracket);
                    }

                    String pathToAppend = "";
                    // If declared subresource has values after the first bracket then let's find it.
                    if (subresource.length() > indexOfEndBracket) {
                        pathToAppend = subresource.substring(indexOfEndBracket + 1);
                    }

                    // Since the value of the path parameter might be shorter than the template name we need to
                    // adjust the "indexOfEndBracket" index in case this subresource contains more templates after
                    // this value.
                    int lengthOfTemplate = length(pathParamName) + 2; // 2 because "{" and "}"
                    int lengthOfValue = length(pathParamValue);
                    if (lengthOfTemplate != lengthOfValue) {
                        if (lengthOfTemplate > lengthOfValue) {
                            indexOfEndBracket -= (lengthOfTemplate - lengthOfValue);
                        } else {
                            indexOfEndBracket += (lengthOfValue - lengthOfTemplate);
                        }
                    }

                    subresource = pathToPrepend + pathParamValue + pathToAppend;
                }
            }
            return acc + separator + (performEncode ? encode(subresource, EncodingTarget.QUERY) : subresource);
        }
    }

    private String findNamedPathParamValue(String pathParamName, Map<String, Integer> pathParamNameUsageCount) {
        Object pathParamValues = this.namedPathParameters.get(pathParamName);
        Object pathParamValue;
        if (pathParamValues instanceof Collection) {
            // Quirk kept from the Groovy implementation ("usageCount[name] = count++"): the count is stored before it's
            // incremented, so the second value is always used.
            int pathParamCount = pathParamNameUsageCount.computeIfAbsent(pathParamName, name -> 0);
            pathParamNameUsageCount.put(pathParamName, pathParamCount);
            pathParamCount++;
            pathParamValue = ((List<?>) pathParamValues).get(pathParamCount);
        } else {
            pathParamValue = pathParamValues;
        }
        return pathParamValue == null ? null : pathParamValue.toString();
    }

    private String createFormParamBody(Map<String, Object> formParams) {
        final StringBuilder body = new StringBuilder();
        for (Entry<String, Object> entry : formParams.entrySet()) {
            body.append(encode(entry.getKey(), EncodingTarget.BODY));
            if (!(entry.getValue() instanceof NoParameterValue)) {
                body.append("=").append(handleMultiValueParamsIfNeeded(entry));
            }
            body.append("&");
        }
        if (!formParams.isEmpty()) {
            body.deleteCharAt(body.length() - 1); //Delete last &
        }
        return body.toString();
    }

    private String encode(Object object, EncodingTarget encodingType) {
        String string = String.valueOf(object);
        if (urlEncodingEnabled) {
            String charset;
            if (encodingType == EncodingTarget.BODY) {
                charset = encoderConfig().defaultContentCharset();
                String contentType = getHeaders().getValue(CONTENT_TYPE);
                if (contentType != null) {
                    String tempCharset = CharsetExtractor.getCharsetFromContentType(contentType);
                    if (tempCharset != null) {
                        charset = tempCharset;
                    } else if (encoderConfig().hasDefaultCharsetForContentType(contentType)) {
                        charset = encoderConfig().defaultCharsetForContentType(contentType);
                    }
                }
            } else { // Query or path parameter
                charset = encoderConfig().defaultQueryParameterCharset();
            }
            return URIBuilder.encode(string, charset);
        } else {
            return string;
        }
    }

    private String handleMultiValueParamsIfNeeded(Entry<String, Object> entry) {
        Object value = entry.getValue();
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            String key = encode(entry.getKey(), EncodingTarget.BODY);
            final StringBuilder multiValueList = new StringBuilder();
            for (int index = 0; index < values.size(); index++) {
                multiValueList.append(encode(values.get(index), EncodingTarget.BODY));
                if (index != values.size() - 1) {
                    multiValueList.append("&").append(key).append("=");
                }
            }
            return multiValueList.toString();
        } else {
            return encode(value, EncodingTarget.BODY);
        }
    }

    public void setResponseSpecification(ResponseSpecification responseSpecification) {
        this.responseSpecification = (FilterableResponseSpecification) responseSpecification;
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Override
    public String getDerivedPath() {
        String uri = partiallyApplyPathParams(path, true, allUnnamedPathParamValues());
        return getDerivedPath(uri);
    }

    @Override
    public String getUserDefinedPath() {
        return PathSupport.getPath(path);
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getURI() {
        String uri = partiallyApplyPathParams(path, true, allUnnamedPathParamValues());
        return getURI(uri);
    }

    @Override
    public int getPort() {
        URL host = toURL(getTargetURI(path));
        return host.getPort();
    }

    @Override
    public Map<String, String> getFormParams() {
        return strings(Collections.unmodifiableMap(formParameters));
    }

    @Override
    public Map<String, String> getPathParams() {
        Map<String, String> namedPathParams = getNamedPathParams();
        Map<String, String> map = new LinkedHashMap<>(namedPathParams);
        for (Entry<String, String> unnamedPathParam : getUnnamedPathParams().entrySet()) {
            if (!namedPathParams.containsKey(unnamedPathParam.getKey())) {
                map.put(unnamedPathParam.getKey(), unnamedPathParam.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public Map<String, String> getNamedPathParams() {
        return strings(Collections.unmodifiableMap(namedPathParameters));
    }

    @Override
    public Map<String, String> getUnnamedPathParams() {
        // A null key means that it's a redundant path param without placeholder
        Map<String, String> map = new LinkedHashMap<>();
        for (Entry<String, String> tuple : unnamedPathParamsTuples) {
            if (tuple.getKey() != null) {
                map.put(tuple.getKey(), tuple.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public List<String> getUnnamedPathParamValues() {
        return Collections.unmodifiableList(unnamedPathParamsTuples == null ? Collections.emptyList() : definedUnnamedPathParamValues());
    }

    /**
     * @return The values of the unnamed path parameters that haven't been removed
     */
    private List<String> definedUnnamedPathParamValues() {
        List<String> values = new ArrayList<>();
        for (Entry<String, String> tuple : unnamedPathParamsTuples) {
            if (tuple.getValue() != null) {
                values.add(tuple.getValue());
            }
        }
        return values;
    }

    /**
     * @return The values of all unnamed path parameters, with null for a removed one
     */
    private List<String> allUnnamedPathParamValues() {
        List<String> values = new ArrayList<>();
        for (Entry<String, String> tuple : unnamedPathParamsTuples) {
            values.add(tuple.getValue());
        }
        return values;
    }

    @Override
    public Map<String, String> getRequestParams() {
        return strings(Collections.unmodifiableMap(requestParameters));
    }

    @Override
    public Map<String, String> getQueryParams() {
        return strings(Collections.unmodifiableMap(queryParameters));
    }

    @Override
    public List<MultiPartSpecification> getMultiPartParams() {
        List<MultiPartSpecification> multiPartParams = new ArrayList<>();
        for (MultiPartInternal multiPart : multiParts) {
            MultiPartSpecificationImpl multiPartSpecification = new MultiPartSpecificationImpl();
            multiPartSpecification.setContent(multiPart.getContent());
            multiPartSpecification.setCharset(multiPart.getCharset());
            multiPartSpecification.setFileName(multiPart.getFileName());
            multiPartSpecification.setMimeType(multiPart.getMimeType());
            multiPartSpecification.setControlName(multiPart.getControlName());
            multiPartSpecification.setHeaders(multiPart.getHeaders());
            multiPartParams.add(multiPartSpecification);
        }
        return multiPartParams;
    }

    @Override
    public Headers getHeaders() {
        return requestHeaders;
    }

    @Override
    public Cookies getCookies() {
        return cookies;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBody() {
        return (T) requestBody;
    }

    @Override
    public List<Filter> getDefinedFilters() {
        return Collections.unmodifiableList(filters);
    }

    @Override
    public RestAssuredConfig getConfig() {
        return restAssuredConfig;
    }

    @Override
    public ProxySpecification getProxySpecification() {
        return proxySpecification;
    }

    @Override
    public FilterableRequestSpecification path(String path) {
        notNull(path, "Path");
        this.path = trimToEmpty(path);
        return this;
    }

    @Override
    public List<String> getUndefinedPathParamPlaceholders() {
        String uri = partiallyApplyPathParams(path, false, allUnnamedPathParamValues());
        return getPlaceholders(uri);
    }

    @Override
    public List<String> getPathParamPlaceholders() {
        String uri = getTargetPath(contains(path, "://") ? substringAfter(path, "://") : path);
        return getPlaceholders(uri);
    }

    public String getRequestContentType() {
        return getContentType();
    }

    @Override
    public String getContentType() {
        return requestHeaders.getValue(CONTENT_TYPE);
    }

    @Override
    public RequestSpecification noFilters() {
        this.filters.clear();
        return this;
    }

    @Override
    public <T extends Filter> RequestSpecification noFiltersOfType(Class<T> filterType) {
        notNull(filterType, "Filter type");
        this.filters.removeIf(it -> isInstanceOf(it, filterType));
        return this;
    }

    private String assembleCompleteTargetPath(String requestPath) {
        String targetUri;
        String targetPath;
        if (isFullyQualified(requestPath)) {
            targetUri = "";
            targetPath = "";
        } else {
            targetUri = getTargetURI(path);
            targetPath = substringBefore(getTargetPath(path), "?");
        }
        return mergeAndRemoveDoubleSlash(mergeAndRemoveDoubleSlash(targetUri, targetPath), requestPath);
    }

    private String findEncoderCharsetOrReturnDefault(String contentType) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentType);
        if (charset == null) {
            final EncoderConfig cfg;
            if (getConfig() == null) {
                cfg = new EncoderConfig();
            } else {
                cfg = getConfig().getEncoderConfig();
            }

            if (cfg.hasDefaultCharsetForContentType(contentType)) {
                charset = cfg.defaultCharsetForContentType(contentType);
            } else {
                charset = cfg.defaultContentCharset();
            }
        }
        return charset;
    }

    private ObjectMapperConfig objectMappingConfig() {
        return getConfig() == null ? ObjectMapperConfig.objectMapperConfig() : getConfig().getObjectMapperConfig();
    }

    private HttpClientConfig httpClientConfig() {
        return getConfig() == null ? HttpClientConfig.httpClientConfig() : getConfig().getHttpClientConfig();
    }

    private ConnectionConfig connectionConfig() {
        return getConfig() == null ? ConnectionConfig.connectionConfig() : getConfig().getConnectionConfig();
    }

    private EncoderConfig encoderConfig() {
        return getConfig() == null ? EncoderConfig.encoderConfig() : getConfig().getEncoderConfig();
    }

    private SessionConfig sessionConfig() {
        return getConfig() == null ? SessionConfig.sessionConfig() : getConfig().getSessionConfig();
    }

    private CsrfConfig csrfConfig() {
        return getConfig() == null ? CsrfConfig.csrfConfig() : getConfig().getCsrfConfig();
    }

    public RestAssuredConfig restAssuredConfig() {
        return getConfig() != null ? getConfig() : new RestAssuredConfig();
    }

    private enum EncodingTarget {
        BODY, QUERY
    }

    public static List<String> getPlaceholders(String uri) {
        // Same rules as PathParamFiller, which fills each path segment and each query parameter on its own: a placeholder
        // never spans a "/" in the path or a "&" in the query, so an unclosed "{" doesn't swallow the next placeholder.
        Set<String> placeholders = new LinkedHashSet<>(); // Remove duplicates such as if we have get("/{x}/{x}")
        int indexOfQuery = uri.indexOf('?');
        if (indexOfQuery < 0) {
            addPlaceholders(PATH_PLACEHOLDER, uri, placeholders);
        } else {
            addPlaceholders(PATH_PLACEHOLDER, uri.substring(0, indexOfQuery), placeholders);
            addPlaceholders(QUERY_PLACEHOLDER, uri.substring(indexOfQuery + 1), placeholders);
        }
        return Collections.unmodifiableList(new ArrayList<>(placeholders));
    }

    private static void addPlaceholders(Pattern placeholderPattern, String uriPart, Set<String> placeholders) {
        Matcher m = placeholderPattern.matcher(uriPart);
        while (m.find()) {
            placeholders.add(m.group(1).trim());
        }
    }

    public static String getDerivedPath(String uri) {
        return PathSupport.getPath(uri);
    }

    public String getURI(String uri) {
        return generateRequestUriForLogging(uri, method);
    }

    // Note that it's not possible to both redundant named and unnamed path parameters
    // as a map since redundant unnamed path parameters doesn't necessarily have a placeholder associated with it.
    // For example if we do get("/{x}", "1", "2") then there's no placeholder name for "2"
    public Map<String, String> getRedundantNamedPathParams() {
        List<String> placeholders = getPathParamPlaceholders();
        // The values aren't necessarily strings (e.g. a list), so don't let the compiler insert a cast to String
        Map<String, ?> namedPathParams = getNamedPathParams();
        Map<String, Object> redundantNamedPathParams = new LinkedHashMap<>();
        for (Entry<String, ?> namedPathParam : namedPathParams.entrySet()) {
            if (!placeholders.contains(namedPathParam.getKey())) {
                redundantNamedPathParams.put(namedPathParam.getKey(), namedPathParam.getValue());
            }
        }
        return strings(Collections.unmodifiableMap(redundantNamedPathParams));
    }

    public List<String> getRedundantUnnamedPathParamValues() {
        Map<String, String> allPathParams = getPathParams();
        long placeholdersWithoutPathParam = getPathParamPlaceholders().stream().filter(it -> !allPathParams.containsKey(it)).count();
        if (placeholdersWithoutPathParam + Math.max(getUnnamedPathParamValues().size() - getPathParamPlaceholders().size(), 0) > 0) {
            Collection<String> pathParamValues = allPathParams.values();
            List<String> redundantValues = new ArrayList<>();
            for (String value : getUnnamedPathParamValues()) {
                if (!pathParamValues.contains(value)) {
                    redundantValues.add(value);
                }
            }
            return Collections.unmodifiableList(redundantValues);
        }
        return Collections.unmodifiableList(Collections.emptyList());
    }

    public void removeUnnamedPathParamAtIndex(int indexOfParamName) {
        unnamedPathParamsTuples.remove(indexOfParamName);
        // We define the a tuple with "null, null" in order to retain path parameter order
        unnamedPathParamsTuples.add(indexOfParamName, new SimpleEntry<>(null, null));
    }

    public void setMethod(String method) {
        this.method = method == null ? null : method.toUpperCase(Locale.ROOT);
    }

    public AuthenticationScheme getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(AuthenticationScheme authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
        explicitlySetValues.add(MergeableValue.AUTHENTICATION_SCHEME);
    }

    /**
     * @return A new request specification with the defaults of the client whose <code>given()</code> created this
     * specification, or else {@link RestAssured#given()}. Form authentication and CSRF send their extra requests with it.
     */
    public RequestSpecification newRequestOfClient() {
        return newRequestOfClient.get();
    }

    /**
     * @return The port set on this specification or its defaults, or {@link RestAssured#UNDEFINED_PORT}. Unlike
     * {@link #getPort()}, not the port of the URI that a request is sent to.
     */
    public int getRequestPort() {
        return port;
    }

    // Accessors for SpecificationMerger and AuthenticationSpecificationImpl. The collections are the live ones.

    void setRequestPort(int port) {
        this.port = port;
        explicitlySetValues.add(MergeableValue.PORT);
    }

    void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        explicitlySetValues.add(MergeableValue.BASE_URI);
    }

    void setBasePath(String basePath) {
        this.basePath = basePath;
        explicitlySetValues.add(MergeableValue.BASE_PATH);
    }

    /**
     * @return The path given to the request method (for example "/users/{id}" for get("/users/{id}", 1)), before the path parameters are applied
     */
    public String getPath() {
        return path;
    }

    void setPath(String path) {
        this.path = path;
    }

    Map<String, Object> getRequestParameters() {
        return requestParameters;
    }

    Map<String, Object> getQueryParameters() {
        return queryParameters;
    }

    Map<String, Object> getFormParameters() {
        return formParameters;
    }

    Map<String, Object> getNamedPathParameters() {
        return namedPathParameters;
    }

    List<Entry<String, String>> getUnnamedPathParamsTuples() {
        return unnamedPathParamsTuples;
    }

    void setUnnamedPathParamsTuples(List<Entry<String, String>> unnamedPathParamsTuples) {
        this.unnamedPathParamsTuples = unnamedPathParamsTuples;
    }

    List<MultiPartInternal> getMultiParts() {
        return multiParts;
    }

    void setCookies(Cookies cookies) {
        this.cookies = cookies;
    }

    void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
        explicitlySetValues.add(MergeableValue.BODY);
    }

    List<Filter> getFilters() {
        return filters;
    }

    boolean isUrlEncodingEnabled() {
        return urlEncodingEnabled;
    }

    boolean isAllowContentType() {
        return allowContentType;
    }

    void setAllowContentType(boolean allowContentType) {
        this.allowContentType = allowContentType;
        explicitlySetValues.add(MergeableValue.ALLOW_CONTENT_TYPE);
    }

    boolean isAddCsrfFilter() {
        return addCsrfFilter;
    }

    void setAddCsrfFilter(boolean addCsrfFilter) {
        this.addCsrfFilter = addCsrfFilter;
        explicitlySetValues.add(MergeableValue.ADD_CSRF_FILTER);
    }

    void setProxySpecification(ProxySpecification proxySpecification) {
        this.proxySpecification = proxySpecification;
        explicitlySetValues.add(MergeableValue.PROXY);
    }

    boolean isExplicitlySet(MergeableValue value) {
        return explicitlySetValues.contains(value);
    }

    /**
     * Values that a specification gets from defaults (such as {@link RestAssured#baseURI}) unless they're set on it. When merging
     * a specification, {@link SpecificationMerger} only copies the ones that it set.
     */
    enum MergeableValue {
        BASE_URI, BASE_PATH, PORT, AUTHENTICATION_SCHEME, BODY, PROXY, URL_ENCODING_ENABLED, ALLOW_CONTENT_TYPE, ADD_CSRF_FILTER
    }

    private static int getFilterOrder(Filter filter) {
        return (filter instanceof OrderedFilter) ? ((OrderedFilter) filter).getOrder()
                : OrderedFilter.DEFAULT_PRECEDENCE;
    }

    private static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> objects(Map<?, ?> map) {
        return (Map) map;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Collection<Object> objects(Collection<?> collection) {
        return (Collection) collection;
    }

    // The parameter maps are exposed as Map<String, String> although they also hold NoParameterValue instances and lists
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, String> strings(Map<String, ?> map) {
        return (Map) map;
    }
}
