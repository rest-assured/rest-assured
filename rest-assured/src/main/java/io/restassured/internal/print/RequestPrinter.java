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

package io.restassured.internal.print;

import io.restassured.config.PrintableStream;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.NoParameterValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.parsing.Parser;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.ProxySpecification;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.restassured.filter.log.LogDetail.*;

/**
 * A request printer can be used to print a request.
 */
public class RequestPrinter {
    private static final String TAB = "\t";
    private static final String EQUALS = "=";
    private static final String NONE = "<none>";
    private static final String BLACKLISTED = "[ BLACKLISTED ]";

    public static String print(FilterableRequestSpecification requestSpec, String requestMethod, String completeRequestUri,
                               LogDetail logDetail, Set<String> blacklistedHeaders,
                               PrintableStream stream, boolean shouldPrettyPrint) {
        final StringBuilder builder = new StringBuilder();
        if (logDetail == ALL || logDetail == METHOD) {
            addSingle(builder, "Request method:", requestMethod);
        }
        if (logDetail == ALL || logDetail == URI) {
            addSingle(builder, "Request URI:", completeRequestUri);
        }
        if (logDetail == ALL) {
            addProxy(requestSpec, builder);
        }
        if (logDetail == ALL || logDetail == PARAMS) {
            addMapDetails(builder, "Request params:", requestSpec.getRequestParams());
            addMapDetails(builder, "Query params:", requestSpec.getQueryParams());
            addMapDetails(builder, "Form params:", requestSpec.getFormParams());
            addMapDetails(builder, "Path params:", requestSpec.getNamedPathParams());
        }

        if (logDetail == ALL || logDetail == HEADERS) {
            addHeaders(requestSpec, blacklistedHeaders, builder);
        }
        if (logDetail == ALL || logDetail == COOKIES) {
            addCookies(requestSpec, builder);
        }

        if (logDetail == ALL || logDetail == PARAMS) {
            addMultiParts(requestSpec, builder);
        }

        if (logDetail == ALL || logDetail == BODY) {
            addBody(requestSpec, builder, shouldPrettyPrint);
        }

        final String logString = StringUtils.chomp(builder.toString());
        stream.println(logString);
        return logString;
    }

    private static void addProxy(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Proxy:");
        ProxySpecification proxySpec = requestSpec.getProxySpecification();
        appendThreeTabs(builder);
        if (proxySpec == null) {
            builder.append(NONE);
        } else {
            builder.append(proxySpec.toString());
        }
        builder.append(System.lineSeparator());
    }

    private static void addBody(FilterableRequestSpecification requestSpec, StringBuilder builder, boolean shouldPrettyPrint) {
        builder.append("Body:");
        if (requestSpec.getBody() == null) {
            appendTab(appendTwoTabs(builder)).append(NONE);
        } else {
            final String body;
            if (shouldPrettyPrint) {
                body = new Prettifier().getPrettifiedBodyIfPossible(requestSpec);
            } else {
                body = requestSpec.getBody();
            }
            builder.append(System.lineSeparator()).append(body);
        }
    }

    private static void addCookies(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Cookies:");
        final Cookies cookies = requestSpec.getCookies();
        if (!cookies.exist()) {
            appendTwoTabs(builder).append(NONE).append(System.lineSeparator());
        }
        int i = 0;
        for (Cookie cookie : cookies) {
            if (i++ == 0) {
                appendTwoTabs(builder);
            } else {
                appendFourTabs(builder);
            }
            builder.append(cookie).append(System.lineSeparator());
        }
    }

