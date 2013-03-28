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

package com.jayway.restassured.internal.path.json.mapping

import com.jayway.restassured.mapper.ObjectDeserializationContext
import com.jayway.restassured.mapper.factory.Jackson2ObjectMapperFactory
import com.jayway.restassured.path.json.mapping.JsonPathObjectDeserializer

class JsonPathJackson2ObjectDeserializer implements JsonPathObjectDeserializer {

    private final Jackson2ObjectMapperFactory factory;

    JsonPathJackson2ObjectDeserializer(Jackson2ObjectMapperFactory factory) {
        this.factory = factory
    }

    private com.fasterxml.jackson.databind.ObjectMapper createJackson2ObjectMapper(Class cls, String charset) {
        return factory.create(cls, charset)
    }

    @Override
    def <T> T deserialize(ObjectDeserializationContext context) {
        def object = context.getDataToDeserialize().asString()
        def cls = context.getType()
        def mapper = createJackson2ObjectMapper(cls, context.getCharset())
        com.fasterxml.jackson.databind.JavaType javaType = mapper.constructType(cls)
        return mapper.readValue(object, javaType) as T
    }
}
