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

public interface ResponseBody<T extends ResponseBody<T>> extends ResponseBodyExtractionOptions {
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

    /**
     * Peeks into the JSON that JsonPath will parse by printing it to the console. You can
     * continue working with JsonPath afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettyPrint()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has is downloaded and transformed into another data structure and is rendered from this data structure.
     * </p>
     *
     * @return The same response instance
     */
    T peek();

    /**
     * Peeks into the response body by printing it to the console in a prettified manner. You can
     * continue working with response path afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettyPrint()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has is downloaded and transformed into another data structure and is rendered from this data structure.
     * </p>
     *
     * @return The same response instance
     */
    T prettyPeek();
}
