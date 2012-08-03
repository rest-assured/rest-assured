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

import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.mapper.ObjectMapperSerializationContext
import com.jayway.restassured.mapper.ObjectMapperType

import static com.jayway.restassured.assertion.AssertParameter.notNull
import static com.jayway.restassured.http.ContentType.ANY
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase

class ObjectMapping {
    private static final boolean isJacksonPresent = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator")

    private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder")

    private static final boolean isGsonPresent = existInCP("com.google.gson.Gson")

    private static boolean existInCP(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader())
            return true
        } catch(Throwable e) {
            return false
        }
    }

    public static <T> T deserialize(Object object, Class<T> cls, String contentType, String defaultContentType, String charset, ObjectMapperType mapperType) {
        def deserializationCtx = deserializationContext(object, cls, contentType, charset)
        if(mapperType != null) {
            return deserializeWithObjectMapper(deserializationCtx, mapperType)
        }
        if(containsIgnoreCase(contentType, "json")) {
            if(isJacksonPresent) {
                return parseWithJackson(deserializationCtx) as T
            } else if(isGsonPresent) {
                return parseWithGson(deserializationCtx) as T
            }
            throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson or Gson in the classpath.")
        } else if(containsIgnoreCase(contentType, "xml")) {
            if(isJaxbPresent) {
                return parseWithJaxb(deserializationCtx) as T
            }
            throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
        } else if(defaultContentType != null){
            if(containsIgnoreCase(defaultContentType, "json")) {
                if(isJacksonPresent) {
                    return parseWithJackson(deserializationCtx) as T
                } else if(isGsonPresent) {
                    return parseWithGson(deserializationCtx) as T
                }
            } else if(containsIgnoreCase(defaultContentType, "xml")) {
                if(isJaxbPresent) {
                    return parseWithJaxb(deserializationCtx) as T
                }
            }
        }
        throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was not specified in response. Content-Type was '%s'.", contentType))
    }

    private static <T> T deserializeWithObjectMapper(ObjectMapperDeserializationContext ctx, ObjectMapperType mapperType) {
        if(mapperType == ObjectMapper.JACKSON && isJacksonPresent) {
            return parseWithJackson(ctx) as T
        } else if(mapperType == ObjectMapper.GSON && isGsonPresent) {
            return parseWithGson(ctx) as T
        } else if(mapperType == ObjectMapper.JAXB && isJaxbPresent) {
            return parseWithJaxb(ctx) as T
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot map response body with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }

    public static Object serialize(Object object, String contentType, String charset, ObjectMapperType mapperType) {
        notNull(object, "Object to serialize")
        def serializationCtx = serializationContext(object, contentType, charset)
        if(mapperType != null) {
            return serializeWithObjectMapper(serializationCtx, mapperType)
        }

        if(contentType == null || contentType == ANY.toString()) {
            if(isJacksonPresent) {
                return serializeWithJackson(serializationCtx)
            } else if(isGsonPresent) {
                return serializeWithGson(serializationCtx)
            } else if(isJaxbPresent) {
                return serializeWithJaxb(serializationCtx)
            }
            throw new IllegalArgumentException("Cannot serialize because no JSON or XML serializer found in classpath.")
        } else {
            def ct = contentType.toLowerCase()
            if(containsIgnoreCase(ct, "json")) {
                if(isJacksonPresent) {
                    return serializeWithJackson(serializationCtx)
                } else if(isGsonPresent) {
                    return serializeWithGson(serializationCtx)
                }
                throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put either Jackson or Gson in the classpath.")
            } else if(containsIgnoreCase(ct, "xml")) {
                if(isJaxbPresent) {
                    return serializeWithJaxb(serializationCtx)
                } else {
                    throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB compliant object mapper in classpath.")
                }
            } else {
                throw new IllegalArgumentException("Cannot serialize because cannot determine how to serialize content-type $contentType")
            }
        }
        return serializeWithJaxb(serializationCtx)
    }

    private static String serializeWithObjectMapper(ObjectMapperSerializationContext ctx, ObjectMapperType mapperType) {
        if(mapperType == ObjectMapper.JACKSON && isJacksonPresent) {
            return serializeWithJackson(ctx)
        } else if(mapperType == ObjectMapper.GSON && isGsonPresent) {
            return serializeWithGson(ctx)
        } else if(mapperType == ObjectMapper.JAXB && isJaxbPresent) {
            return serializeWithJaxb(ctx)
        } else {
            def lowerCase = mapperType.toString().toLowerCase()
            throw new IllegalArgumentException("Cannot serialize object with mapper $mapperType because $lowerCase doesn't exist in the classpath.")
        }
    }

    private static Object serializeWithGson(ObjectMapperSerializationContext ctx) {
        new GsonMapper().serialize(ctx)
    }

    private static Object serializeWithJackson(ObjectMapperSerializationContext ctx) {
        new JacksonMapper().serialize(ctx)
    }

    private static Object serializeWithJaxb(ObjectMapperSerializationContext ctx) {
        new JaxbMapper().serialize(ctx)
    }

    private static def parseWithJaxb(ObjectMapperDeserializationContext ctx) {
        new JaxbMapper().deserialize(ctx)
    }

    private static def parseWithGson(ObjectMapperDeserializationContext ctx) {
        new GsonMapper().deserialize(ctx)
    }

    static def parseWithJackson(ObjectMapperDeserializationContext ctx) {
        new JacksonMapper().deserialize(ctx)
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
