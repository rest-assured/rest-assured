/*
 * Copyright 2016 the original author or authors.
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

import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.mapper.ObjectMapperType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.config.XmlPathConfig;
import io.restassured.response.*;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * We delegate to the groovy impl here because the Groovy impl messes up generics (see e.g. http://stackoverflow.com/questions/11395527/groovy-generics-failure) and thus we cannot
 * let the Groovy implementation implement our interfaces directly.
 */
public class RestAssuredResponseOptionsImpl<R extends ResponseOptions<R>> implements ExtractableResponse<R> {

    private LogRepository logRepository;

    protected RestAssuredResponseOptionsGroovyImpl groovyResponse = new RestAssuredResponseOptionsGroovyImpl();

    public void setResponseHeaders(Object responseHeaders) {
        this.groovyResponse.setResponseHeaders(responseHeaders);
    }

    public void setCookies(Cookies cookies) {
        this.groovyResponse.setCookies(cookies);
    }

    public void setContent(Object content) {
        this.groovyResponse.setContent(content);
    }

    public void setContentType(Object contentType) {
        this.groovyResponse.setContentType(contentType);
    }

    public void setStatusLine(Object statusLine) {
        this.groovyResponse.setStatusLine(statusLine);
    }

    public void setStatusCode(Object statusCode) {
        this.groovyResponse.setStatusCode(statusCode);
    }

    public void setSessionIdName(Object sessionIdName) {
        this.groovyResponse.setSessionIdName(sessionIdName);
    }

    public void setFilterContextProperties(Map filterContextProperties) {
        this.groovyResponse.setFilterContextProperties(filterContextProperties);
    }

    public void setConnectionManager(Object connectionManager) {
        this.groovyResponse.setConnectionManager(connectionManager);
    }

    public void setDefaultContentType(String defaultContentType) {
        this.groovyResponse.setDefaultContentType(defaultContentType);
    }

    public void setRpr(ResponseParserRegistrar rpr) {
        this.groovyResponse.setRpr(rpr);
    }

    public void setDecoderConfig(DecoderConfig decoderConfig) {
        this.groovyResponse.setDecoderConfig(decoderConfig);
    }

    public void setHasExpectations(boolean hasExpectations) {
        this.groovyResponse.setHasExpectations(hasExpectations);
    }

    public void setConfig(RestAssuredConfig config) {
        this.groovyResponse.setConfig(config);
    }

    public ResponseParserRegistrar getRpr() {
        return groovyResponse.getRpr();
    }

    public RestAssuredConfig getConfig() {
        return groovyResponse.getConfig();
    }

    //    End setters and getters

    public ResponseBody body() {
        return (ResponseBody) this;
    }

    public Headers headers() {
        return groovyResponse.headers();
    }

    public String header(String name) {
        return groovyResponse.header(name);
    }

    public Map<String, String> cookies() {
        return groovyResponse.cookies();
    }

    public Cookies detailedCookies() {
        return groovyResponse.detailedCookies();
    }

    public String cookie(String name) {
        return groovyResponse.cookie(name);
    }

    public Cookie detailedCookie(String name) {
        return groovyResponse.detailedCookie(name);
    }

    public String contentType() {
        return groovyResponse.contentType();
    }

    public String statusLine() {
        return groovyResponse.statusLine();
    }

    public String sessionId() {
        return groovyResponse.sessionId();
    }

    public int statusCode() {
        return groovyResponse.statusCode();
    }

    public R response() {
        return (R) this;
    }

    public <T> T as(Class<T> cls) {
        return groovyResponse.as(cls, this);
    }

    public <T> T as(Class<T> cls, ObjectMapperType mapperType) {
        return groovyResponse.as(cls, mapperType, this);
    }

    public <T> T as(Class<T> cls, ObjectMapper mapper) {
        return groovyResponse.as(cls, mapper);
    }

    public JsonPath jsonPath() {
        return groovyResponse.jsonPath();
    }

