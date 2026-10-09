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

package io.restassured.internal.http;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ContentTypeSubTypeExtractor {

    private static final String SEMICOLON = ";";

    /**
     * Returns the value of the parameter named <code>subType</code> (matched case-insensitively) in <code>contentType</code>,
     * with surrounding whitespace and double quotes removed. If the parameter occurs more than once, the last occurrence wins.
     *
     * @return The value, or <code>null</code> if not found.
     */
    public static String getSubTypeValueFromContentType(String contentType, String subType) {
        if (isBlank(contentType) || !StringUtils.containsIgnoreCase(contentType, subType)) {
            return null;
        }
        String foundSubType = null;
        for (String parameter : contentType.split(SEMICOLON)) {
            // Split on the first "=" only since the value may contain "=" (e.g. a multipart boundary)
            int equalsIndex = parameter.indexOf('=');
            if (equalsIndex < 0 || equalsIndex == parameter.length() - 1) {
                continue;
            }
            String name = parameter.substring(0, equalsIndex);
            if (name.trim().equalsIgnoreCase(subType)) {
                String value = parameter.substring(equalsIndex + 1);
                foundSubType = StringUtils.removeEnd(StringUtils.removeStart(value.trim(), "\""), "\"");
            }
        }
        return foundSubType;
    }
}
