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

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Verifies if a String is a URI
 */
public class UriValidator {

    /**
     * Checks if the <code>potentialUri</code> is a URI.
     *
     * @param potentialUri The URI to check.
     * @return <code>true</code> if it is a URI, <code>false</code> otherwise.
     */
    public static boolean isUri(String potentialUri) {
        if (StringUtils.isBlank(potentialUri)) {
            return false;
        }

        try {
            URI uri = new URI(potentialUri);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
