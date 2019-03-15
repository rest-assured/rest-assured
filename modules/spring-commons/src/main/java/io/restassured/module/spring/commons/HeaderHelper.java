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
package io.restassured.module.spring.commons;

import io.restassured.config.EncoderConfig;
import io.restassured.config.HeaderConfig;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.module.spring.commons.config.SpecificationConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class HeaderHelper {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CHARSET = "charset";

    private HeaderHelper() {
    }

    public static Headers headers(Headers requestHeaders, Map<String, ?> headers, SpecificationConfig config) {
        notNull(headers, "headers");
        List<Header> headerList = new ArrayList<Header>();
        if (requestHeaders.exist()) {
            for (Header requestHeader : requestHeaders) {
                headerList.add(requestHeader);
            }
        }

        for (Map.Entry<String, ?> stringEntry : headers.entrySet()) {
            Object value = stringEntry.getValue();
            if (value instanceof List) {
                List<?> values = (List<?>) value;
                for (Object headerValue : values) {
                    headerList.add(new Header(stringEntry.getKey(), Serializer.serializeIfNeeded(headerValue,
                            getRequestContentType(requestHeaders), config)));
                }
            } else {
                headerList.add(new Header(stringEntry.getKey(), Serializer.serializeIfNeeded(value,
                        getRequestContentType(requestHeaders), config)));
            }
        }
        return new Headers(headerList);
    }

    public static String getRequestContentType(Headers requestHeaders) {
        Header header = requestHeaders.get(CONTENT_TYPE);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    public static Headers headers(Headers requestHeaders, Headers headersToAdd, HeaderConfig headerConfig) {
        notNull(headersToAdd, "Headers");
        if (headersToAdd.exist()) {
            List<Header> headerList = new ArrayList<Header>();
            if (requestHeaders.exist()) {
                for (Header requestHeader : requestHeaders) {
                    headerList.add(requestHeader);
                }
            }

            for (Header requestHeader : headersToAdd) {
                headerList.add(requestHeader);
            }
            return new Headers(removeMergedHeadersIfNeeded(headerList, headerConfig));
        }
        return requestHeaders;
    }

    // TODO Extract content-type from headers and apply charset if needed!
    public static String findContentType(Headers headers, List<Object> multiParts, SpecificationConfig config) {
        String requestContentType = headers.getValue(CONTENT_TYPE);
        if (StringUtils.isBlank(requestContentType) && !multiParts.isEmpty()) {
            requestContentType = "multipart/" + config.getMultiPartConfig().defaultSubtype();
        }

        EncoderConfig encoderConfig = config.getEncoderConfig();
        if (requestContentType != null && encoderConfig.shouldAppendDefaultContentCharsetToContentTypeIfUndefined() && !StringUtils.containsIgnoreCase(requestContentType, CHARSET)) {
            // Append default charset to request content type
            requestContentType += "; charset=";
            if (encoderConfig.hasDefaultCharsetForContentType(requestContentType)) {
                requestContentType += encoderConfig.defaultCharsetForContentType(requestContentType);
            } else {
                requestContentType += encoderConfig.defaultContentCharset();
            }
        }
        return requestContentType;
    }

    public static String buildApplicationFormEncodedContentType(SpecificationConfig config, String baseContentType) {
        String contentType = baseContentType;
        EncoderConfig encoderConfig = config.getEncoderConfig();
        if (encoderConfig.shouldAppendDefaultContentCharsetToContentTypeIfUndefined()) {
            contentType += "; charset=";
            if (encoderConfig.hasDefaultCharsetForContentType(contentType)) {
                contentType += encoderConfig.defaultCharsetForContentType(contentType);
            } else {
                contentType += encoderConfig.defaultContentCharset();

            }
        }
        return contentType;
    }

    public static Object[] mapToArray(Map<String, ?> map) {
        if (map == null) {
            return new Object[0];
        }
        return map.values().toArray(new Object[map.values().size()]);
    }

    private static List<Header> removeMergedHeadersIfNeeded(List<Header> headerList, HeaderConfig headerConfig) {
        List<Header> filteredList = new ArrayList<Header>();
        for (Header header : headerList) {
            String headerName = header.getName();
            if (headerConfig.shouldOverwriteHeaderWithName(headerName)) {
                int index = -1;
                for (int i = 0; i < filteredList.size(); i++) {
                    Header filteredHeader = filteredList.get(i);
                    if (filteredHeader.hasSameNameAs(header)) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    filteredList.remove(index);
                }
            }

            filteredList.add(header);
        }
        return filteredList;
    }

    public static Headers headers(final Headers requestHeaders, final String headerName, final Object headerValue,
                                  final SpecificationConfig config,
                                  Object... additionalHeaderValues) {
        notNull(headerName, "Header name");
        notNull(headerValue, "Header value");

        List<Header> headerList = new ArrayList<Header>() {{
            add(new Header(headerName, Serializer.serializeIfNeeded(headerValue,
                    getRequestContentType(requestHeaders), config)));
        }};

        if (additionalHeaderValues != null) {
            for (Object additionalHeaderValue : additionalHeaderValues) {
                headerList.add(new Header(headerName, Serializer.serializeIfNeeded(additionalHeaderValue,
                        getRequestContentType(requestHeaders), config)));
            }
        }
        return new Headers(headerList);
    }
}
