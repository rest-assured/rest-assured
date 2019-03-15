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


package io.restassured.internal.path.json.mapping

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.common.mapper.ObjectDeserializationContext
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer

import java.lang.reflect.Type

class JsonPathJackson2ObjectDeserializer implements JsonPathObjectDeserializer {

  private final Jackson2ObjectMapperFactory factory;

  JsonPathJackson2ObjectDeserializer(Jackson2ObjectMapperFactory factory) {
    this.factory = factory
  }

  private ObjectMapper createJackson2ObjectMapper(Type cls, String charset) {
    return factory.create(cls, charset)
  }

  @Override
  deserialize(ObjectDeserializationContext context) {
    def object = context.getDataToDeserialize().asString()
    def cls = context.getType()
    def mapper = createJackson2ObjectMapper(cls, context.getCharset())
    JavaType javaType = mapper.constructType(cls)
    mapper.readValue(object, javaType)
  }
}