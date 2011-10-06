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

package com.jayway.restassured.response;

import com.jayway.restassured.mapper.ObjectMapper;

import java.io.InputStream;

public interface ResponseBody {
    /**
     * Print the response body and return it as string. Mainly useful for debug purposes when writing tests.
     *
     * @return The body as a string.
     */
    String print();

    /**
     * Get the body as a string.
     *
     * @return The body as a string.
     */
    String asString();

    /**
     * Get the body as a byte array.
     *
     * @return The body as a array.
     */
    byte[] asByteArray();

    /**
     * Get the body as an input stream.
     *
     * @return The body as an input stream.
     */
    InputStream asInputStream();

    /**
     * Get the body and map it to a Java object. For JSON responses this requires that you have either
     * <ol>
     * <li>Jackson, or</li>
     * <li>Gson</li>
     * </ol>
     * in the classpath or for XML responses it requires JAXB to be in the classpath.
     * <br/>
     * It also requires that the response content-type is either JSON or XML or that a default parser has been been set.
     * You can also force a specific object mapper using {@link #as(Class, com.jayway.restassured.mapper.ObjectMapper)}.
     *
     * @return The object
     */
    <T> T as(Class<T> cls);

    /**
     * Get the body and map it to a Java object using a specific object mapper. It will use the supplied
     * mapper regardless of the response content-type.

     * @return The object
     */
    <T> T as(Class<T> cls, ObjectMapper mapper);
}
