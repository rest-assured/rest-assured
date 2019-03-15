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

package io.restassured.specification;

import io.restassured.filter.log.LogDetail;
import org.hamcrest.Matcher;

/**
 * A response specification that also supports getting the defined values. Intended for Filters.
 */
public interface FilterableResponseSpecification extends ResponseSpecification {

    /**
     * @return The Hamcrest matcher that needs to be match the status code (may be <code>null</code>).
     */
    Matcher<Integer> getStatusCode();

    /**
     * @return The Hamcrest matcher that needs to be match the status line (may be <code>null</code>).
     */
    Matcher<String> getStatusLine();

    /**
     * @return <code>true</code> if any header assertions are defined
     */
    boolean hasHeaderAssertions();

    /**
     * @return <code>true</code> if any cookie assertions are defined
     */
    boolean hasCookieAssertions();

    /**
     * @return The response content type
     */
    String getResponseContentType();

    /**
     * @return The body root path when expecting XML or JSON
     */
    String getRootPath();

    /**
     * @return  The log detail for the response
     */
    LogDetail getLogDetail();
}
