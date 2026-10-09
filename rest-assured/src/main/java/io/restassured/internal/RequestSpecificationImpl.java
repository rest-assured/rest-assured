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
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.FormAuthScheme;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.CsrfConfig;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RedirectConfig;
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
import io.restassured.internal.common.util.GroovyStyleToString;
import io.restassured.internal.filter.CsrfFilter;
import io.restassured.internal.filter.FilterContextImpl;
import io.restassured.internal.filter.FormAuthFilter;
import io.restassured.internal.filter.SendRequestFilter;
import io.restassured.internal.http.BoundaryExtractor;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.http.ContentEncoding;
import io.restassured.internal.http.ContentTypeExtractor;
import io.restassured.internal.http.CrossHostSensitiveHeaderStripper;
import io.restassured.internal.http.HTTPBuilder;
import io.restassured.internal.http.HttpResponseHandler;
import io.restassured.internal.http.NoSecureConnectionReuseStrategy;
import io.restassured.internal.http.RequestBodyEncoder;
import io.restassured.internal.http.Status;
import io.restassured.internal.http.URIBuilder;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.internal.multipart.MultiPartInternal;
import io.restassured.internal.multipart.MultiPartSpecificationImpl;
import io.restassured.internal.multipart.RestAssuredMultiPartEntity;
import io.restassured.internal.proxy.RestAssuredProxySelector;
import io.restassured.internal.proxy.RestAssuredProxySelectorRoutePlanner;
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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.params.HttpParams;
import org.apache.http.ConnectionReuseStrategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
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
import static org.apache.http.client.params.ClientPNames.ALLOW_CIRCULAR_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.MAX_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.REJECT_RELATIVE_REDIRECT;

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
    private Map<String, Object> httpClientParams = new LinkedHashMap<>();
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

    private AbstractHttpClient httpClient;

    private boolean allowContentType;

    private boolean addCsrfFilter;

    public RequestSpecificationImpl(String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme, List<Filter> filters,
                                    RequestSpecification defaultSpec, boolean urlEncode, RestAssuredConfig restAssuredConfig, LogRepository logRepository,
                                    ProxySpecification proxySpecification, boolean allowContentType, boolean addCsrfFilter) {
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
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(GET, path);
        });
    }

    @Override
    public Response post(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(POST, path);
        });
    }

    @Override
    public Response put(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(PUT, path);
        });
    }

    @Override
    public Response delete(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(DELETE, path);
        });
    }

    @Override
    public Response head(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(HEAD, path);
        });
    }

    @Override
    public Response patch(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(PATCH, path);
        });
    }

    @Override
    public Response options(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(OPTIONS, path);
        });
    }

    @Override
    public Response query(String path, Map<String, ?> pathParamsMap) {
        return ExceptionUnwrapper.runWithUnwrap(() -> {
            pathParams(pathParamsMap);
            return applyPathParamsAndSendRequest(QUERY, path);
        });
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
        // Allow all host names in order to be backward compatible
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password).allowAllHostnames());
        return this;
    }

    @Override
    public RequestSpecification keyStore(File pathToJks, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        // Allow all host names in order to be backward compatible
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password).allowAllHostnames());
        return this;
    }

    @Override
    public RequestSpecification trustStore(String path, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password).allowAllHostnames());
        return this;
    }

    @Override
    public RequestSpecification trustStore(File path, String password) {
        SSLConfig sslConfig = restAssuredConfig().getSSLConfig();
        restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password).allowAllHostnames());
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
        logSpecification.setBlacklistedHeaders(restAssuredConfig().getLogConfig().blacklistedHeaders());
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
        return responseSpecification;
    }

    @Override
    public ResponseSpecification expect() {
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
        addCsrfFilter = false;
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
        this.port = port;
        return this;
    }

    @Override
    public RequestSpecification body(String body) {
        notNull(body, "body");
        this.requestBody = body;
        return this;
    }

    @Override
    public RequestSpecification baseUri(String baseUri) {
        notNull(baseUri, "Base URI");
        this.baseUri = baseUri;
        return this;
    }

    @Override
    public RequestSpecification basePath(String basePath) {
        notNull(basePath, "Base Path");
        this.basePath = basePath;
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
        try {
            return proxy(new org.apache.http.client.utils.URIBuilder().setHost(host).setPort(port).setScheme(scheme).build());
        } catch (URISyntaxException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    @Override
    public RequestSpecification proxy(URI uri) {
        notNull(uri, String.class);
        return proxy(new ProxySpecification(uri.getHost(), uri.getPort(), uri.getScheme()));
    }

    @Override
    public RequestSpecification proxy(ProxySpecification proxySpecification) {
        notNull(proxySpecification, ProxySpecification.class);
        this.proxySpecification = proxySpecification;
        return this;
    }

    @Override
    public RequestSpecification body(byte[] body) {
        notNull(body, "body");
        this.requestBody = body;
        return this;
    }

    @Override
    public RequestSpecification body(File body) {
        notNull(body, "body");
        this.requestBody = body;
        return this;
    }

    @Override
    public RequestSpecification body(InputStream body) {
        notNull(body, "body");
        this.requestBody = body;
        return this;
    }

    @Override
    public RequestSpecification body(Object object) {
        notNull(object, "object");
        if (!isSerializableCandidate(object)) {
            return body(object.toString());
        }

        this.requestBody = ObjectMapping.serialize(object, getRequestContentType(), findEncoderCharsetOrReturnDefault(getRequestContentType()), null, objectMappingConfig(), restAssuredConfig().getEncoderConfig());
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
        this.requestBody = mapper.serialize(ctx);
        return this;
    }

    @Override
    public RequestSpecification body(Object object, ObjectMapperType mapperType) {
        notNull(object, "object");
        notNull(mapperType, "Object mapper type");
        this.requestBody = ObjectMapping.serialize(object, getRequestContentType(), findEncoderCharsetOrReturnDefault(getRequestContentType()), mapperType, objectMappingConfig(), restAssuredConfig().getEncoderConfig());
        return this;
    }

    @Override
    public RequestSpecification contentType(ContentType contentType) {
        notNull(contentType, ContentType.class);
        allowContentType = true;
        return header(CONTENT_TYPE, contentType);
    }

    @Override
    public RequestSpecification contentType(String contentType) {
        notNull(contentType, "Content-Type header cannot be null");
        allowContentType = true;
        return header(CONTENT_TYPE, contentType);
    }

    @Override
    public RequestSpecification noContentType() {
        allowContentType = false;
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
        cookieList.add(new Cookie.Builder(cookieName, serializeIfNeeded(value)).build());
        if (additionalValues != null) {
            for (Object additionalValue : additionalValues) {
                cookieList.add(new Cookie.Builder(cookieName, serializeIfNeeded(additionalValue)).build());
            }
        }

        return cookies(new Cookies(cookieList));
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
        return new RedirectSpecificationImpl(this, httpClientParams);
    }

    @Override
    public RequestSpecification spec(RequestSpecification requestSpecificationToMerge) {
        if (requestSpecificationToMerge != null && !(requestSpecificationToMerge instanceof RequestSpecificationImpl)) {
            throw new IllegalArgumentException("Cannot merge a request specification of type " + requestSpecificationToMerge.getClass().getName()
                    + ", it must be of type " + RequestSpecificationImpl.class.getName() + ".");
        }
        SpecificationMerger.merge(this, (RequestSpecificationImpl) requestSpecificationToMerge);
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

        // The Groovy implementation checked "!requestSpecification.getHttpClient() instanceof AbstractHttpClient" here, which
        // never fired. A client that isn't an AbstractHttpClient fails with a ClassCastException instead (see below and
        // applyPathParamsAndSendRequest).

        ResponseSpecificationImpl responseSpecification = (ResponseSpecificationImpl) this.responseSpecification;
        ResponseSpecificationImpl.HamcrestAssertionClosure hamcrestAssertionClosure = (ResponseSpecificationImpl.HamcrestAssertionClosure) assertionClosure;
        RestAssuredHttpBuilder http = new RestAssuredHttpBuilder(responseSpecification, requestHeaders, queryParameters, targetUri, hamcrestAssertionClosure, urlEncodingEnabled,
                getConfig(), (AbstractHttpClient) requestSpecification.getHttpClient(), allowContentType, responseSpecification.getRpr().getDefaultParser());
        applyProxySettings(http);
        applyRestAssuredConfig(http);
        registerRestAssuredEncoders(http);
        setRequestHeadersToHttpBuilder(http);

        if (cookies.exist()) {
            String cookieHeaderValue = cookies.asList().stream().map(it -> it.getName() + "=" + it.getValue()).collect(Collectors.joining("; "));
            headersOf(http).put("Cookie", cookieHeaderValue);
        }

        // Allow returning a the response
        RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();
        restAssuredResponse.setLogRepository(logRepository);
        RestAssuredConfig cfg = getConfig() != null ? getConfig() : new RestAssuredConfig();
        restAssuredResponse.setSessionIdName(cfg.getSessionConfig().sessionIdName());
        restAssuredResponse.setDecoderConfig(cfg.getDecoderConfig());
        restAssuredResponse.setConnectionManager(http.getClient().getConnectionManager());
        restAssuredResponse.setConfig(cfg);
        restAssuredResponse.setFilterContextProperties(filterContextProperties);
        responseSpecification.setRestAssuredResponse(restAssuredResponse);
        Object acceptContentType = hamcrestAssertionClosure.getResponseContentType();

        // Applying the SSL config or certificate authentication replaces the https scheme of the http client's scheme registry.
        // Restore the original scheme once the request has been sent, and don't keep https connections established with the
        // request specific scheme alive, so that the SSL settings of this request don't leak into later requests if the http
        // client instance (or its connection manager) is reused.
        AbstractHttpClient client = http.getClient();
        SchemeRegistry schemeRegistry = client.getConnectionManager().getSchemeRegistry();
        Scheme originalHttpsScheme = schemeRegistry.get("https");
        ConnectionReuseStrategy originalReuseStrategy = client.getConnectionReuseStrategy();
        try {
            if (shouldApplySSLConfig(http, cfg)) {
                SSLConfig sslConfig = cfg.getSSLConfig();
                CertAuthScheme certAuthScheme = new CertAuthScheme();
                certAuthScheme.setPathToKeyStore(sslConfig.getPathToKeyStore());
                certAuthScheme.setKeyStorePassword(sslConfig.getKeyStorePassword());
                certAuthScheme.setKeystoreType(sslConfig.getKeyStoreType());
                certAuthScheme.setKeyStore(sslConfig.getKeyStore());
                certAuthScheme.setPathToTrustStore(sslConfig.getPathToTrustStore());
                certAuthScheme.setTrustStorePassword(sslConfig.getTrustStorePassword());
                certAuthScheme.setTrustStoreType(sslConfig.getTrustStoreType());
                certAuthScheme.setTrustStore(sslConfig.getTrustStore());
                certAuthScheme.setPort(sslConfig.getPort());
                certAuthScheme.setSslSocketFactory(sslConfig.getSSLSocketFactory());
                certAuthScheme.setX509HostnameVerifier(sslConfig.getX509HostnameVerifier());
                certAuthScheme.authenticate(http);
            }

            authenticationScheme.authenticate(http);

            if (schemeRegistry.get("https") != originalHttpsScheme) {
                client.setReuseStrategy(new NoSecureConnectionReuseStrategy(originalReuseStrategy));
            }

            // Register the cross-host header stripper after authentication so it runs last in the interceptor chain.
            RedirectConfig redirectConfig = restAssuredConfig == null ? null : restAssuredConfig.getRedirectConfig();
            applyCrossHostRedirectHeaderStripping(http, redirectConfig != null ? redirectConfig : new RedirectConfig());

            if (mayHaveBody(method)) {
                if (hasFormParams() && requestBody != null) {
                    throw new IllegalStateException("You can either send form parameters OR body content in " + method + ", not both!");
                }
                Object bodyContent = createFormParamBodyContent(assembleBodyContent(method));
                if (POST.name().equalsIgnoreCase(method)) {
                    http.post(formRequestArguments(targetPath, bodyContent, acceptContentType), responseHandler(hamcrestAssertionClosure));
                } else if (PATCH.name().equalsIgnoreCase(method)) {
                    http.patch(formRequestArguments(targetPath, bodyContent, acceptContentType), responseHandler(hamcrestAssertionClosure));
                } else {
                    requestBody = bodyContent;
                    sendHttpRequest(http, method, acceptContentType, targetPath, hamcrestAssertionClosure);
                }
            } else {
                sendHttpRequest(http, method, acceptContentType, targetPath, hamcrestAssertionClosure);
            }
        } finally {
            if (originalHttpsScheme == null) {
                schemeRegistry.unregister("https");
            } else {
                schemeRegistry.register(originalHttpsScheme);
            }
            client.setReuseStrategy(originalReuseStrategy);
        }
        return restAssuredResponse;
    }

    private Map<String, Object> formRequestArguments(String targetPath, Object bodyContent, Object acceptContentType) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("path", targetPath);
        arguments.put("body", bodyContent);
        arguments.put("allowContentType", allowContentType);
        arguments.put("requestContentType", requestHeaders.getValue(CONTENT_TYPE));
        arguments.put("contentType", acceptContentType);
        return arguments;
    }

    private static HttpResponseHandler responseHandler(ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure) {
        return (response, content) -> {
            if (assertionClosure != null) {
                assertionClosure.call(response, content);
            }
            return null;
        };
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

    public boolean shouldApplySSLConfig(HTTPBuilder http, RestAssuredConfig cfg) {
        URI uri = ((URIBuilder) http.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        if (!cfg.getSSLConfig().isUserConfigured() || authenticationScheme instanceof CertAuthScheme) {
            return false;
        }
        if (uri.getScheme() != null && "https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
            return true;
        }
        // A non-https request may be redirected to https (see issue #790) so the SSL config is applied to it as well (the SSL
        // socket factory is then created lazily, see AuthConfig#certificate). This is only done for http clients created by
        // REST Assured's default http client factory since a custom factory may have registered its own https scheme that
        // must not be replaced for requests that don't start out as https.
        return cfg.getHttpClientConfig().usesDefaultHttpClientFactory();
    }

    public void applyRestAssuredConfig(HTTPBuilder http) {
        // Decoder config should always be applied regardless if restAssuredConfig is null or not because
        // by default we should support GZIP and DEFLATE decoding.
        DecoderConfig decoderConfig = restAssuredConfig == null ? null : restAssuredConfig.getDecoderConfig();
        applyContentDecoders(http, (decoderConfig != null ? decoderConfig : new DecoderConfig()).contentDecoders());
        if (restAssuredConfig != null) {
            applyRedirectConfig(restAssuredConfig.getRedirectConfig());
            applyHttpClientConfig(restAssuredConfig.getHttpClientConfig());
            applyEncoderConfig(http, restAssuredConfig.getEncoderConfig());
            applySessionConfig(restAssuredConfig.getSessionConfig());
        }
        if (!httpClientParams.isEmpty()) {
            HttpParams p = http.getClient().getParams();

            // Groovy callers may use GString keys, which Groovy converted to String
            for (Entry<?, ?> entry : ((Map<?, ?>) httpClientParams).entrySet()) {
                p.setParameter(castToString(entry.getKey()), entry.getValue());
            }
        }
    }

    private void applyCrossHostRedirectHeaderStripping(HTTPBuilder http, RedirectConfig redirectConfig) {
        if (!(http.getClient() instanceof AbstractHttpClient)) {
            return;
        }
        AbstractHttpClient client = http.getClient();
        // Remove first so repeated configuration (e.g. a reused HttpClient instance) never stacks duplicates.
        // Register last (after authentication schemes have added their interceptors) so that an auth interceptor
        // such as the OAuth/OAuth2 signer cannot re-add Authorization after we have stripped it on a redirect.
        client.removeRequestInterceptorByClass(CrossHostSensitiveHeaderStripper.class);
        if (redirectConfig.stripsSensitiveHeadersOnCrossHostRedirect()) {
            client.addRequestInterceptor(new CrossHostSensitiveHeaderStripper());
        }
    }

    private void applyContentDecoders(HTTPBuilder httpBuilder, List<DecoderConfig.ContentDecoder> contentDecoders) {
        Object[] httpBuilderContentEncoders = contentDecoders.stream().map(contentDecoder -> ContentEncoding.Type.valueOf(contentDecoder.toString())).toArray();
        httpBuilder.setContentEncoding(httpBuilderContentEncoders);
    }

    public void applySessionConfig(SessionConfig sessionConfig) {
        if (sessionConfig.isSessionIdValueDefined() && !cookies.hasCookieWithName(sessionConfig.sessionIdName())) {
            cookie(sessionConfig.sessionIdName(), sessionConfig.sessionIdValue());
        }
    }

    public void applyEncoderConfig(HTTPBuilder httpBuilder, EncoderConfig encoderConfig) {
        httpBuilder.getEncoders().setEncoderConfig(encoderConfig);
    }

    public void applyHttpClientConfig(HttpClientConfig httpClientConfig) {
        for (Entry<String, ?> entry : new LinkedHashMap<String, Object>(httpClientConfig.params()).entrySet()) {
            httpClientParams.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public void applyRedirectConfig(RedirectConfig redirectConfig) {
        httpClientParams.putIfAbsent(ALLOW_CIRCULAR_REDIRECTS, redirectConfig.allowsCircularRedirects());
        httpClientParams.putIfAbsent(HANDLE_REDIRECTS, redirectConfig.followsRedirects());
        httpClientParams.putIfAbsent(MAX_REDIRECTS, redirectConfig.maxRedirects());
        httpClientParams.putIfAbsent(REJECT_RELATIVE_REDIRECT, redirectConfig.rejectRelativeRedirects());
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

    public void setRequestHeadersToHttpBuilder(HTTPBuilder http) {
        Map<Object, Object> httpHeaders = headersOf(http);
        for (Header header : requestHeaders) {
            String headerName = header.getName();
            String headerValue = header.getValue();
            if (httpHeaders.containsKey(headerName)) {
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
                httpHeaders.put(headerName, headerValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> headersOf(HTTPBuilder http) {
        return (Map<Object, Object>) http.getHeaders();
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

    private void registerRestAssuredEncoders(HTTPBuilder http) {
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

        final HttpMultipartMode multipartMode = httpClientConfig().httpMultipartMode();

        RequestBodyEncoder multiPartEncoder = (contentType, content) -> {
            RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity(subType, charsetToUse, multipartMode, boundaryToUse);

            for (MultiPartInternal multiPart : multiParts) {
                ContentBody body = (ContentBody) multiPart.getContentBody();
                String controlName = multiPart.getControlName();
                Map<?, ?> headers = multiPart.getHeaders();
                FormBodyPartBuilder builder = FormBodyPartBuilder.create(controlName, body);
                if (headers != null) {
                    // Groovy callers may use GString names and values, which Groovy converted to String
                    for (Entry<?, ?> header : headers.entrySet()) {
                        builder.addField(castToString(header.getKey()), castToString(header.getValue()));
                    }
                }
                FormBodyPart part = builder.build();
                // note: as of org.apache.httpcomponents:httpmime:4.5.13 FormBodyPartBuilder adds `Content-Transfer-Encoding` header to
                // each part, causing issues for sides that follow https://datatracker.ietf.org/doc/html/rfc7578#section-4.7
                part.getHeader().removeFields("Content-Transfer-Encoding");
                entity.addPart(part);
            }

            return entity;
        };
        http.getEncoders().putAt(ct, multiPartEncoder);
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

    private void sendHttpRequest(HTTPBuilder http, String method, Object responseContentType, String targetPath,
                                 ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure) throws IOException, URISyntaxException {
        Map<String, Object> allQueryParams = mergeMapsAndRetainOrder(requestParameters, queryParameters);
        if (method.equals(GET.name())) {
            allQueryParams = mergeMapsAndRetainOrder(allQueryParams, formParameters);
        }
        boolean hasBody = requestBody != null;
        Map<String, Object> queryParams = allQueryParams;
        http.request(method, responseContentType, hasBody, delegate -> {
            delegate.getUri().setPath(targetPath);

            if (this.allowContentType) {
                delegate.setRequestContentType(defineRequestContentTypeAsString(method));
            }

            if (hasBody) {
                delegate.setBody(delegate.getRequestContentType(), requestBody);
            }

            delegate.getUri().setQuery(queryParams);

            HttpResponseHandler responseHandler = assertionClosure.getResponseHandler();
            // response handler for a success response code:
            delegate.getResponse().put(Status.SUCCESS.toString(), responseHandler);

            // handler for any failure status code:
            delegate.getResponse().put(Status.FAILURE.toString(), responseHandler);
        });
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

    private Response applyPathParamsAndSendRequest(String method, String path, Object... unnamedPathParams) {
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
        LogConfig logConfig = restAssuredConfig().getLogConfig();
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
        httpClient = (AbstractHttpClient) httpClientConfig().httpClientInstance();
        Response response = ctx.next(this, responseSpecification);
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
    public HttpClient getHttpClient() {
        return httpClient;
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

    private void applyProxySettings(RestAssuredHttpBuilder http) {
        // make client aware of JRE proxy settings http://freeside.co/betamax/
        RestAssuredProxySelector proxySelector = new RestAssuredProxySelector();
        proxySelector.setDelegatingProxySelector(ProxySelector.getDefault());
        proxySelector.setProxySpecification(proxySpecification);
        http.getClient().setRoutePlanner(new RestAssuredProxySelectorRoutePlanner(http.getClient().getConnectionManager().getSchemeRegistry(), proxySelector, proxySpecification));
        if (proxySpecification != null && (proxySpecification.getUsername() != null || proxySpecification.getPassword() != null)) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            InetSocketAddress address = new InetSocketAddress(proxySpecification.getHost(), proxySpecification.getPort());
            // We need to convert the host to an IP since that's what our proxy selector (RestAssuredProxySelector) expects
            AuthScope authScope = new AuthScope(address.getAddress().getHostAddress(), proxySpecification.getPort());
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxySpecification.getUsername(), proxySpecification.getPassword());
            credsProvider.setCredentials(authScope, credentials);
            http.getClient().setCredentialsProvider(credsProvider);
        }
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
    }

    // Accessors for SpecificationMerger and AuthenticationSpecificationImpl. The collections are the live ones.

    int getRequestPort() {
        return port;
    }

    void setRequestPort(int port) {
        this.port = port;
    }

    void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    String getPath() {
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
    }

    boolean isAddCsrfFilter() {
        return addCsrfFilter;
    }

    void setAddCsrfFilter(boolean addCsrfFilter) {
        this.addCsrfFilter = addCsrfFilter;
    }

    void setProxySpecification(ProxySpecification proxySpecification) {
        this.proxySpecification = proxySpecification;
    }

    // Unlike config(..), this doesn't change the config of the response specification
    void setRestAssuredConfig(RestAssuredConfig restAssuredConfig) {
        this.restAssuredConfig = restAssuredConfig;
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
