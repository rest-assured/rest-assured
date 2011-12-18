/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.specification;

/**
 * Base interface for request- and response log specifications.
 */
public interface LogSpecification<T extends RequestSender> {

    /**
     * Logs only the content of the body.
     *
     * @return The specification
     */
    T body();

    /**
     * Logs everything in the specification, including e.g. headers, cookies, body.
     *
     * @return The specification
     */
    T all();

    /**
     ** Logs everything in the specification, including e.g. headers, cookies, body.
     *
     * @return The specification
     */
    T everything();

    /**
     * Logs only the headers.
     *
     * @return The specification
     */
    T headers();

    /**
     * Logs only the cookies.
     *
     * @return The specification
     */
    T cookies();
}
