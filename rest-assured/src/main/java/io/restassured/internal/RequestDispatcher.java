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

package io.restassured.internal;

import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.http.ContentType;
import io.restassured.internal.common.util.GroovyStyleToString;
import io.restassured.internal.http.ContentDecoding;
import io.restassured.internal.http.EncoderRegistry;
import io.restassured.internal.http.ResponseParseException;
import io.restassured.internal.http.client.TransportExceptions;
import io.restassured.internal.util.SafeExceptionRethrower;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static io.restassured.http.ContentType.ANY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Builds the request that an HTTP client sends, and hands the response of the HTTP client to the REST Assured response.
 */
final class RequestDispatcher {
    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH = "multipart/";

    private RequestDispatcher() {
    }

    /**
     * Builds the request with the headers that HttpClient 4 got from REST Assured until 7.0: the headers of the request
     * specification, the cookies and the headers of the authentication scheme are in <code>headers</code>, as put
     * there before the request is sent. A collection value is sent as one header per element, and a <code>null</code>
     * value removes the header.
     *
     * @param acceptContentType  the expected response content type, a String, a {@link ContentType} or a Matcher
     * @param requestContentType the content type to encode the body with, or <code>null</code> to use
     *                           <code>acceptContentType</code>
     * @param body               the body, or <code>null</code> if the request has none
     * @param allowContentType   <code>false</code> if the request must not have a content type
     * @param acceptHeaderDefined whether the request specification has an Accept header
     * @param options            how the request is sent
     */
    static HttpRequest newRequest(String method, URI uri, Map<?, ?> headers, Object acceptContentType, String requestContentType,
                                  Object body, boolean allowContentType, boolean acceptHeaderDefined, EncoderRegistry encoders,
                                  ContentDecoding contentDecoding, RequestOptions options) {
        String contentType = requestContentType == null ? Objects.toString(acceptContentType, null) : requestContentType;
        RequestBody requestBody = body == null ? null : encode(encoders, allowContentType, contentType, body);

        List<HttpHeader> requestHeaders = new ArrayList<>();
        if (!acceptHeaderDefined) {
            String accept = acceptContentType instanceof ContentType ? ((ContentType) acceptContentType).getAcceptHeader() : acceptContentType.toString();
            setHeader(requestHeaders, ACCEPT, accept);
        }
        if (allowContentType && contentType != null && !contentType.equals(ANY.toString()) && (requestBody == null || requestBody.getContentType() == null)
                && headerValues(requestHeaders, CONTENT_TYPE).isEmpty()) {
            setHeader(requestHeaders, CONTENT_TYPE, trim(contentType));
        }
        for (Map.Entry<?, ?> header : copyOf(headers).entrySet()) {
            String name = header.getKey().toString();
            Object value = header.getValue();
            if (value == null || name.equalsIgnoreCase(CONTENT_TYPE) && !allowContentType) {
                requestHeaders.removeIf(it -> it.name().equalsIgnoreCase(name));
            } else if (!name.equalsIgnoreCase(CONTENT_TYPE) || !value.toString().startsWith(MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)) {
                // A multipart content type is left to the body, which adds the boundary
                if (value instanceof Collection) {
                    for (String element : flattenToString((Collection<?>) value)) {
                        requestHeaders.add(new HttpHeader(name, element));
                    }
                } else {
                    setHeader(requestHeaders, name, value.toString());
                }
            }
        }
        String acceptEncoding = contentDecoding.acceptEncoding(headerValues(requestHeaders, ContentDecoding.ACCEPT_ENCODING));
        if (acceptEncoding != null) {
            setHeader(requestHeaders, ContentDecoding.ACCEPT_ENCODING, acceptEncoding);
        }
        return new HttpRequest(method, uri, requestHeaders, requestBody, options);
    }

    private static RequestBody encode(EncoderRegistry encoders, boolean allowContentType, String contentType, Object body) {
        try {
            return allowContentType ? encoders.getAt(contentType).encode(contentType, body) : encoders.encodeWithoutContentType(body);
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    // A copy, since the order of a HashMap depends on how it was filled
    private static Map<Object, Object> copyOf(Map<?, ?> headers) {
        Map<Object, Object> copy = new HashMap<>();
        for (Map.Entry<?, ?> header : headers.entrySet()) {
            if (header.getKey() != null) {
                copy.put(header.getKey().toString(), header.getValue());
            }
        }
        return copy;
    }

    // Replaces the first header with this name (ignoring case) in place, or adds one, like HttpClient 4's setHeader
    private static void setHeader(List<HttpHeader> headers, String name, String value) {
        HttpHeader header = new HttpHeader(name, value);
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).name().equalsIgnoreCase(name)) {
                headers.set(i, header);
                return;
            }
        }
        headers.add(header);
    }

    private static List<String> headerValues(List<HttpHeader> headers, String name) {
        List<String> values = new ArrayList<>();
        for (HttpHeader header : headers) {
            if (header.name().equalsIgnoreCase(name)) {
                values.add(header.value());
            }
        }
        return values;
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

    /**
     * Parses the response into the REST Assured response of the assertion closure. With body assertions the body is
     * read right away, otherwise it's read when the REST Assured response is asked for it.
     */
    static void receive(HttpResponse response, ContentDecoding contentDecoding, boolean hasBodyAssertions,
                        ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure) throws IOException {
        InputStream body = response.body();
        boolean bodyIsReadLater = false;
        try {
            try {
                String contentEncoding = response.firstHeader(ContentDecoding.CONTENT_ENCODING);
                // The decoded length of an encoded body isn't known, so it's parsed even when the encoded body is empty
                if (body == null || response.contentLength() == 0 && !contentDecoding.decodes(contentEncoding)) {
                    // Closing the empty body releases its connection
                    closeQuietly(body);
                    assertionClosure.call(response, EMPTY);
                } else {
                    InputStream content = contentDecoding.decode(body, contentEncoding);
                    assertionClosure.call(response, content);
                    bodyIsReadLater = !hasBodyAssertions;
                }
            } catch (Exception ex) {
                // A timeout while the body is read throws REST Assured's timeout exception, as one before the response does
                Throwable cause = ex instanceof IOException ? TransportExceptions.translate((IOException) ex) : ex;
                throw new ResponseParseException(response.statusCode(), response.reasonPhrase(), cause);
            }
        } finally {
            if (hasBodyAssertions) {
                closeQuietly(body);
            }
            // A body that is read later closes the response once it has been read instead
            if (!bodyIsReadLater) {
                response.close();
            }
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // Like HttpClient 4's EntityUtils.consumeQuietly
        }
    }
}
