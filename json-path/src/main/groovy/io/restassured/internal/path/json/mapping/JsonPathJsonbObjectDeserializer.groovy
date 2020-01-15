/*
 * Copyright 2020 the original author or authors.
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
import io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer
import javax.json.bind.Jsonb

import java.lang.reflect.Type

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class JsonPathJsonbObjectDeserializer implements JsonPathObjectDeserializer {
  private final JsonbObjectMapperFactory factory

  JsonPathJsonbObjectDeserializer(JsonbObjectMapperFactory factory) {
    notNull(factory, "JsonbObjectMapperFactory")
    this.factory = factory;
  }

  private Jsonb createJsonbObjectMapper(Type cls, String charset) {
    return factory.create(cls, charset)
  }

  @Override
  def deserialize(ObjectDeserializationContext context) {
    def cls = context.getType()
    def mapper = createJsonbObjectMapper(cls, context.getCharset())

    context.getDataToDeserialize().asInputStream().withReader { reader ->
      mapper.fromJson(reader, cls)
    }
  }
}
