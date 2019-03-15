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


import org.hamcrest.Matcher;

/**
 * The response logging specification
 */
public interface ResponseLogSpecification extends LogSpecification<ResponseSpecification> {

    /**
     * Logs only the status line (includes the status code)
     *
     * @return The response specification
     */
    ResponseSpecification status();

    /**
     * Logs everything only if an error occurs (status code >= 400).
     *
     * @return The response specification
     */
    ResponseSpecification ifError();

    /**
     * Logs everything only if if the status code is equal to <code>statusCode</code>.
     *
     * @param statusCode The status code
     * @return The response specification
     */
    ResponseSpecification ifStatusCodeIsEqualTo(int statusCode);

    /**
     * Logs everything only if if the status code matches the supplied <code>matcher</code>
     *
     * @param matcher The hamcrest matcher
     * @return The response specification
     */
    ResponseSpecification ifStatusCodeMatches(Matcher<Integer> matcher);
}
