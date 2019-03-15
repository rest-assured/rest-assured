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
 * The request logging specification
 */
public interface RequestLogSpecification extends LogSpecification<RequestSpecification> {

    /**
     * Logs only the parameters of the request. Same as {@link #parameters()} but slightly shorter syntax.
     *
     * @return The request specification
     */
    RequestSpecification params();

    /**
     * Logs only the parameters of the request. Same as {@link #params()} but more explicit syntax.
     *
     * @return The request specification
     */
    RequestSpecification parameters();

    /**
     * Only logs the request uri.
     *
     * @return The request specification
     */
    RequestSpecification uri();

    /**
     * Only logs the request method.
     *
     * @return The request specification
     */
    RequestSpecification method();
}