    private static void addHeaders(FilterableRequestSpecification requestSpec, Set<String> blacklistedHeaders,
                                   StringBuilder builder) {
        builder.append("Headers:");
        final Headers headers = requestSpec.getHeaders();
        if (!headers.exist()) {
            appendTwoTabs(builder).append(NONE).append(System.lineSeparator());
        } else {
            int i = 0;
            for (Header header : headers) {
                if (i++ == 0) {
                    appendTwoTabs(builder);
                } else {
                    appendFourTabs(builder);
                }
                Header processedHeader = header;
                if (blacklistedHeaders.contains(header.getName())) {
                    processedHeader = new Header(header.getName(), BLACKLISTED);
                }
                builder.append(processedHeader).append(System.lineSeparator());
            }
        }
    }


    private static void addMultiParts(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Multiparts:");
        final List<MultiPartSpecification> multiParts = requestSpec.getMultiPartParams();
        if (multiParts.isEmpty()) {
            appendTwoTabs(builder).append(NONE).append(System.lineSeparator());
        } else {
            for (int i = 0; i < multiParts.size(); i++) {
                MultiPartSpecification multiPart = multiParts.get(i);
                if (i == 0) {
                    appendTwoTabs(builder);
                } else {
                    appendFourTabs(builder.append(System.lineSeparator()));
                }

                builder.append("------------");
                appendFourTabs(appendFourTabs(builder.append(System.lineSeparator()))
                        .append("Content-Disposition: ")
                        .append(requestSpec.getContentType().replace("multipart/", ""))
                        .append("; name = ").append(multiPart.getControlName())
                        .append(multiPart.hasFileName() ? "; filename = " + multiPart.getFileName() : "")
                        .append(System.lineSeparator()))
                        .append("Content-Type: ")
                        .append(multiPart.getMimeType());
                final Map<String, String> headers = multiPart.getHeaders();
                if (!headers.isEmpty()) {
                    final Set<Entry<String, String>> headerEntries = headers.entrySet();
                    for (Entry<String, String> headerEntry : headerEntries) {
                        appendFourTabs(appendFourTabs(builder.append(System.lineSeparator()))
                                .append(headerEntry.getKey()).append(": ").append(headerEntry.getValue()));
                    }
                }
                builder.append(System.lineSeparator()); // There's a newline between headers and content in multi-parts
                if (multiPart.getContent() instanceof InputStream) {
                    appendFourTabs(builder.append(System.lineSeparator())).append("<inputstream>");
                } else {
                    Parser parser = Parser.fromContentType(multiPart.getMimeType());
                    String prettified = new Prettifier().prettify(multiPart.getContent().toString(), parser);
                    String prettifiedIndented = StringUtils.replace(prettified, System.lineSeparator(), System.lineSeparator() + TAB + TAB + TAB + TAB);
                    appendFourTabs(builder.append(System.lineSeparator())).append(prettifiedIndented);
                }
            }
            builder.append(System.lineSeparator());
        }
    }

    private static void addSingle(StringBuilder builder, String str, String requestPath) {
        appendTab(builder.append(str)).append(requestPath).append(System.lineSeparator());
    }

    private static void addMapDetails(StringBuilder builder, String title, Map<String, ?> map) {
        appendTab(builder.append(title));
        if (map.isEmpty()) {
            builder.append(NONE).append(System.lineSeparator());
        } else {
            int i = 0;
            for (Entry<String, ?> entry : map.entrySet()) {
                if (i++ != 0) {
                    appendFourTabs(builder);
                }
                final Object value = entry.getValue();
                builder.append(entry.getKey());
                if (!(value instanceof NoParameterValue)) {
                    builder.append(EQUALS).append(value);
                }
                builder.append(System.lineSeparator());
            }
        }
    }

    private static StringBuilder appendFourTabs(StringBuilder builder) {
        appendTwoTabs(appendTwoTabs(builder));
        return builder;
    }

    private static StringBuilder appendTwoTabs(StringBuilder builder) {
        appendTab(appendTab(builder));
        return builder;
    }

    private static StringBuilder appendThreeTabs(StringBuilder builder) {
        appendTwoTabs(appendTab(builder));
        return builder;
    }

    private static StringBuilder appendTab(StringBuilder builder) {
        return builder.append(TAB);
    }
}
