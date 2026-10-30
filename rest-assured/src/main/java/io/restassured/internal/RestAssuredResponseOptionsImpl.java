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

import io.restassured.common.mapper.DataToDeserialize;
import io.restassured.common.mapper.TypeRef;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.config.DecoderConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.assertion.CookieMatcher;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.mapping.ObjectMapperDeserializationContextImpl;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.internal.path.CorePathSettings;
import io.restassured.internal.path.PathProviders;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.internal.support.CloseHTTPClientConnectionInputStreamWrapper;
import io.restassured.internal.support.Prettifier;
import io.restassured.internal.support.UnreadBodies;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.XmlPath.CompatibilityMode;
import io.restassured.path.xml.config.XmlPathConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static io.restassured.internal.util.SafeExceptionRethrower.safeRethrow;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RestAssuredResponseOptionsImpl<R extends ResponseOptions<R>> implements ExtractableResponse<R> {
    private static final String CANNOT_PARSE_MSG = "Failed to parse response.";
    private static final String BINARY = "binary";
    private static final long NO_RESPONSE_TIME = -1;

    private LogRepository logRepository;

    // The Object-typed fields can be set to anything through their setters. Like the Groovy implementation this class
    // replaces, the accessors convert them to the type they return when they are read.
    private Object responseHeaders;
    private Cookies cookies;
    private Object content;
    private Object contentType;
    private Object statusLine;
    private Object statusCode;
    private Object sessionIdName;
    private Map filterContextProperties;
    // Closing it tells the HTTP client that REST Assured is done with the response
    private Closeable responseCloser;
    private URI effectiveUri;
    private String defaultContentType;
    private ResponseParserRegistrar rpr;
    private DecoderConfig decoderConfig;
    private boolean hasExpectations;
    private RestAssuredConfig config;
    // The defaults of then(), from the client that sent the request
    private Supplier<ResponseDefaults> responseDefaults;

    void parseResponse(HttpResponse response, Object content, boolean hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
        boolean readLater = !hasBodyAssertions && content instanceof InputStream;
        parse(response, readLater ? UnreadBodies.closeWhenUnreachable((InputStream) content, response) : content, hasBodyAssertions,
                responseParserRegistrar);
        // Closed along with a body that is read later (see asInputStream)
        responseCloser = response;
    }

    private void parse(HttpResponse response, Object content, boolean hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
        parseHeaders(response);
        parseContentType(response);
        parseCookies();
        parseStatus(response);
        if (hasBodyAssertions) {
            parseContent(content);
        } else {
            this.content = content;
        }
        hasExpectations = hasBodyAssertions;
        this.rpr = responseParserRegistrar;
        Parser defaultParser = responseParserRegistrar.getDefaultParser();
        this.defaultContentType = defaultParser == null ? null : defaultParser.getContentType();
    }

    private void parseHeaders(HttpResponse response) {
        List<Header> headerList = new ArrayList<>();
        for (HttpHeader header : response.headers()) {
            headerList.add(new Header(header.name(), header.value()));
        }
        this.responseHeaders = new Headers(headerList);
    }

    private void parseContentType(HttpResponse response) {
        String contentTypeHeader = response.firstHeader("Content-Type");
        // No content type was found, set it to empty
        contentType = contentTypeHeader == null ? "" : contentTypeHeader;
    }

    private void parseCookies() {
        Headers headers = headers();
        if (headers.hasHeaderWithName("Set-Cookie")) {
            cookies = CookieMatcher.getCookies(headers.getValues("Set-Cookie"));
        }
    }

    private void parseStatus(HttpResponse response) {
        // Like HttpClient 4's status line, which keeps the space before an empty reason phrase
        String reasonPhrase = response.reasonPhrase() == null ? "" : response.reasonPhrase();
        statusLine = response.protocolVersion() + " " + response.statusCode() + " " + reasonPhrase;
        statusCode = response.statusCode();
    }

    private void parseContent(Object content) {
        try {
            if (content instanceof InputStream) {
                this.content = convertStreamToByteArray((InputStream) content);
            } else if (content instanceof String) {
                this.content = content;
            } else {
                // The HTTP layer hands over an InputStream, a String or null, and null becomes an empty body here
                this.content = convertToString((Reader) content);
            }
        } catch (IllegalStateException e) {
            throw new IllegalStateException(CANNOT_PARSE_MSG, e);
        }
    }

    public void setResponseHeaders(Object responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setCookies(Cookies cookies) {
        this.cookies = cookies;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public void setContentType(Object contentType) {
        this.contentType = contentType;
    }

    public void setStatusLine(Object statusLine) {
        this.statusLine = statusLine;
    }

    public void setStatusCode(Object statusCode) {
        this.statusCode = statusCode;
    }

    public void setSessionIdName(Object sessionIdName) {
        this.sessionIdName = sessionIdName;
    }

    public void setFilterContextProperties(Map filterContextProperties) {
        this.filterContextProperties = filterContextProperties;
    }

    public void setEffectiveUri(URI effectiveUri) {
        this.effectiveUri = effectiveUri;
    }

    public void setResponseCloser(Closeable responseCloser) {
        this.responseCloser = responseCloser;
    }

    public void setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
    }

    public void setRpr(ResponseParserRegistrar rpr) {
        this.rpr = rpr;
    }

    public void setDecoderConfig(DecoderConfig decoderConfig) {
        this.decoderConfig = decoderConfig;
    }

    public void setHasExpectations(boolean hasExpectations) {
        this.hasExpectations = hasExpectations;
    }

    public void setConfig(RestAssuredConfig config) {
        this.config = config;
    }

    public ResponseParserRegistrar getRpr() {
        return rpr;
    }

    public RestAssuredConfig getConfig() {
        return config;
    }

    public void setResponseDefaults(Supplier<ResponseDefaults> responseDefaults) {
        this.responseDefaults = responseDefaults;
    }

    /**
     * @return Gives the defaults of <code>then()</code>, or <code>null</code> for a response that REST Assured didn't
     * send, such as one built with {@link io.restassured.builder.ResponseBuilder}
     */
    public Supplier<ResponseDefaults> getResponseDefaults() {
        return responseDefaults;
    }

    /**
     * @return The defaults of <code>then()</code> now, those of the static fields of {@link io.restassured.RestAssured}
     * for a response that REST Assured didn't send
     */
    protected ResponseDefaults responseDefaults() {
        return responseDefaults == null ? ResponseDefaults.ofStaticFields() : responseDefaults.get();
    }

    //    End setters and getters

    public ResponseBody body() {
        return (ResponseBody) this;
    }

    public Headers headers() {
        return responseHeaders == null ? new Headers() : (Headers) responseHeaders;
    }

    public String header(String name) {
        notNull(name, "name");
        return headers().getValue(name);
    }

    public Map<String, String> cookies() {
        Map<String, String> cookieMap = new LinkedHashMap<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie.getValue());
            }
        }
        return Collections.unmodifiableMap(cookieMap);
    }

    public Cookies detailedCookies() {
        return cookies == null ? new Cookies() : cookies;
    }

    public String cookie(String name) {
        notNull(name, "name");
        return cookies == null ? null : cookies.getValue(name);
    }

    public Cookie detailedCookie(String name) {
        return detailedCookies().get(name);
    }

    public String contentType() {
        return castToString(contentType);
    }

    public String statusLine() {
        return castToString(statusLine);
    }

    public String sessionId() {
        String name = castToString(sessionIdName);
        if (name == null) {
            // Responses that aren't created by RequestSpecificationImpl (e.g. Spring MockMvc, WebTestClient and
            // ResponseBuilder responses) have no session id name, so use the one from the config instead.
            name = config == null ? SessionConfig.DEFAULT_SESSION_ID_NAME : config.getSessionConfig().sessionIdName();
        }
        return cookie(name);
    }

    public int statusCode() {
        Number code = (Number) statusCode;
        // Groovy truth: a status code of 0 falls back to -1 just like a missing one
        return code == null || code.doubleValue() == 0 ? -1 : code.intValue();
    }

    public R response() {
        //noinspection unchecked
        return (R) this;
    }

    @Override
    public <T> T as(Class<T> cls) {
        return as((Type) cls);
    }

    public <T> T as(Class<T> cls, ObjectMapperType mapperType) {
        return as((Type) cls, mapperType);
    }

    public <T> T as(Class<T> cls, ObjectMapper mapper) {
        return as((Type) cls, mapper);
    }

    @Override
    public <T> T as(TypeRef<T> typeRef) {
        notNull(typeRef, "Type ref");
        return as(typeRef.getType());
    }

    public <T> T as(Type cls) {
        String charset = findCharset();
        String contentTypeToChose = findContentType(() -> new IllegalStateException("Cannot parse content to " + cls + " because no content-type was present in the response and no default parser has been set.\n" +
                "You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n\n" +
                "or you can specify an explicit ObjectMapper using as(" + cls + ", <ObjectMapper>);"));
        return ObjectMapping.deserialize(this, cls, contentTypeToChose, defaultContentType, charset, null, config.getObjectMapperConfig());
    }

    public <T> T as(Type cls, ObjectMapperType mapperType) {
        notNull(mapperType, "Object mapper type");
        String charset = findCharset();
        return ObjectMapping.deserialize(this, cls, null, defaultContentType, charset, mapperType, config.getObjectMapperConfig());
    }

    public <T> T as(Type cls, ObjectMapper mapper) {
        notNull(mapper, "Object mapper");
        ObjectMapperDeserializationContext ctx = createObjectMapperDeserializationContext(cls);
        //noinspection unchecked
        return (T) mapper.deserialize(ctx);
    }

    // json-path-gpath and xml-path-gpath are optional, so these methods check for them before GPathResponseViews uses them

    public JsonPath jsonPath() {
        PathProviders.requireGPath(DocumentType.JSON, "jsonPath()");
        return GPathResponseViews.jsonPath(asString(), findCharset(), config);
    }

    public JsonPath jsonPath(JsonPathConfig config) {
        notNull(config, "JsonPathConfig");
        PathProviders.requireGPath(DocumentType.JSON, "jsonPath()");
        return GPathResponseViews.jsonPath(asString(), config);
    }

    public XmlPath xmlPath() {
        PathProviders.requireGPath(DocumentType.XML, "xmlPath()");
        return GPathResponseViews.xmlPath(asString(), findCharset(), config);
    }

    public XmlPath xmlPath(XmlPathConfig config) {
        notNull(config, "XmlPathConfig");
        PathProviders.requireGPath(DocumentType.XML, "xmlPath()");
        return GPathResponseViews.xmlPath(asString(), config);
    }

    public XmlPath xmlPath(CompatibilityMode compatibilityMode) {
        notNull(compatibilityMode, "Compatibility mode");
        PathProviders.requireGPath(DocumentType.XML, "xmlPath()");
        return GPathResponseViews.xmlPath(compatibilityMode, asString(), findCharset(), config);
    }

    public XmlPath htmlPath() {
        PathProviders.requireGPath(DocumentType.HTML, "htmlPath()");
        return GPathResponseViews.htmlPath(asString(), findCharset(), config);
    }

    public <T> T path(String path, String... arguments) {
        notNull(path, "Path");
        if (arguments != null && arguments.length > 0) {
            path = String.format(path, (Object[]) arguments);
        }
        String contentType = findContentType(() -> new IllegalStateException("Cannot invoke the path method because no content-type was present in the response and no default parser has been set.\n\n" +
                "You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n"));
        DocumentType type;
        if (containsIgnoreCase(contentType, "xml")) {
            type = DocumentType.XML;
        } else if (containsIgnoreCase(contentType, "json")) {
            type = DocumentType.JSON;
        } else if (containsIgnoreCase(contentType, "html")) {
            type = DocumentType.HTML;
        } else {
            throw new IllegalStateException("Cannot determine which path implementation to use because the content-type " + contentType + " doesn't map to a path implementation.");
        }
        PathProvider provider = PathProviders.forType(type, config, path);
        Object document;
        try {
            document = provider.parse(PathInput.ofText(asString()), type, CorePathSettings.forPathMethod(config));
        } catch (Exception e) {
            throw provider.parseFailure(type, e);
        }
        //noinspection unchecked
        return (T) PathProviders.valueOf(provider, provider.evaluate(document, type, path));
    }

    public String asString() {
        return asString(false);
    }

    public String asString(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
        return charsetToString(findCharset(forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse));
    }

    public String asPrettyString() {
        return new Prettifier().getPrettifiedBodyIfPossible((ResponseOptions) this, (ResponseBody) this);
    }

    public byte[] asByteArray() {
        if (content == null) {
            return new byte[0];
        }
        if (content instanceof byte[]) {
            return (byte[]) content;
        } else if (content instanceof String) {
            return convertStringToByteArray((String) content);
        } else {
            byte[] bytes = readContentStream();
            content = bytes;
            return bytes;
        }
    }

    public InputStream asInputStream() {
        if (content == null || content instanceof InputStream) {
            return new CloseHTTPClientConnectionInputStreamWrapper(responseCloser, (InputStream) content);
        } else {
            return content instanceof String ? new ByteArrayInputStream(convertStringToByteArray((String) content)) : new ByteArrayInputStream((byte[]) content);
        }
    }

    public boolean isInputStream() {
        return content instanceof InputStream;
    }

    public String print() {
        String string = asString();
        content = string;
        System.out.println(string);
        return string;
    }

    public String prettyPrint() {
        String body = asPrettyString();
        System.out.println(body);
        return body;
    }

    public R peek() {
        ResponsePrinter.print((ResponseOptions) this, (ResponseBody) this, System.out, LogDetail.ALL, false, blacklistedHeaders());
        //noinspection unchecked
        return (R) this;
    }

    public R prettyPeek() {
        ResponsePrinter.print((ResponseOptions) this, (ResponseBody) this, System.out, LogDetail.ALL, true, blacklistedHeaders());
        //noinspection unchecked
        return (R) this;
    }

    public R andReturn() {
        //noinspection unchecked
        return (R) this;
    }

    public R thenReturn() {
        //noinspection unchecked
        return (R) this;
    }

    public ResponseBody getBody() {
        return (ResponseBody) this;
    }

    public Headers getHeaders() {
        return headers();
    }

    public String getHeader(String name) {
        return header(name);
    }

    public Cookies getDetailedCookies() {
        return detailedCookies();
    }

    public String getCookie(String name) {
        return cookie(name);
    }

    public Cookie getDetailedCookie(String name) {
        return detailedCookie(name);
    }

    public String getSessionId() {
        return sessionId();
    }

    public Map<String, String> getCookies() {
        return cookies();
    }

    public String getContentType() {
        return contentType();
    }

    public String getStatusLine() {
        return statusLine();
    }

    public int getStatusCode() {
        return statusCode();
    }

    public Object getContent() {
        return content;
    }

    public boolean getHasExpectations() {
        return hasExpectations;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    public Object getSessionIdName() {
        return sessionIdName;
    }

    public Closeable getResponseCloser() {
        return responseCloser;
    }

    public Object getResponseHeaders() {
        return responseHeaders;
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public Map getFilterContextProperties() {
        return filterContextProperties;
    }

    /**
     * @return The URI of the request that the response answers, which is the last one of its redirects, or
     * <code>null</code> if the response didn't come from an HTTP request
     */
    public URI getEffectiveUri() {
        return effectiveUri;
    }

    public long time() {
        if (filterContextProperties != null && filterContextProperties.containsKey(TimingFilter.RESPONSE_TIME_MILLISECONDS)) {
            return ((Number) filterContextProperties.get(TimingFilter.RESPONSE_TIME_MILLISECONDS)).longValue();
        } else {
            return NO_RESPONSE_TIME;
        }
    }

    public long timeIn(TimeUnit timeUnit) {
        notNull(timeUnit, TimeUnit.class);
        long time = time();
        if (time != NO_RESPONSE_TIME && timeUnit != TimeUnit.MILLISECONDS) {
            time = timeUnit.convert(time, TimeUnit.MILLISECONDS);
        }
        return time;
    }

    public long getTime() {
        return time();
    }

    public long getTimeIn(TimeUnit timeUnit) {
        return timeIn(timeUnit);
    }

    private String findCharset() {
        return findCharset(false);
    }

    private String findCharset(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
        String contentType = contentType();
        String charset = CharsetExtractor.getCharsetFromContentType(isBlank(contentType) ? defaultContentType : contentType);

        if (charset == null || charset.trim().equals("")) {
            if (decoderConfig == null || forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
                return Charset.defaultCharset().toString();
            } else {
                charset = decoderConfig.defaultCharsetForContentType(contentType);
            }
        }

        if (StringUtils.equalsIgnoreCase(charset, BINARY)) {
            charset = decoderConfig.defaultCharsetForContentType(contentType);
        }

        return charset;
    }

    private Set<String> blacklistedHeaders() {
        LogConfig logConfig = config == null ? null : config.getLogConfig();
        Set<String> blacklistedHeaders = logConfig == null ? null : logConfig.blacklistedHeaders();
        return blacklistedHeaders == null ? Collections.emptySet() : blacklistedHeaders;
    }

    private String findContentType(Supplier<IllegalStateException> noContentTypeException) {
        String contentType = contentType();
        if ("".equals(contentType)) {
            if (defaultContentType != null) {
                return defaultContentType;
            }
            throw noContentTypeException.get();
        } else if (rpr.hasCustomParserExcludingDefaultParser(contentType)) {
            return rpr.getNonDefaultParser(contentType).getContentType();
        } else {
            return contentType;
        }
    }

    private String charsetToString(String charset) {
        if (content == null) {
            return "";
        }

        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof byte[]) {
            return newString((byte[]) content, charset);
        } else {
            byte[] bytes = readContentStream();
            content = bytes;
            return newString(bytes, charset);
        }
    }

    private byte[] convertStringToByteArray(String string) {
        try {
            return string.getBytes(findCharset());
        } catch (UnsupportedEncodingException e) {
            return safeRethrow(e);
        }
    }

    private static String newString(byte[] bytes, String charset) {
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            return safeRethrow(e);
        }
    }

    // Reads the body through asInputStream(), which closes idle connections once it's read if configured to
    private byte[] readContentStream() {
        return convertStreamToByteArray(asInputStream());
    }

    private static byte[] convertStreamToByteArray(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            try {
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
            } finally {
                is.close();
            }
        } catch (IOException e) {
            return safeRethrow(e);
        }
        return buffer.toByteArray();
    }

    private static String convertToString(Reader reader) {
        if (reader == null) {
            return "";
        }

        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            try {
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            return safeRethrow(e);
        }
        return writer.toString();
    }

    private ObjectMapperDeserializationContext createObjectMapperDeserializationContext(Type cls) {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();
        ctx.setType(cls);
        ctx.setCharset(findCharset());
        ctx.setContentType(contentType());
        ctx.setDataToDeserialize(new DataToDeserialize() {
            @Override
            public String asString() {
                return RestAssuredResponseOptionsImpl.this.asString();
            }

            @Override
            public byte[] asByteArray() {
                return RestAssuredResponseOptionsImpl.this.asByteArray();
            }

            @Override
            public InputStream asInputStream() {
                return RestAssuredResponseOptionsImpl.this.asInputStream();
            }
        });
        return ctx;
    }
}
