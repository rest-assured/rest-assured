/*
 * Copyright 2022 the original author or authors.
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

import io.restassured.config.ConnectionConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Headers;
import io.restassured.internal.common.util.GroovyStyleToString;
import io.restassured.internal.http.*;
import io.restassured.parsing.Parser;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;
import java.util.function.Function;

import static io.restassured.http.ContentType.ANY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trim;

class RestAssuredHttpBuilder extends HTTPBuilder {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MULTIPART = "multipart";
    private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH = MULTIPART + "/";

    private Headers requestHeaders;
    private RestAssuredConfig config;
    private boolean allowContentType;
    private Parser parser;
    FilterableResponseSpecification responseSpecification;
    ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure;

    RestAssuredHttpBuilder(FilterableResponseSpecification responseSpecification, Headers requestHeaders, Object defaultURI,
                           ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure, boolean urlEncodingEnabled, RestAssuredConfig config, AbstractHttpClient client, boolean allowContentType,
                           Parser parser) {
        super(defaultURI, urlEncodingEnabled, orNull(config, RestAssuredConfig::getEncoderConfig), orNull(config, RestAssuredConfig::getDecoderConfig), orNull(config, RestAssuredConfig::getOAuthConfig), client);
        this.responseSpecification = responseSpecification;
        this.requestHeaders = requestHeaders;
        this.assertionClosure = assertionClosure;
        this.config = config;
        this.allowContentType = allowContentType;
        this.parser = parser;
    }

    private static <E, T> T orNull(E instance, Function<E, T> s) {
        return instance == null ? null : s.apply(instance);
    }

    /**
     * A copy of HTTP builders doRequest method with two exceptions.
     * <ol>
     *  <li>The exception is that the entity's content is not closed if no body matchers are specified.</li>
     *  <li>If headers contain a list of elements the headers are added and not overridden</li>
     *  </ol>
     */
    @Override
    protected Object doRequest(HTTPBuilder.RequestConfigDelegate delegate) throws IOException {
        if (delegate.getRequest() instanceof HttpPost && assertionClosure != null) {
            delegate.getResponse().put(
                    Status.FAILURE.toString(),
                    (response, content) -> assertionClosure.call(response, content));
        }
        final HttpRequestBase reqMethod = delegate.getRequest();
        Object acceptContentType = delegate.getContentType();
        if (!requestHeaders.hasHeaderWithName("Accept")) {
            String acceptContentTypes = acceptContentType.toString();
            if (acceptContentType instanceof ContentType)
                acceptContentTypes = ((ContentType) acceptContentType).getAcceptHeader();
            reqMethod.setHeader("Accept", acceptContentTypes);
        }
        reqMethod.setURI(delegate.getUri().toURI());
        if (shouldApplyContentTypeFromRestAssuredConfigDelegate(delegate, reqMethod)) {
            String contentTypeToUse = trim(delegate.getRequestContentType());
            reqMethod.setHeader(CONTENT_TYPE, contentTypeToUse);
        }
        if (reqMethod.getURI() == null)
            throw new IllegalStateException("Request URI cannot be null");
        Map<?, ?> headers1 = delegate.getHeaders();
        for (Object key : headers1.keySet()) {
            if (key == null) continue;
            Object val = headers1.get(key);
            if (val == null) {
                reqMethod.removeHeaders(key.toString());
            } else if (key.toString().equalsIgnoreCase(CONTENT_TYPE) && !allowContentType) {
                reqMethod.removeHeaders(key.toString());
            } else if (!key.toString().equalsIgnoreCase(CONTENT_TYPE) || !val.toString().startsWith(MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)) {
                // Don't overwrite multipart header because HTTP Client have added boundary
                String keyAsString = key.toString();
                if (val instanceof Collection) {
                    Collection<String> flattened = flattenToString((Collection<?>) val);
                    flattened.forEach(it ->
                            reqMethod.addHeader(keyAsString, it)
                    );
                } else {
                    reqMethod.setHeader(keyAsString, val.toString());
                }
            }
        }
        final HttpResponseDecorator resp = new HttpResponseDecorator(
                this.client.execute(reqMethod, delegate.getContext()),
                delegate.getContext(), null);
        try {
            int status = resp.getStatusLine().getStatusCode();
            HttpResponseHandler responseHandler = delegate.findResponseHandler(status);

            HttpEntity entity = resp.getEntity();
            try {
                if (entity == null || entity.getContentLength() == 0) {
                    return responseHandler.handle(resp, EMPTY);
                } else {
                    return responseHandler.handle(resp, this.parseResponse(resp, acceptContentType));
                }
            } catch (Exception ex) {
                // A Groovy closure coerced to HttpResponseHandler has its checked exceptions wrapped by the proxy
                throw new ResponseParseException(resp, ex instanceof UndeclaredThrowableException ? ex.getCause() : ex);
            }
        } finally {
            if (responseSpecification instanceof ResponseSpecificationImpl && ((ResponseSpecificationImpl) responseSpecification).hasBodyAssertionsDefined()) {
                HttpEntity entity = resp.getEntity();
                if (entity != null) EntityUtils.consumeQuietly(entity);
            }
            // Close idle connections to the server
            ConnectionConfig connectionConfig = connectionConfig();
            if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
                ConnectionConfig.CloseIdleConnectionConfig closeConnectionConfig = connectionConfig.closeIdleConnectionConfig();
                client.getConnectionManager().closeIdleConnections(closeConnectionConfig.getIdleTime(), closeConnectionConfig.getTimeUnit());
            }
        }
    }

    private boolean doesntHaveEntity(HttpRequestBase reqMethod) {
        // Port of (!reqMethod.hasProperty("entity") || reqMethod.entity ?.contentType == null)
        if (!(reqMethod instanceof HttpEntityEnclosingRequestBase)) {
            return true;
        }

        HttpEntity entity = ((HttpEntityEnclosingRequestBase) reqMethod).getEntity();
        return entity == null || entity.getContentType() == null;
    }

    private boolean shouldApplyContentTypeFromRestAssuredConfigDelegate(HTTPBuilder.RequestConfigDelegate delegate, HttpRequestBase reqMethod) {
        String requestContentType = delegate.getRequestContentType();
        return allowContentType && requestContentType != null && !requestContentType.equals(ANY.toString()) &&
                doesntHaveEntity(reqMethod) &&
                Arrays.stream(reqMethod.getAllHeaders()).noneMatch(header ->
                        header.getName().equalsIgnoreCase(CONTENT_TYPE)
                );
    }

    /**
     * We override this method because ParserRegistry.getContentType(..) called by
     * the super method throws an exception if no content-type is available in the response
     * and then HTTPBuilder for some reason uses the streaming octet parser instead of the
     * defaultParser in the ParserRegistry to parse the response. To fix this we set the
     * content-type of the defaultParser if registered to Rest Assured to the response if no
     * content-type is defined.
     */
    protected Object parseResponse(HttpResponse resp, Object contentType) throws IOException {
        if (parser != null && ANY.toString().equals(contentType.toString())) {
            try {
                HttpResponseContentTypeFinder.findContentType(resp);
            } catch (IllegalArgumentException ignored) {
                // This means that no content-type is defined the response
                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    resp.setEntity(new HttpEntityWrapper(entity) {

                        public org.apache.http.Header getContentType() {
                            // We don't use CONTENT_TYPE field because of issue 253 (no tests for this!)
                            return new BasicHeader("Content-Type", parser.getContentType());
                        }
                    });
                }
            }
        }
        return super.parseResponse(resp, contentType);
    }

    /**
     * Flattens a collection header value to the strings that are sent as separate headers, like Groovy's
     * <code>collection.flatten().collect { it?.toString() }</code> did: nested collections, iterators and arrays are
     * flattened recursively, a present {@link Optional} is replaced by its value and an empty one is dropped,
     * <code>null</code> is kept, and a {@link Set} drops duplicates (a {@link SortedSet} also sorts with its comparator).
     */
    @SuppressWarnings("unchecked")
    static Collection<String> flattenToString(Collection<?> collection) {
        Collection<Object> flattened;
        if (collection instanceof SortedSet) {
            flattened = new TreeSet<>((Comparator<Object>) ((SortedSet<?>) collection).comparator());
        } else if (collection instanceof Set) {
            flattened = new LinkedHashSet<>();
        } else {
            flattened = new ArrayList<>();
        }
        flattenInto(collection.iterator(), flattened);
        List<String> strings = new ArrayList<>(flattened.size());
        for (Object element : flattened) {
            strings.add(element == null ? null : GroovyStyleToString.toString(element));
        }
        return strings;
    }

    private static void flattenInto(Iterator<?> elements, Collection<Object> flattened) {
        while (elements.hasNext()) {
            Object element = elements.next();
            if (element instanceof Iterator) {
                flattenInto((Iterator<?>) element, flattened);
            } else if (element instanceof Collection) {
                flattenInto(((Collection<?>) element).iterator(), flattened);
            } else if (element != null && element.getClass().isArray()) {
                List<Object> items = new ArrayList<>();
                for (int i = 0; i < Array.getLength(element); i++) {
                    items.add(Array.get(element, i));
                }
                flattenInto(items.iterator(), flattened);
            } else if (element instanceof Optional) {
                ((Optional<?>) element).ifPresent(flattened::add);
            } else {
                flattened.add(element);
            }
        }
    }


    private ConnectionConfig connectionConfig() {
        return config == null ? ConnectionConfig.connectionConfig() : config.getConnectionConfig();
    }

}
