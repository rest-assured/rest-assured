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

package io.restassured.internal.mapping;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.restassured.common.mapper.factory.GsonObjectMapperFactory;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;

import java.lang.reflect.Type;

public class GsonMapper implements ObjectMapper {

    private final GsonObjectMapperFactory factory;

    public GsonMapper(GsonObjectMapperFactory factory) {
        AssertParameter.notNull(factory, "GsonObjectMapperFactory");
        this.factory = factory;
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        Type type = context.getType();
        return factory.create(type, context.getCharset()).fromJson(context.getDataToDeserialize().asString(), type);
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        Gson gson = factory.create(MapperSupport.typeOf(object), context.getCharset());
        // Groovy dispatched on the runtime type, so a JsonElement went to the JsonElement overload
        return object instanceof JsonElement ? gson.toJson((JsonElement) object) : gson.toJson(object);
    }
}
