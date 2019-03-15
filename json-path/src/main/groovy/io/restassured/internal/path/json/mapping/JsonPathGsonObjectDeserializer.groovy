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
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class JsonPathGsonObjectDeserializer implements JsonPathObjectDeserializer {
  private final GsonObjectMapperFactory factory

  JsonPathGsonObjectDeserializer(GsonObjectMapperFactory factory) {
    notNull(factory, "GsonObjectMapperFactory")
    this.factory = factory;
  }

  @Override
  def deserialize(ObjectDeserializationContext ctx) {
    factory.create(ctx.type, ctx.charset).fromJson(ctx.dataToDeserialize.asString(), ctx.type)
  }
}
