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
package com.jayway.restassured.internal.mapping

import com.jayway.restassured.config.ObjectMapperConfig
import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.mapper.ObjectMapperSerializationContext
import com.jayway.restassured.mapper.ObjectMapperType
import com.jayway.restassured.mapper.factory.GsonObjectMapperFactory
import com.jayway.restassured.mapper.factory.JAXBObjectMapperFactory
import com.jayway.restassured.mapper.factory.JacksonObjectMapperFactory
import org.apache.commons.lang3.Validate

import static com.jayway.restassured.assertion.AssertParameter.notNull
import static com.jayway.restassured.http.ContentType.ANY
import static com.jayway.restassured.mapper.resolver.ObjectMapperResolver.*
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase

class ObjectMapping {

    public static <T> T deserialize(Object object, Class<T> cls, String contentType, String defaultContentType, String charset, ObjectMapperType mapperType,
                                    ObjectMapperConfig objectMapperConfig) {
        Validate.notNull(objectMapperConfig, "Object mapper configuration wasn't found, cannot deserialize.")
        def deserializationCtx = deserializationContext(object, cls, contentType, charset)
        if(objectMapperConfig.hasDefaultObjectMapper()) {
            return objectMapperConfig.defaultObjectMapper().deserialize(deserializationContext(object, cls, contentType, charset)) as T;
        } else if(mapperType != null || objectMapperConfig.hasDefaultObjectMapperType()) {
            ObjectMapperType mapperTypeToUse = mapperType == null ? objectMapperConfig.defaultObjectMapperType() : mapperType;
            return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, objectMapperConfig)
        }
        if(containsIgnoreCase(contentType, "json")) {
            if(isJacksonInClassPath()) {
                return parseWithJackson(deserializationCtx, objectMapperConfig.jacksonObjectMapperFactory()) as T
            } else if(isGsonInClassPath()) {
                return parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory()) as T
            }
            throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson or Gson in the classpath.")
        } else if(containsIgnoreCase(contentType, "xml")) {
            if(isJAXBInClassPath()) {
                return parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory()) as T
            }
            throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
        } else if(defaultContentType != null){
            if(containsIgnoreCase(defaultContentType, "json")) {
                if(isJacksonInClassPath()) {
                    return parseWithJackson(deserializationCtx, objectMapperConfig.jacksonObjectMapperFactory()) as T
                } else if(isGsonInClassPath()) {
                    return parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory()) as T
                }
            } else if(containsIgnoreCase(defaultContentType, "xml")) {
                if(isJAXBInClassPath()) {
                    return parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory()) as T
                }
            }
        }
        throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was not specified in response. Content-Type was '%s'.", contentType))
    }

    private static <T> T deserializeWithObjectMapper(ObjectMapperDeserializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
        if(mapperType == ObjectMapper.JACKSON && isJacksonInClassPath()) {
            return parseWithJackson(ctx, config.jacksonObjectMapperFactory()) as T
        } else if(mapperType == ObjectMapper.GSON && isGsonInClassPath()) {
            return parseWithGson(ctx, config.gsonObjectMapperFactory()) as T
        } else if(mapperType == ObjectMapper.JAXB && isJAXBInClassPath()) {
            return parseWithJaxb(ctx, config.jaxbObjectMapperFactory()) as T
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot map response body with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }

    public static Object serialize(Object object, String contentType, String charset, ObjectMapperType mapperType, ObjectMapperConfig config) {
        notNull(object, "Object to serialize")
        notNull(config, "Object mapper configuration not found, cannot serialize object.")

        def serializationCtx = serializationContext(object, contentType, charset)
        if(config.hasDefaultObjectMapper()) {
            return config.defaultObjectMapper().serialize(serializationContext(object, contentType, charset));
        } else if(mapperType != null) {
            return serializeWithObjectMapper(serializationCtx, mapperType, config)
        }

        if(contentType == null || contentType == ANY.toString()) {
            if(isJacksonInClassPath()) {
                return serializeWithJackson(serializationCtx, config.jacksonObjectMapperFactory())
            } else if(isGsonInClassPath()) {
                return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory())
            } else if(isJAXBInClassPath()) {
                return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
            }
            throw new IllegalArgumentException("Cannot serialize because no JSON or XML serializer found in classpath.")
        } else {
            def ct = contentType.toLowerCase()
            if(containsIgnoreCase(ct, "json")) {
                if(isJacksonInClassPath()) {
                    return serializeWithJackson(serializationCtx, config.jacksonObjectMapperFactory())
                } else if(isGsonInClassPath()) {
                    return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory())
                }
                throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put either Jackson or Gson in the classpath.")
            } else if(containsIgnoreCase(ct, "xml")) {
                if(isJAXBInClassPath()) {
                    return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
                } else {
                    throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
                }
            } else {
                throw new IllegalArgumentException("Cannot serialize because cannot determine how to serialize content-type $contentType")
            }
        }
        return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory())
    }

    private static String serializeWithObjectMapper(ObjectMapperSerializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
        if(mapperType == ObjectMapper.JACKSON && isJacksonInClassPath()) {
            return serializeWithJackson(ctx, config.jacksonObjectMapperFactory())
        } else if(mapperType == ObjectMapper.GSON && isGsonInClassPath()) {
            return serializeWithGson(ctx, config.gsonObjectMapperFactory())
        } else if(mapperType == ObjectMapper.JAXB && isJAXBInClassPath()) {
            return serializeWithJaxb(ctx, config.jaxbObjectMapperFactory())
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot serialize object with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }

    private static Object serializeWithGson(ObjectMapperSerializationContext ctx, GsonObjectMapperFactory factory) {
        new GsonMapper(factory).serialize(ctx)
    }

    private static Object serializeWithJackson(ObjectMapperSerializationContext ctx, JacksonObjectMapperFactory factory) {
        new JacksonMapper(factory).serialize(ctx)
    }

    private static Object serializeWithJaxb(ObjectMapperSerializationContext ctx, JAXBObjectMapperFactory factory) {
        new JaxbMapper(factory).serialize(ctx)
    }

    private static def parseWithJaxb(ObjectMapperDeserializationContext ctx, JAXBObjectMapperFactory factory) {
        new JaxbMapper(factory).deserialize(ctx)
    }

    private static def parseWithGson(ObjectMapperDeserializationContext ctx, GsonObjectMapperFactory factory) {
        new GsonMapper(factory).deserialize(ctx)
    }

    static def parseWithJackson(ObjectMapperDeserializationContext ctx, JacksonObjectMapperFactory factory) {
        new JacksonMapper(factory).deserialize(ctx)
    }

    private static ObjectMapperDeserializationContext deserializationContext(Object object, Class cls, contentType, charset) {
        def ctx = new ObjectMapperDeserializationContextImpl()
        ctx.type = cls
        ctx.charset = charset
        ctx.contentType = contentType
        ctx.object = object
        ctx
    }

    private static ObjectMapperSerializationContext serializationContext(Object object, contentType, charset) {
        def ctx = new ObjectMapperSerializationContextImpl()
        ctx.charset = charset
        ctx.contentType = contentType
        ctx.object = object
        ctx
    }
}
