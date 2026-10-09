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
import io.restassured.internal.path.json.mapping.JsonPathGsonObjectDeserializer;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;

public class GsonMapper implements ObjectMapper {

    private final GsonObjectMapperFactory factory;

    private final JsonPathObjectDeserializer deserializer;

    public GsonMapper(GsonObjectMapperFactory factory) {
        this.factory = factory;
        deserializer = new JsonPathGsonObjectDeserializer(factory);
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        return deserializer.deserialize(context);
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        Gson gson = factory.create(MapperSupport.typeOf(object), context.getCharset());
        // Groovy dispatched on the runtime type, so a JsonElement went to the JsonElement overload
        return object instanceof JsonElement ? gson.toJson((JsonElement) object) : gson.toJson(object);
    }
}
