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

import io.restassured.common.mapper.TypeRef;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.config.XmlPathConfig;

import java.lang.reflect.Type;

public interface ResponseBodyExtractionOptions extends ResponseBodyData {
    /**
     * Get the body and map it to a Java object. For JSON responses this requires that you have either
     * <ol>
     * <li>Jackson, or</li>
     * <li>Gson</li>
     * </ol>
     * in the classpath or for XML responses it requires JAXB to be in the classpath.
     * <br/>
     * It also requires that the response content-type is either JSON or XML or that a default parser has been been set.
     * You can also force a specific object mapper using {@link #as(Class, ObjectMapper)}.
     *
     * @return The object
     */
    <T> T as(Class<T> cls);

    /**
     * Get the body and map it to a Java object using a specific object mapper type. It will use the supplied
     * mapper regardless of the response content-type.
     *
     * @return The object
     */
    <T> T as(Class<T> cls, ObjectMapperType mapperType);

    /**
     * Get the body and map it to a Java object using a specific object mapper. It will use the supplied
     * mapper regardless of the response content-type.
     *
     * @return The object
     */
    <T> T as(Class<T> cls, ObjectMapper mapper);


    /**
     * Get the body as a container with a generic type. Note that this may not work for JAXB (XML) or HTML.
     *
     * @return The object
     */
    <T> T as(TypeRef<T> typeRef);

    /**
     * Get the body and map it to a Java type with specified type. For JSON responses this requires that you have either
     * <ol>
     * <li>Jackson, or</li>
     * <li>Gson</li>
     * </ol>
     * in the classpath or for XML responses it requires JAXB to be in the classpath.
     * <br/>
     * It also requires that the response content-type is either JSON or XML or that a default parser has been been set.
     * You can also force a specific object mapper using {@link #as(Type, ObjectMapper)}.
     *
     * @return The object
     */
    <T> T as(Type cls);

    /**
     * Get the body and map it to a Java type with specified type using a specific object mapper type. It will use the supplied
     * mapper regardless of the response content-type.
     *
     * @return The object
     */
    <T> T as(Type cls, ObjectMapperType mapperType);

    /**
     * Get the body and map it to a Java type using a specific object mapper. It will use the supplied
     * mapper regardless of the response content-type.
     *
     * @return The object
     */
    <T> T as(Type cls, ObjectMapper mapper);

    /**
     * Get a JsonPath view of the response body. This will let you use the JsonPath syntax to get values from the response.
     * Example:
     * <p>
     * Assume that the GET request (to <tt>http://localhost:8080/lotto</tt>) returns JSON as:
     * <pre>
     * {
     * "lotto":{
     *   "lottoId":5,
     *   "winning-numbers":[2,45,34,23,7,5,3],
     *   "winners":[{
     *     "winnerId":23,
     *     "numbers":[2,45,34,23,3,5]
     *   },{
     *     "winnerId":54,
     *     "numbers":[52,3,12,11,18,22]
     *   }]
     *  }
     * }
     * </pre>
     * </p>
     * You can the make the request and get the winner id's by using JsonPath:
     * <pre>
     * List<Integer> winnerIds = get("/lotto").jsonPath().getList("lotto.winnders.winnerId");
     * </pre>
     */
    JsonPath jsonPath();

    /**
     * Get a JsonPath view of the response body using the specified configuration.
     *
     * @param config The configuration to use
     * @see #jsonPath()
     */
    JsonPath jsonPath(JsonPathConfig config);

    /**
     * Get an XmlPath view of the response body. This will let you use the XmlPath syntax to get values from the response.
     * Example:
     * <p>
     * Imagine that a POST request to <tt>http://localhost:8080/greetXML<tt>  returns:
     * <pre>
     * &lt;greeting&gt;
     *     &lt;firstName&gt;John&lt;/firstName&gt;
     *     &lt;lastName&gt;Doe&lt;/lastName&gt;
     *   &lt;/greeting&gt;
     * </pre>
     * </pre>
     * </p>
     * You can the make the request and get the winner id's by using JsonPath:
     * <pre>
     * String firstName = get("/greetXML").xmlPath().getString("greeting.firstName");
     * </pre>
     */
    XmlPath xmlPath();

    /**
     * Get an XmlPath view of the response body with a given configuration.
     *
     * @param config The configuration of the XmlPath
     * @see #xmlPath()
     */
    XmlPath xmlPath(XmlPathConfig config);

    /**
     * Get an XmlPath view of the response body but also pass in a {@link XmlPath.CompatibilityMode}.
     * This is mainly useful if you want to parse HTML documents.
     *
     * @param compatibilityMode The compatibility mode to use
     * @see #htmlPath()
     * @see #xmlPath()
     */
    XmlPath xmlPath(XmlPath.CompatibilityMode compatibilityMode);

    /**
     * Get an XmlPath view of the response body that uses {@link XmlPath.CompatibilityMode} <code>HTML</code>.
     * This is mainly useful when parsing HTML documents.
     * <p>
     * Note that this is the same as calling {@link #xmlPath(XmlPath.CompatibilityMode)} with <code>CompatibilityMode</code> <code>HTML</code>.
     * </p>
     */
    XmlPath htmlPath();

    /**
     * Get a value from the response body using the JsonPath or XmlPath syntax. REST Assured will
     * automatically determine whether to use JsonPath or XmlPath based on the content-type of the response.
     * If no content-type is defined then REST Assured will try to look at the "default parser" if defined (RestAssured.defaultParser).
     * <p>
     * Note that you can also also supply arguments, for example:
     * <pre>
     * String z = get("/x").path("x.y.%s", "z");
     * </pre>
     *
     * The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * </p>
     *
     * @param path      The json- or xml path
     * @param <T>       The return type
     * @param arguments Options arguments
     * @return The value returned by the path
     * @see #jsonPath()
     * @see #xmlPath()
     */
    <T> T path(String path, String... arguments);
}
