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


package io.restassured.internal.mapping

import io.restassured.internal.path.json.mapping.JsonPathGsonObjectDeserializer
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer

class GsonMapper implements ObjectMapper {

    private GsonObjectMapperFactory factory;

    private JsonPathObjectDeserializer deserializer

    public GsonMapper(GsonObjectMapperFactory factory) {
        this.factory = factory
        deserializer = new JsonPathGsonObjectDeserializer(factory)
    }

    def Object deserialize(ObjectMapperDeserializationContext context) {
        return deserializer.deserialize(context);
    }

    def Object serialize(ObjectMapperSerializationContext context) {
        def object = context.getObjectToSerialize();
        def gson = factory.create(object.getClass(), context.getCharset())
        return gson.toJson(object)
    }
}
