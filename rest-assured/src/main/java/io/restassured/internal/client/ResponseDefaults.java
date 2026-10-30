/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.client;

import io.restassured.RestAssured;
import io.restassured.specification.ResponseSpecification;

/**
 * The defaults that <code>then()</code> validates a response with.
 *
 * @param rootPath              The root path of the body expectations
 * @param responseSpecification The default response specification, or <code>null</code>
 */
public record ResponseDefaults(String rootPath, ResponseSpecification responseSpecification) {

    /**
     * No root path and no response specification, for the responses of a client of the Spring modules, which doesn't
     * read the static fields of {@link RestAssured}
     */
    public static final ResponseDefaults NONE = new ResponseDefaults(RestAssured.DEFAULT_BODY_ROOT_PATH, null);

    /**
     * @return {@link RestAssured#rootPath} and {@link RestAssured#responseSpecification} as they are now
     */
    public static ResponseDefaults ofStaticFields() {
        return new ResponseDefaults(RestAssured.rootPath, RestAssured.responseSpecification);
    }
}
