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

import io.restassured.common.mapper.DataToDeserialize
import io.restassured.common.mapper.ObjectDeserializationContext
import io.restassured.common.mapper.resolver.ObjectMapperResolver
import io.restassured.internal.common.mapper.ObjectDeserializationContextImpl
import io.restassured.path.json.config.JsonParserType
import io.restassured.path.json.config.JsonPathConfig
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory
import io.restassured.path.json.mapper.factory.Jackson1ObjectMapperFactory
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory
import io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory
import org.apache.commons.lang3.Validate

class JsonObjectDeserializer {

    public static <T> T deserialize(String json, Class<T> cls, JsonPathConfig jsonPathConfig) {
        Validate.notNull(jsonPathConfig, "JsonPath configuration wasn't specified, cannot deserialize.")
        def mapperType = jsonPathConfig.defaultParserType()
        def deserializationCtx = new ObjectDeserializationContextImpl()
        deserializationCtx.type = cls
        deserializationCtx.charset = jsonPathConfig.charset()
        deserializationCtx.dataToDeserialize = new DataToDeserialize() {

            @Override
            String asString() {
                return json
            }

            @Override
            byte[] asByteArray() {
                return json.getBytes(jsonPathConfig.charset())
            }

            @Override
            InputStream asInputStream() {
                return new ByteArrayInputStream(asByteArray())
            }
        }

        if (jsonPathConfig.hasDefaultDeserializer()) {
            return jsonPathConfig.defaultDeserializer().deserialize(deserializationCtx) as T;
        } else if (mapperType != null || jsonPathConfig.hasDefaultParserType()) {
            JsonParserType mapperTypeToUse = mapperType == null ? jsonPathConfig.defaultParserType() : mapperType;
            return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, jsonPathConfig)
        }

        if (ObjectMapperResolver.isJackson2InClassPath()) {
            return deserializeWithJackson2(deserializationCtx, jsonPathConfig.jackson2ObjectMapperFactory()) as T
        } else if (ObjectMapperResolver.isJackson1InClassPath()) {
            return deserializeWithJackson1(deserializationCtx, jsonPathConfig.jackson1ObjectMapperFactory()) as T
        } else if (ObjectMapperResolver.isGsonInClassPath()) {
            return deserializeWithGson(deserializationCtx, jsonPathConfig.gsonObjectMapperFactory()) as T
        } else if (ObjectMapperResolver.isJohnzonInClassPath()) {
            return deserializeWithJohnzon(deserializationCtx, jsonPathConfig.johnzonObjectMapperFactory()) as T
        }
        throw new IllegalStateException("Cannot deserialize object because no JSON deserializer found in classpath. Please put either Jackson (Databind) or Gson in the classpath.")
    }

    private static <T> T deserializeWithObjectMapper(ObjectDeserializationContext ctx, JsonParserType mapperType, JsonPathConfig config) {
        if (mapperType == JsonParserType.JACKSON_2 && ObjectMapperResolver.isJackson2InClassPath()) {
            return deserializeWithJackson2(ctx, config.jackson2ObjectMapperFactory()) as T
        } else if (mapperType == JsonParserType.JACKSON_1 && ObjectMapperResolver.isJackson1InClassPath()) {
            return deserializeWithJackson1(ctx, config.jackson1ObjectMapperFactory()) as T
        } else if (mapperType == JsonParserType.GSON && ObjectMapperResolver.isGsonInClassPath()) {
            return deserializeWithGson(ctx, config.gsonObjectMapperFactory()) as T
        } else if (mapperType == JsonParserType.JOHNZON && ObjectMapperResolver.isJohnzonInClassPath()) {
            return deserializeWithJohnzon(ctx, config.johnzonObjectMapperFactory()) as T
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot deserialize object using $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }

    private static def deserializeWithGson(ObjectDeserializationContext ctx, GsonObjectMapperFactory factory) {
        new JsonPathGsonObjectDeserializer(factory).deserialize(ctx)
    }

    static def deserializeWithJackson1(ObjectDeserializationContext ctx, Jackson1ObjectMapperFactory factory) {
        new JsonPathJackson1ObjectDeserializer(factory).deserialize(ctx)
    }

    static def deserializeWithJackson2(ObjectDeserializationContext ctx, Jackson2ObjectMapperFactory factory) {
        new JsonPathJackson2ObjectDeserializer(factory).deserialize(ctx)
    }

	static def deserializeWithJohnzon(ObjectDeserializationContext ctx, JohnzonObjectMapperFactory factory) {
		new JsonPathJohnzonObjectDeserializer(factory).deserialize(ctx)
	}
}
