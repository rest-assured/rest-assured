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

package io.restassured.mapper;

/**
 * An object mapper is used to serialize and deserialize a Java object to and from a String, byte[] or InputStream. REST Assured provides
 * mappers for XML and JSON out of the box (see {@link ObjectMapperType}) but you can implement this interface
 * to roll your own mapper implementations for custom formats.
 */
public interface ObjectMapper {
    /**
     * Deserialize a response to a Java object
     *
     * @param context The details needed to convert the response to a Java object
     * @return A Java object
     */
    Object deserialize(ObjectMapperDeserializationContext context);


    /**
     * Serialize a request to an object (String, InputStream or byte[]) that'll be used as the request body.
     *
     * @param context The details needed to convert the request from a Java object to a object
     * @return A serialized representation of the Java object (String, InputStream or byte[]).
     */
    Object serialize(ObjectMapperSerializationContext context);
}
