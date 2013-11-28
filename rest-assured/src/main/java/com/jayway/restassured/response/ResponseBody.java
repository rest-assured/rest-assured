/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.response;

public interface ResponseBody extends ResponseBodyExtractionOptions {
    /**
     * Print the response body and return it as string. Mainly useful for debug purposes when writing tests.
     *
     * @return The body as a string.
     */
    String print();

    /**
     * Pretty-print the response body if possible and return it as string. Mainly useful for debug purposes when writing tests.
     * Pretty printing is possible for content-types JSON, XML and HTML.
     *
     * @return The body as a string.
     */
    String prettyPrint();
}
