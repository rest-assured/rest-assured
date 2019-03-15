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
 * Class containing details needed for serializing a response to a Java class.
 */
public interface ObjectMapperSerializationContext {

    /**
     * @return The object that should be serialized.
     */
    Object getObjectToSerialize();

    /**
     * @return The object that should be serialized as a specific type. If the object is not of the expected type then an IllegalArgumentException is thrown.
     */
    <T> T getObjectToSerializeAs(Class<T> expectedType);

    /**
     * @return The content-type of the request or <code>null</code> if not defined.
     */
    String getContentType();

    /**
     * If a charset is specified in the content-type then this method will return that charset otherwise
     * it'll return the default content charset specified in the REST Assured configuration.
     *
     * @return The charset or <code>null</code> if not defined.
     */
    String getCharset();
}
