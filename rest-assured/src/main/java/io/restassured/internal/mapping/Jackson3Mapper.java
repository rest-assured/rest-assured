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

import io.restassured.common.mapper.factory.Jackson3ObjectMapperFactory;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * Support for Jackson 3.0
 */
public class Jackson3Mapper implements ObjectMapper {

    private final Jackson3ObjectMapperFactory factory;

    public Jackson3Mapper(Jackson3ObjectMapperFactory factory) {
        this.factory = factory;
    }

    private tools.jackson.databind.ObjectMapper createJackson3JsonMapper(Type cls, String charset) {
        return factory.create(cls, charset);
    }

    public String serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        JsonEncoding jsonEncoding = getEncoding(context.getCharset());
        tools.jackson.databind.ObjectMapper mapper = createJackson3JsonMapper(MapperSupport.typeOf(object), context.getCharset());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = mapper.createGenerator(stream, jsonEncoding);
        mapper.writeValue(jsonGenerator, object);
        try {
            return stream.toString(jsonEncoding.getJavaName());
        } catch (UnsupportedEncodingException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        String object = context.getDataToDeserialize().asString();
        Type type = context.getType();
        tools.jackson.databind.ObjectMapper mapper = createJackson3JsonMapper(type, context.getCharset());
        return mapper.readValue(object, mapper.constructType(type));
    }

    private static JsonEncoding getEncoding(String charset) {
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
