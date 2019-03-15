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

package io.restassured.module.mockmvc.specification;


import io.restassured.specification.LogSpecification;

/**
 * The request logging specification
 */
public interface MockMvcRequestLogSpecification extends LogSpecification<MockMvcRequestSpecification> {

    /**
     * Logs only the parameters of the request. Same as {@link #parameters()} but slightly shorter syntax.
     *
     * @return The response specification
     */
    MockMvcRequestSpecification params();

    /**
     * Logs only the parameters of the request. Same as {@link #params()} but more explicit syntax.
     *
     * @return The response specification
     */
    MockMvcRequestSpecification parameters();
}
