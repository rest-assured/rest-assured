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

/**
 * Specify how redirects should behave.
 */
public interface RedirectSpecification {

    /**
     * Limit the number of redirects to prevent infinite loops. Default is <code>100</code>.
     *
     * @param maxNumberOfRedirect The max number of redirects allowed
     * @return The RequestSpecification
     */
    RequestSpecification max(int maxNumberOfRedirect);

    /**
     * Defines whether redirects should be followed automatically. Default is <code>true</code>.
     *
     * @param followRedirects <code>true</code> means redirects will be followed automatically
     * @return The RequestSpecification
     */
    RequestSpecification follow(boolean followRedirects);

    /**
     * Defines whether circular redirects are allowed. Default is <code>false</code>.
     *
     * @param allowCircularRedirects <code>true</code> means circular redirects are allowed.
     * @return The RequestSpecification
     */
    RequestSpecification allowCircular(boolean allowCircularRedirects);

    /**
     * Defines whether relative redirects should be allowed. Default is <code>false</code>.
     *
     * @param rejectRelativeRedirects <code>true</code> means relative redirects are rejected.
     * @return The RequestSpecification
     */
    RequestSpecification rejectRelative(boolean rejectRelativeRedirects);
}
