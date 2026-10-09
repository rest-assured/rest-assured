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

import io.restassured.internal.path.json.mapping.JsonPathJohnzonObjectDeserializer;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;
import org.apache.johnzon.mapper.Mapper;

import java.io.StringWriter;

public class JohnzonMapper implements ObjectMapper {
    private final JohnzonObjectMapperFactory factory;

    private final JsonPathObjectDeserializer deserializer;

    public JohnzonMapper(JohnzonObjectMapperFactory factory) {
        this.factory = factory;
        deserializer = new JsonPathJohnzonObjectDeserializer(factory);
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        return deserializer.deserialize(context);
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        Mapper mapper = factory.create(MapperSupport.typeOf(object), context.getCharset());

        StringWriter out = new StringWriter();
        mapper.writeObject(context.getObjectToSerialize(), out);
        return out.toString();
    }
}
