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

package io.restassured.config;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Allow you to configure settings for headers.
 */
public class HeaderConfig implements Config {

    private static final String ACCEPT_HEADER_NAME = "accept";
    private static final String CONTENT_TYPE_HEADER_NAME = "content-type";

    private final Map<String, Boolean> headersToOverwrite;
    private final boolean isUserDefined;

    /**
     * Create a new instance of {@link HeaderConfig}.
     */
    public HeaderConfig() {
        this(newHashMapReturningFalseByDefault(CONTENT_TYPE_HEADER_NAME, ACCEPT_HEADER_NAME), false);
    }

    private HeaderConfig(Map<String, Boolean> headersToOverwrite, boolean isUserDefined) {
        this.headersToOverwrite = headersToOverwrite;
        this.isUserDefined = isUserDefined;
    }

    /**
     * Define headers that should be overwritten instead of merged adding headers or using request specifications. Note that
     * by default all headers are merged except the {@value #ACCEPT_HEADER_NAME} and {@value #CONTENT_TYPE_HEADER_NAME} headers.
     * For example, if the header with name <code>header1</code> is <i>not</i> marked as overwritable (default) and you do the following:
     * <pre>
     * given().header("header1", "value1").header("header1, "value2"). ..
     * </pre>
     * <p/>
     * Then <code>header1</code> will be sent twice in the request:
     * <pre>
     * header1: value1
     * header1: value2
     * </pre>
     * <p/>
     * If you configure <code>header1</code> to be overwritable by doing:
     * <pre>
     * given().
     *         config(RestAssured.config().headerConfig(headerConfig().overwriteHeadersWithName("header1")).
     *         header("header1", "value1").
     *         header("header1", "value2").
     *         ...
     * </pre>
     * then <code>header1</code> will only be sent once:
     * <pre>
     * header1: value2
     * </pre>
     *
     * @param headerName            The header name to overwrite.
     * @param additionalHeaderNames Additional header names to overwrite (optional).
     * @return A new instance of {@link HeaderConfig}.
     */
    public HeaderConfig overwriteHeadersWithName(String headerName, String... additionalHeaderNames) {
        notNull(headerName, "Header name");
        Map<String, Boolean> map = newHashMapReturningFalseByDefault(headerName);
        if (additionalHeaderNames != null && additionalHeaderNames.length > 0) {
            for (String additionalHeaderName : additionalHeaderNames) {
                map.put(additionalHeaderName.toUpperCase(), true);
            }
        }
        return new HeaderConfig(map, true);
    }

    /**
     * Define headers that should be be merged instead of overwritten when adding headers or using request specifications. Note that
     * by default all headers are merged except the {@value #ACCEPT_HEADER_NAME} and {@value #CONTENT_TYPE_HEADER_NAME} headers.
     * This method is thus mainly used to change to merge behavior for headers that by default are overwritten or the revert
     * changes of a request specification merge.
     *
     * @param headerName            The header name to merge.
     * @param additionalHeaderNames Additional header names to merge (optional).
     * @return A new instance of {@link HeaderConfig}.
     */
    public HeaderConfig mergeHeadersWithName(String headerName, String... additionalHeaderNames) {
        notNull(headerName, "Header name");
        Map<String, Boolean> map = newHashMapReturningFalseByDefault();
        map.put(headerName, false);
        if (additionalHeaderNames != null && additionalHeaderNames.length > 0) {
            for (String additionalHeaderName : additionalHeaderNames) {
                map.put(additionalHeaderName.toUpperCase(), false);
            }
        }
        return new HeaderConfig(map, true);
    }

    /**
     * Returns whether or not the specified header should be returned
     *
     * @param headerName The header name to check.
     * @return <code>true</code> if header should be overwritten, <code>false</code> otherwise.
     */
    public boolean shouldOverwriteHeaderWithName(String headerName) {
        notNull(headerName, "Header name");
        return headersToOverwrite.get(headerName.toUpperCase());
    }

    /**
     * Syntactic sugar, same as calling <code>new {@link HeaderConfig#HeaderConfig()}</code>.
     *
     * @return a new instance of {@link HeaderConfig}.
     */
    public static HeaderConfig headerConfig() {
        return new HeaderConfig();
    }

    private static Map<String, Boolean> newHashMapReturningFalseByDefault(final String... headerNamesToOverwrite) {
        return new HashMap<String, Boolean>() {
            {
                for (String headerName : headerNamesToOverwrite) {
                    put(headerName.toUpperCase(), true);
                }
            }

            @Override
            public Boolean get(Object key) {
                Boolean aBoolean = super.get(key);
                if (aBoolean == null) {
                    return Boolean.FALSE;
                }
                return aBoolean;
            }
        };
    }

    public boolean isUserConfigured() {
        return isUserDefined;
    }
}