    public JsonPath jsonPath(JsonPathConfig config) {
        return groovyResponse.jsonPath(config);
    }

    public XmlPath xmlPath() {
        return groovyResponse.xmlPath();
    }

    public XmlPath xmlPath(XmlPathConfig config) {
        return groovyResponse.xmlPath(config);
    }

    public XmlPath xmlPath(XmlPath.CompatibilityMode compatibilityMode) {
        return groovyResponse.xmlPath(compatibilityMode);
    }

    public XmlPath htmlPath() {
        return groovyResponse.htmlPath();
    }

    public <T> T path(String path, String... arguments) {
        return groovyResponse.path(path, arguments);
    }

    public String asString() {
        return groovyResponse.asString();
    }

    public String asString(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
        return groovyResponse.asString(forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse);
    }

    public byte[] asByteArray() {
        return groovyResponse.asByteArray();
    }

    public InputStream asInputStream() {
        return groovyResponse.asInputStream();
    }

    public boolean isInputStream() {
        return groovyResponse.isInputStream();
    }

    public String print() {
        return groovyResponse.print();
    }

    public String prettyPrint() {
        return groovyResponse.prettyPrint((ResponseOptions) this, (ResponseBody) this);
    }

    public R peek() {
        groovyResponse.peek((ResponseOptions) this, (ResponseBody) this);
        return (R) this;
    }

    public R prettyPeek() {
        groovyResponse.prettyPeek((ResponseOptions) this, (ResponseBody) this);
        return (R) this;
    }

    public R andReturn() {
        return (R) this;
    }

    public R thenReturn() {
        return (R) this;
    }

    public ResponseBody getBody() {
        return (ResponseBody) this;
    }

    public Headers getHeaders() {
        return groovyResponse.getHeaders();
    }

    public String getHeader(String name) {
        return groovyResponse.getHeader(name);
    }

    public Cookies getDetailedCookies() {
        return groovyResponse.getDetailedCookies();
    }

    public String getCookie(String name) {
        return groovyResponse.getCookie(name);
    }

    public Cookie getDetailedCookie(String name) {
        return groovyResponse.getDetailedCookie(name);
    }

    public String getSessionId() {
        return groovyResponse.getSessionId();
    }

    public Map<String, String> getCookies() {
        return groovyResponse.getCookies();
    }

    public String getContentType() {
        return groovyResponse.getContentType();
    }

    public String getStatusLine() {
        return groovyResponse.getStatusLine();
    }

    public int getStatusCode() {
        return groovyResponse.getStatusCode();
    }

    public Object getContent() {
        return groovyResponse.getContent();
    }

    public boolean getHasExpectations() {
        return groovyResponse.getHasExpectations();
    }

    public String getDefaultContentType() {
        return groovyResponse.getDefaultContentType();
    }

    public DecoderConfig getDecoderConfig() {
        return groovyResponse.getDecoderConfig();
    }

    public Object getSessionIdName() {
        return groovyResponse.getSessionIdName();
    }

    public Object getConnectionManager() {
        return groovyResponse.getConnectionManager();
    }

    public Object getResponseHeaders() {
        return groovyResponse.getResponseHeaders();
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public RestAssuredResponseOptionsGroovyImpl getGroovyResponse() {
        return groovyResponse;
    }

    public Map getFilterContextProperties() {
        return this.groovyResponse.getFilterContextProperties();
    }

    public void setGroovyResponse(RestAssuredResponseOptionsGroovyImpl groovyResponse) {
        this.groovyResponse = groovyResponse;
    }

    public long time() {
        return groovyResponse.time();
    }

    public long timeIn(TimeUnit timeUnit) {
        return groovyResponse.timeIn(timeUnit);
    }

    public long getTime() {
        return groovyResponse.time();
    }

    public long getTimeIn(TimeUnit timeUnit) {
        return groovyResponse.timeIn(timeUnit);
    }
}
