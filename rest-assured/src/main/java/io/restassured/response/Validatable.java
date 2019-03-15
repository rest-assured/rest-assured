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

package io.restassured.response;

public interface Validatable<T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>> {

    /**
     * Returns a validatable response that's lets you validate the response. Usage example:
     * <p/>
     * <pre>
     * given().
     *         param("firstName", "John").
     *         param("lastName", "Doe").
     * when().
     *         get("/greet").
     * then().
     *         body("greeting", equalTo("John Doe"));
     * </pre>
     *
     * @return A validatable response
     */
    T then();
}
