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

import java.io.InputStream;

import com.jayway.restassured.internal.mapping.ObjectMapper;
import com.jayway.restassured.mapper.ObjectMapperType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;

public interface ResponseBody {
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
	 * Get the body and map it to a Java object using a specific object mapper type. It will use the supplied
	 * mapper regardless of the response content-type.

	 * @return The object
	 */
	<T> T as(Class<T> cls, ObjectMapperType mapperType);

	/**
	 * Get the body and map it to a Java object using a specific object mapper. It will use the supplied
	 * mapper regardless of the response content-type.

	 * @return The object
	 */
	<T> T as(Class<T> cls, ObjectMapper mapper);

    /**
     * Get a JsonPath view of the response body. This will let you use the JsonPath syntax to get values from the response.
     * Example:
     * <p>
     *  Assume that the GET request (to <tt>http://localhost:8080/lotto</tt>) returns JSON as:
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
     * Get a value from the response body using the JsonPath or XmlPath syntax. REST Assured will
     * automatically determine whether to use JsonPath or XmlPath based on the content-type of the response.
     * If no content-type is defined then REST Assured will try to look at the "default parser" if defined (RestAssured.defaultParser).
     *
     * @param path The json- or xml path
     * @param <T> The return type
     * @return The value returned by the path
     * @see #jsonPath()
     * @see #xmlPath()
     */
    <T> T path(String path);
}
