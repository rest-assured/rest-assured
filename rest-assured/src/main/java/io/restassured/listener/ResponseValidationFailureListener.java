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

package io.restassured.listener;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * An interface that can be implemented to get callback when a failure occurs during test execution.
 */
@FunctionalInterface
public interface ResponseValidationFailureListener {

    /**
     * Called when a failure occurs
     *
     * @param requestSpecification The request specification
     * @param responseSpecification The reponse specification
     * @param response The actual response
     */
    void onFailure(RequestSpecification requestSpecification,
                   ResponseSpecification responseSpecification,
                   Response response);
}
