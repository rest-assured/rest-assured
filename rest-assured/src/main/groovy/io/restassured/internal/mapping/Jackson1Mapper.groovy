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


package io.restassured.internal.mapping

import io.restassured.internal.path.json.mapping.JsonPathJackson1ObjectDeserializer
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.path.json.mapper.factory.Jackson1ObjectMapperFactory
import io.restassured.path.json.mapping.JsonPathObjectDeserializer
import org.codehaus.jackson.JsonEncoding
import org.codehaus.jackson.JsonGenerator

class Jackson1Mapper implements ObjectMapper {

    private final Jackson1ObjectMapperFactory factory;

    private JsonPathObjectDeserializer deserializer

    Jackson1Mapper(Jackson1ObjectMapperFactory factory) {
        this.factory = factory
        deserializer = new JsonPathJackson1ObjectDeserializer(factory)
    }

    private org.codehaus.jackson.map.ObjectMapper createJacksonObjectMapper(Class cls, String charset) {
        return factory.create(cls, charset)
    }

    def String serialize(ObjectMapperSerializationContext context) {
        def object = context.getObjectToSerialize()
        JsonEncoding jsonEncoding = getEncoding(context.getCharset())
        def mapper = createJacksonObjectMapper(object.getClass(), context.getCharset())
        def stream = new ByteArrayOutputStream()
        JsonGenerator jsonGenerator = mapper.getJsonFactory().createJsonGenerator(stream, jsonEncoding)
        mapper.writeValue(jsonGenerator, object)
        return stream.toString(jsonEncoding.getJavaName())
    }

    def Object deserialize(ObjectMapperDeserializationContext context) {
        return deserializer.deserialize(context)
    }

    private JsonEncoding getEncoding(String charset) {
        def foundEncoding = JsonEncoding.UTF8
        if (charset != null) {
            for (JsonEncoding encoding : JsonEncoding.values()) {
                if (charset.equals(encoding.getJavaName())) {
                    foundEncoding = encoding
                    break
                }
            }
        }
        return foundEncoding
    }
}