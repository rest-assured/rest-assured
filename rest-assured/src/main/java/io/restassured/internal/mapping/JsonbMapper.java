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

package io.restassured.internal.mapping;

import io.restassured.common.mapper.factory.JsonbObjectMapperFactory;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import jakarta.json.bind.Jsonb;

import java.io.Reader;
import java.lang.reflect.Type;

public class JsonbMapper implements ObjectMapper {
    private final JsonbObjectMapperFactory factory;

    public JsonbMapper(JsonbObjectMapperFactory factory) {
        AssertParameter.notNull(factory, "JsonbObjectMapperFactory");
        this.factory = factory;
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        Type type = context.getType();
        try (Jsonb mapper = factory.create(type, context.getCharset());
             Reader reader = MapperSupport.platformCharsetReader(context)) {
            return mapper.fromJson(reader, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        Jsonb mapper = factory.create(MapperSupport.typeOf(object), context.getCharset());
        return mapper.toJson(object);
    }
}
