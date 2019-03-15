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

import io.restassured.common.mapper.ObjectDeserializationContext
import io.restassured.path.json.mapper.factory.Jackson1ObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer
import org.codehaus.jackson.type.JavaType

import java.lang.reflect.Type

class JsonPathJackson1ObjectDeserializer implements JsonPathObjectDeserializer {

  private final Jackson1ObjectMapperFactory factory;

  JsonPathJackson1ObjectDeserializer(Jackson1ObjectMapperFactory factory) {
    this.factory = factory
  }

  private org.codehaus.jackson.map.ObjectMapper createJacksonObjectMapper(Type cls, String charset) {
    return factory.create(cls, charset)
  }

  @Override
  def deserialize(ObjectDeserializationContext ctx) {
    def object = ctx.getDataToDeserialize().asString()
    def cls = ctx.getType()
    def mapper = createJacksonObjectMapper(cls, ctx.getCharset())
    JavaType javaType = mapper.constructType(cls)
    mapper.readValue(object, javaType)
  }
}
