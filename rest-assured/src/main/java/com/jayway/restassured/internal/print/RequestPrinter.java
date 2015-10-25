/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.internal.print;

import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.internal.NoParameterValue;
import com.jayway.restassured.internal.support.Prettifier;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.MultiPartSpecification;
import com.jayway.restassured.specification.ProxySpecification;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.filter.log.LogDetail.*;

/**
 * A request printer can be used to print a request.
 */
public class RequestPrinter {
    private static final String TAB = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String EQUALS = "=";
    private static final String NONE = "<none>";

    public static String print(FilterableRequestSpecification requestSpec, String requestMethod, String completeRequestUri,
                               LogDetail logDetail, PrintStream stream, boolean shouldPrettyPrint) {
        final StringBuilder builder = new StringBuilder();
        if (logDetail == ALL || logDetail == METHOD) {
            addSingle(builder, "Request method:", requestMethod);
        }
        if (logDetail == ALL || logDetail == PATH) {
            addSingle(builder, "Request path:", completeRequestUri);
        }
        if (logDetail == ALL) {
            addProxy(requestSpec, builder);
        }
        if (logDetail == ALL || logDetail == PARAMS) {
            addMapDetails(builder, "Request params:", requestSpec.getRequestParams());
            addMapDetails(builder, "Query params:", requestSpec.getQueryParams());
            addMapDetails(builder, "Form params:", requestSpec.getFormParams());
            addMapDetails(builder, "Path params:", requestSpec.getPathParams());
            addMultiParts(requestSpec, builder);
        }

        if (logDetail == ALL || logDetail == HEADERS) {
            addHeaders(requestSpec, builder);
        }
        if (logDetail == ALL || logDetail == COOKIES) {
            addCookies(requestSpec, builder);
        }
        if (logDetail == ALL || logDetail == BODY) {
            addBody(requestSpec, builder, shouldPrettyPrint);
        }
        String logString = builder.toString();
        if (logString.endsWith("\n")) {
            logString = StringUtils.removeEnd(logString, "\n");
        }
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
        builder.append(NEW_LINE);
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
            builder.append(NEW_LINE).append(body);
        } else {
            appendTab(appendTwoTabs(builder)).append(NONE);
        }
    }

    private static void addCookies(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Cookies:");
        final Cookies cookies = requestSpec.getCookies();
        if (!cookies.exist()) {
            appendTwoTabs(builder).append(NONE).append(NEW_LINE);
        }
        int i = 0;
        for (Cookie cookie : cookies) {
            if (i++ == 0) {
                appendTwoTabs(builder);
            } else {
                appendFourTabs(builder);
            }
            builder.append(cookie).append(NEW_LINE);
        }
    }

    private static void addHeaders(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Headers:");
        final Headers headers = requestSpec.getHeaders();
        if (!headers.exist()) {
            appendTwoTabs(builder).append(NONE).append(NEW_LINE);
        } else {
            int i = 0;
            for (Header header : headers) {
                if (i++ == 0) {
                    appendTwoTabs(builder);
                } else {
                    appendFourTabs(builder);
                }
                builder.append(header).append(NEW_LINE);
            }
        }
    }


    private static void addMultiParts(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Multiparts:");
        final List<MultiPartSpecification> multiParts = requestSpec.getMultiPartParams();
        if (multiParts.isEmpty()) {
            appendTwoTabs(builder).append(NONE).append(NEW_LINE);
        } else {
            int i = 0;
            for (MultiPartSpecification multiPart : multiParts) {
                if (i++ == 0) {
                    appendTwoTabs(builder);
                } else {
                    appendFourTabs(builder);
                }
                builder.append(multiPart).append(NEW_LINE);
            }
        }
    }

    private static void addSingle(StringBuilder builder, String str, String requestPath) {
        appendTab(builder.append(str)).append(requestPath).append(NEW_LINE);
    }

    private static void addMapDetails(StringBuilder builder, String title, Map<String, ?> map) {
        appendTab(builder.append(title));
        if (map.isEmpty()) {
            builder.append(NONE).append(NEW_LINE);
        } else {
            int i = 0;
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                if (i++ != 0) {
                    appendFourTabs(builder);
                }
                final Object value = entry.getValue();
                builder.append(entry.getKey());
                if (!(value instanceof NoParameterValue)) {
                    builder.append(EQUALS).append(value);
                }
                builder.append(NEW_LINE);
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
