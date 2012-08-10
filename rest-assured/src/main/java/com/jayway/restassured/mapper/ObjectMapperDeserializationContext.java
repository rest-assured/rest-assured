/*
 * Copyright 2012 the original author or authors.
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

package com.jayway.restassured.mapper;

import com.jayway.restassured.response.ResponseBodyData;

/**
 * Class containing details needed for deserializing a response to a Java class.
 */
public interface ObjectMapperDeserializationContext {

    /**
     * @return The response data that should be deserialized to a Java object.
     */
    ResponseBodyData getResponse();

    /**
     * @return The expected type of the object to deserialize
     */
    Class<?> getType();

    /**
     * @return The content-type of the response.
     */
    String getContentType();

    /**
     * If a charset is specified in the content-type then this method will return that charset otherwise
     * it'll return the default content charset specified in the REST Assured configuration.
     *
     * @return The charset.
     */
    String getCharset();
}
