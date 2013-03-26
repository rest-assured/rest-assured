/*
 * Copyright 2012 the original author or authors.
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
package com.jayway.restassured.internal.path.json.mapping

import com.jayway.restassured.config.ObjectMapperConfig
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.mapper.ObjectMapperSerializationContext
import com.jayway.restassured.mapper.ObjectMapperType
import com.jayway.restassured.path.json.config.JsonPathConfig
import com.jayway.restassured.response.ResponseBodyData
import org.apache.commons.lang3.Validate

import static com.jayway.restassured.http.ContentType.ANY
import static com.jayway.restassured.mapper.resolver.ObjectMapperResolver.*
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase

class JsonObjectMapping {

    public static <T> T deserialize(String json, Class<T> cls, ObjectMapperType mapperType, JsonPathConfig objectMapperConfig) {
        Validate.notNull(objectMapperConfig, "Object mapper configuration wasn't found, cannot deserialize.")
        if (objectMapperConfig.hasDefaultObjectMapper()) {
            return objectMapperConfig.defaultObjectMapper().toObject(cls, json) as T;
        } else if (mapperType != null || objectMapperConfig.hasDefaultObjectMapperType()) {
            ObjectMapperType mapperTypeToUse = mapperType == null ? objectMapperConfig.defaultObjectMapperType() : mapperType;
            return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, objectMapperConfig)
        }
        if (containsIgnoreCase(contentType, "json")) {
            if (com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isJackson2InClassPath()) {
                return parseWithJackson2(deserializationCtx, objectMapperConfig.jackson2ObjectMapperFactory()) as T
            } else if (com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isJackson1InClassPath()) {
                return parseWithJackson1(deserializationCtx, objectMapperConfig.jackson1ObjectMapperFactory()) as T
            } else if (com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isGsonInClassPath()) {
                return new JsonPathGsonObjectMapper(objectMapperConfig.gsonObjectMapperFactory()).toObject(cls, json) as T
            }
            throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson or Gson in the classpath.")
        }
        throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was not specified in response. Content-Type was '%s'.", contentType))
    }

    private static <T> T deserializeWithObjectMapper(ObjectMapperDeserializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
        if (mapperType == ObjectMapperType.JACKSON_2 && com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isJackson2InClassPath()) {
            return parseWithJackson2(ctx, config.jackson2ObjectMapperFactory()) as T
        } else if (mapperType == ObjectMapperType.JACKSON_1 && com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isJackson1InClassPath()) {
            return parseWithJackson1(ctx, config.jackson1ObjectMapperFactory()) as T
        } else if (mapperType == ObjectMapperType.GSON && com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isGsonInClassPath()) {
            return parseWithGson(ctx, config.gsonObjectMapperFactory()) as T
        } else if (mapperType == ObjectMapperType.JAXB && com.jayway.restassured.mapper.resolver.ObjectMapperResolver.isJAXBInClassPath()) {
            return parseWithJaxb(ctx, config.jaxbObjectMapperFactory()) as T
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot map response body with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }
}
