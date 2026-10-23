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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Support for Jackson 2.0 (https://github.com/FasterXML/jackson-core)
 */
public class Jackson2Mapper implements ObjectMapper {

    private final Jackson2ObjectMapperFactory factory;

    public Jackson2Mapper(Jackson2ObjectMapperFactory factory) {
        this.factory = factory;
    }

    private com.fasterxml.jackson.databind.ObjectMapper createJackson2ObjectMapper(Type cls, String charset) {
        return factory.create(cls, charset);
    }

    @SuppressWarnings("deprecation")
    public String serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        JsonEncoding jsonEncoding = getEncoding(context.getCharset());
        com.fasterxml.jackson.databind.ObjectMapper mapper = createJackson2ObjectMapper(MapperSupport.typeOf(object), context.getCharset());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(stream, jsonEncoding);
            mapper.writeValue(jsonGenerator, object);
            return stream.toString(jsonEncoding.getJavaName());
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        String object = context.getDataToDeserialize().asString();
        Type type = context.getType();
        com.fasterxml.jackson.databind.ObjectMapper mapper = createJackson2ObjectMapper(type, context.getCharset());
        try {
            return mapper.readValue(object, mapper.constructType(type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonEncoding getEncoding(String charset) {
        JsonEncoding foundEncoding = JsonEncoding.UTF8;
        if (charset != null) {
            for (JsonEncoding encoding : JsonEncoding.values()) {
                if (charset.equals(encoding.getJavaName())) {
                    foundEncoding = encoding;
                    break;
                }
            }
        }
        return foundEncoding;
    }
}
