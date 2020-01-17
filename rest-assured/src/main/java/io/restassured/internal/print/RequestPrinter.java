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
import org.apache.commons.lang3.SystemUtils;

import java.io.InputStream;
import java.io.PrintStream;
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
                               PrintStream stream, boolean shouldPrettyPrint) {
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

        final String logString = StringUtils.removeEnd(builder.toString(), SystemUtils.LINE_SEPARATOR);
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
        builder.append(SystemUtils.LINE_SEPARATOR);
    }

    private static void addBody(FilterableRequestSpecification requestSpec, StringBuilder builder, boolean shouldPrettyPrint) {
        builder.append("Body:");
        if (requestSpec.getBody() != null) {
            final String body;
            if (shouldPrettyPrint) {
                body = new Prettifier().getPrettifiedBodyIfPossible(requestSpec);
            } else {
                body = requestSpec.getBody();
            }
            builder.append(SystemUtils.LINE_SEPARATOR).append(body);
        } else {
            appendTab(appendTwoTabs(builder)).append(NONE);
        }
    }

    private static void addCookies(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Cookies:");
        final Cookies cookies = requestSpec.getCookies();
        if (!cookies.exist()) {
            appendTwoTabs(builder).append(NONE).append(SystemUtils.LINE_SEPARATOR);
        }
        int i = 0;
        for (Cookie cookie : cookies) {
            if (i++ == 0) {
                appendTwoTabs(builder);
            } else {
                appendFourTabs(builder);
            }
            builder.append(cookie).append(SystemUtils.LINE_SEPARATOR);
        }
    }

    private static void addHeaders(FilterableRequestSpecification requestSpec, Set<String> blacklistedHeaders,
                                   StringBuilder builder) {
        builder.append("Headers:");
        final Headers headers = requestSpec.getHeaders();
        if (!headers.exist()) {
            appendTwoTabs(builder).append(NONE).append(SystemUtils.LINE_SEPARATOR);
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
                builder.append(processedHeader).append(SystemUtils.LINE_SEPARATOR);
            }
        }
    }


    private static void addMultiParts(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Multiparts:");
        final List<MultiPartSpecification> multiParts = requestSpec.getMultiPartParams();
        if (multiParts.isEmpty()) {
            appendTwoTabs(builder).append(NONE).append(SystemUtils.LINE_SEPARATOR);
        } else {
            for (int i = 0; i < multiParts.size(); i++) {
                MultiPartSpecification multiPart = multiParts.get(i);
                if (i == 0) {
                    appendTwoTabs(builder);
                } else {
                    appendFourTabs(builder.append(SystemUtils.LINE_SEPARATOR));
                }

                builder.append("------------");
                appendFourTabs(appendFourTabs(builder.append(SystemUtils.LINE_SEPARATOR))
                        .append("Content-Disposition: ")
                        .append(requestSpec.getContentType().replace("multipart/", ""))
                        .append("; name = ").append(multiPart.getControlName())
                        .append(multiPart.hasFileName() ? "; filename = " + multiPart.getFileName() : "")
                        .append(SystemUtils.LINE_SEPARATOR))
                        .append("Content-Type: ")
                        .append(multiPart.getMimeType());
                final Map<String, String> headers = multiPart.getHeaders();
                if (!headers.isEmpty()) {
                    final Set<Entry<String, String>> headerEntries = headers.entrySet();
                    for (Entry<String, String> headerEntry : headerEntries) {
                        appendFourTabs(appendFourTabs(builder.append(SystemUtils.LINE_SEPARATOR))
                                .append(headerEntry.getKey()).append(": ").append(headerEntry.getValue()));
                    }
                }
                builder.append(SystemUtils.LINE_SEPARATOR); // There's a newline between headers and content in multi-parts
                if (multiPart.getContent() instanceof InputStream) {
                    appendFourTabs(builder.append(SystemUtils.LINE_SEPARATOR)).append("<inputstream>");
                } else {
                    Parser parser = Parser.fromContentType(multiPart.getMimeType());
                    String prettified = new Prettifier().prettify(multiPart.getContent().toString(), parser);
                    String prettifiedIndented = StringUtils.replace(prettified, SystemUtils.LINE_SEPARATOR, SystemUtils.LINE_SEPARATOR + TAB + TAB + TAB + TAB);
                    appendFourTabs(builder.append(SystemUtils.LINE_SEPARATOR)).append(prettifiedIndented);
                }
            }
            builder.append(SystemUtils.LINE_SEPARATOR);
        }
    }

    private static void addSingle(StringBuilder builder, String str, String requestPath) {
        appendTab(builder.append(str)).append(requestPath).append(SystemUtils.LINE_SEPARATOR);
    }

    private static void addMapDetails(StringBuilder builder, String title, Map<String, ?> map) {
        appendTab(builder.append(title));
        if (map.isEmpty()) {
            builder.append(NONE).append(SystemUtils.LINE_SEPARATOR);
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
                builder.append(SystemUtils.LINE_SEPARATOR);
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