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

import io.restassured.common.mapper.DataToDeserialize;
import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.http.ContentTypeExtractor;
import io.restassured.internal.util.GroovyStringConversion;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory;
import io.restassured.response.ResponseBodyData;
import org.apache.commons.lang3.Validate;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Locale;

import static io.restassured.common.mapper.resolver.ObjectMapperResolver.*;
import static io.restassured.http.ContentType.ANY;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class ObjectMapping {

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(ResponseBodyData response, Type cls, String contentType, String defaultContentType, String charset, ObjectMapperType mapperType,
                                    ObjectMapperConfig objectMapperConfig) {
        Validate.notNull(objectMapperConfig, "String mapper configuration wasn't found, cannot deserialize.");
        ObjectMapperDeserializationContext deserializationCtx = deserializationContext(response, cls, contentType, charset);
        if (objectMapperConfig.hasDefaultObjectMapper()) {
            return (T) objectMapperConfig.defaultObjectMapper().deserialize(deserializationContext(response, cls, contentType, charset));
        } else if (mapperType != null || objectMapperConfig.hasDefaultObjectMapperType()) {
            ObjectMapperType mapperTypeToUse = mapperType == null ? objectMapperConfig.defaultObjectMapperType() : mapperType;
            return (T) deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, objectMapperConfig);
        }
        if (containsIgnoreCase(contentType, "json")) {
            if (isJackson3InClassPath()) {
                return (T) parseWithJackson3(deserializationCtx, objectMapperConfig.jackson3ObjectMapperFactory());
            } else if (isJackson2InClassPath()) {
                return (T) parseWithJackson2(deserializationCtx, objectMapperConfig.jackson2ObjectMapperFactory());
            } else if (isJackson1InClassPath()) {
                return (T) parseWithJackson1(deserializationCtx, objectMapperConfig.jackson1ObjectMapperFactory());
            } else if (isGsonInClassPath()) {
                return (T) parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory());
            } else if (isJohnzonInClassPath()) {
                return (T) parseWithJohnzon(deserializationCtx, objectMapperConfig.johnzonObjectMapperFactory());
            } else if (isYassonInClassPath()) {
                return (T) parseWithJsonb(deserializationCtx, objectMapperConfig.jsonbObjectMapperFactory());
            }
            throw new IllegalStateException("Cannot parse object because no JSON deserializer found in classpath. Please put either Jackson (Databind) or Gson in the classpath.");
        } else if (containsIgnoreCase(contentType, "xml")) {
            if (isJakartaEEInClassPath()) {
                return (T) parseWithJakartaEE(deserializationCtx, objectMapperConfig.jakartaEEObjectMapperFactory());
            } else if (isJAXBInClassPath()) {
                return (T) parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory());
            }
            throw new IllegalStateException("Cannot parse object because no XML deserializer found in classpath. Please put a JAXB compliant object mapper in classpath.");
        } else if (defaultContentType != null) {
            if (containsIgnoreCase(defaultContentType, "json")) {
                if (isJackson3InClassPath()) {
                    return (T) parseWithJackson3(deserializationCtx, objectMapperConfig.jackson3ObjectMapperFactory());
                } else if (isJackson2InClassPath()) {
                    return (T) parseWithJackson2(deserializationCtx, objectMapperConfig.jackson2ObjectMapperFactory());
                } else if (isJackson1InClassPath()) {
                    return (T) parseWithJackson1(deserializationCtx, objectMapperConfig.jackson1ObjectMapperFactory());
                } else if (isGsonInClassPath()) {
                    return (T) parseWithGson(deserializationCtx, objectMapperConfig.gsonObjectMapperFactory());
                } else if (isJohnzonInClassPath()) {
                    return (T) parseWithJohnzon(deserializationCtx, objectMapperConfig.johnzonObjectMapperFactory());
                } else if (isYassonInClassPath()) {
                    return (T) parseWithJsonb(deserializationCtx, objectMapperConfig.jsonbObjectMapperFactory());
                }
            } else if (containsIgnoreCase(defaultContentType, "xml")) {
                if (isJakartaEEInClassPath()) {
                    return (T) parseWithJakartaEE(deserializationCtx, objectMapperConfig.jakartaEEObjectMapperFactory());
                } else if (isJAXBInClassPath()) {
                    return (T) parseWithJaxb(deserializationCtx, objectMapperConfig.jaxbObjectMapperFactory());
                }
            }
        }
        throw new IllegalStateException(String.format("Cannot parse object because no supported Content-Type was specified in response. Content-Type was '%s'.", contentType));
    }

    private static Object deserializeWithObjectMapper(ObjectMapperDeserializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
        if (mapperType == ObjectMapperType.JACKSON_3 && isJackson3InClassPath()) {
            return parseWithJackson3(ctx, config.jackson3ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JACKSON_2 && isJackson2InClassPath()) {
            return parseWithJackson2(ctx, config.jackson2ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JACKSON_1 && isJackson1InClassPath()) {
            return parseWithJackson1(ctx, config.jackson1ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.GSON && isGsonInClassPath()) {
            return parseWithGson(ctx, config.gsonObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JAKARTA_EE && isJakartaEEInClassPath()) {
            return parseWithJakartaEE(ctx, config.jakartaEEObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JAXB && isJAXBInClassPath()) {
            return parseWithJaxb(ctx, config.jaxbObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JOHNZON && isJohnzonInClassPath()) {
            return parseWithJohnzon(ctx, config.johnzonObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JSONB && isYassonInClassPath()) {
            return parseWithJsonb(ctx, config.jsonbObjectMapperFactory());
        } else {
            String lowerCase = mapperType.toString().toLowerCase(Locale.ROOT);
            throw new IllegalArgumentException("Cannot map response body with mapper " + mapperType + " because " + lowerCase + " doesn't exist in the classpath.");
        }
    }

    /**
     * Serializes the object. A default object mapper may return any type: like the Groovy implementation did, it is
     * converted with {@link GroovyStringConversion#castToString(Object)} (so a {@code byte[]} becomes e.g. {@code "[65, 66]"}).
     */
    public static String serialize(Object object, String contentType, String charset, ObjectMapperType mapperType, ObjectMapperConfig config,
                                   EncoderConfig encoderConfig) {
        notNull(object, "String to serialize");
        notNull(config, "Object mapper configuration not found, cannot serialize object.");
        notNull(encoderConfig, "Encoder configuration not found, cannot serialize object.");

        ObjectMapperSerializationContext serializationCtx = serializationContext(object, contentType, charset);
        if (config.hasDefaultObjectMapper()) {
            return GroovyStringConversion.castToString(config.defaultObjectMapper().serialize(serializationContext(object, contentType, charset)));
        } else if (mapperType != null || config.hasDefaultObjectMapperType()) {
            ObjectMapperType mapperTypeToUse = mapperType != null ? mapperType : config.defaultObjectMapperType();
            return serializeWithObjectMapper(serializationCtx, mapperTypeToUse, config);
        }

        if (contentType == null || contentType.equals(ANY.toString())) {
            if (isJackson3InClassPath()) {
                return serializeWithJackson3(serializationCtx, config.jackson3ObjectMapperFactory());
            } else if (isJackson2InClassPath()) {
                return serializeWithJackson2(serializationCtx, config.jackson2ObjectMapperFactory());
            } else if (isJackson1InClassPath()) {
                return serializeWithJackson1(serializationCtx, config.jackson1ObjectMapperFactory());
            } else if (isGsonInClassPath()) {
                return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory());
            } else if (isJakartaEEInClassPath()) {
                return serializeWithJakartaEE(serializationCtx, config.jakartaEEObjectMapperFactory());
            } else if (isJAXBInClassPath()) {
                return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory());
            } else if (isJohnzonInClassPath()) {
                return serializeWithJohnzon(serializationCtx, config.johnzonObjectMapperFactory());
            } else if (isYassonInClassPath()) {
                return serializeWithJsonb(serializationCtx, config.jsonbObjectMapperFactory());
            }
            throw new IllegalArgumentException("Cannot serialize because no JSON or XML serializer found in classpath.");
        } else {
            String ct = contentType.toLowerCase(Locale.ROOT);
            ContentType encoderType = encoderConfig.contentEncoders().get(ContentTypeExtractor.getContentTypeWithoutCharset(ct));
            if (containsIgnoreCase(ct, "json") || encoderType == ContentType.JSON) {
                if (isJackson3InClassPath()) {
                    return serializeWithJackson3(serializationCtx, config.jackson3ObjectMapperFactory());
                } else if (isJackson2InClassPath()) {
                    return serializeWithJackson2(serializationCtx, config.jackson2ObjectMapperFactory());
                } else if (isJackson1InClassPath()) {
                    return serializeWithJackson1(serializationCtx, config.jackson1ObjectMapperFactory());
                } else if (isGsonInClassPath()) {
                    return serializeWithGson(serializationCtx, config.gsonObjectMapperFactory());
                } else if (isJohnzonInClassPath()) {
                    return serializeWithJohnzon(serializationCtx, config.johnzonObjectMapperFactory());
                } else if (isYassonInClassPath()) {
                    return serializeWithJsonb(serializationCtx, config.jsonbObjectMapperFactory());
                }
                throw new IllegalStateException("Cannot serialize object because no JSON serializer found in classpath. Please put Jackson (Databind), Gson, Johnzon, or Yasson in the classpath.");
            } else if (containsIgnoreCase(ct, "xml") || encoderType == ContentType.XML) {
                if (isJakartaEEInClassPath()) {
                    return serializeWithJakartaEE(serializationCtx, config.jakartaEEObjectMapperFactory());
                } else if (isJAXBInClassPath()) {
                    return serializeWithJaxb(serializationCtx, config.jaxbObjectMapperFactory());
                } else {
                    throw new IllegalStateException("Cannot serialize object because no XML serializer found in classpath. Please put a JAXB or JakartaEE compliant object mapper in classpath.");
                }
            } else {
                String errorMessage = "Cannot serialize because cannot determine how to serialize content-type " + contentType;
                if (encoderType != null) {
                    errorMessage = errorMessage + " as " + encoderType.name() + " (no serializer supports this format)";
                }
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    private static String serializeWithObjectMapper(ObjectMapperSerializationContext ctx, ObjectMapperType mapperType, ObjectMapperConfig config) {
        if (mapperType == ObjectMapperType.JACKSON_3 && isJackson3InClassPath()) {
            return serializeWithJackson3(ctx, config.jackson3ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JACKSON_2 && isJackson2InClassPath()) {
            return serializeWithJackson2(ctx, config.jackson2ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JACKSON_1 && isJackson1InClassPath()) {
            return serializeWithJackson1(ctx, config.jackson1ObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.GSON && isGsonInClassPath()) {
            return serializeWithGson(ctx, config.gsonObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JAKARTA_EE && isJakartaEEInClassPath()) {
            return serializeWithJakartaEE(ctx, config.jakartaEEObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JAXB && isJAXBInClassPath()) {
            return serializeWithJaxb(ctx, config.jaxbObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JOHNZON && isJohnzonInClassPath()) {
            return serializeWithJohnzon(ctx, config.johnzonObjectMapperFactory());
        } else if (mapperType == ObjectMapperType.JSONB && isYassonInClassPath()) {
            return serializeWithJsonb(ctx, config.jsonbObjectMapperFactory());
        } else {
            String lowerCase = mapperType.toString().toLowerCase(Locale.ROOT);
            throw new IllegalArgumentException("Cannot serialize object with mapper " + mapperType + " because " + lowerCase + " doesn't exist in the classpath.");
        }
    }

    private static String serializeWithGson(ObjectMapperSerializationContext ctx, GsonObjectMapperFactory factory) {
        return (String) new GsonMapper(factory).serialize(ctx);
    }

    private static String serializeWithJackson1(ObjectMapperSerializationContext ctx, Jackson1ObjectMapperFactory factory) {
        return new Jackson1Mapper(factory).serialize(ctx);
    }

    private static String serializeWithJackson2(ObjectMapperSerializationContext ctx, Jackson2ObjectMapperFactory factory) {
        return new Jackson2Mapper(factory).serialize(ctx);
    }

    private static String serializeWithJackson3(ObjectMapperSerializationContext ctx, Jackson3ObjectMapperFactory factory) {
        return new Jackson3Mapper(factory).serialize(ctx);
    }

    private static String serializeWithJaxb(ObjectMapperSerializationContext ctx, JAXBObjectMapperFactory factory) {
        return (String) new JaxbMapper(factory).serialize(ctx);
    }

    private static String serializeWithJakartaEE(ObjectMapperSerializationContext ctx, JakartaEEObjectMapperFactory factory) {
        return (String) new JakartaEEMapper(factory).serialize(ctx);
    }

    private static String serializeWithJohnzon(ObjectMapperSerializationContext ctx, JohnzonObjectMapperFactory factory) {
        return (String) new JohnzonMapper(factory).serialize(ctx);
    }

    private static String serializeWithJsonb(ObjectMapperSerializationContext ctx, JsonbObjectMapperFactory factory) {
        return (String) new JsonbMapper(factory).serialize(ctx);
    }

    private static Object parseWithJaxb(ObjectMapperDeserializationContext ctx, JAXBObjectMapperFactory factory) {
        return new JaxbMapper(factory).deserialize(ctx);
    }

    private static Object parseWithJakartaEE(ObjectMapperDeserializationContext ctx, JakartaEEObjectMapperFactory factory) {
        return new JakartaEEMapper(factory).deserialize(ctx);
    }

    private static Object parseWithGson(ObjectMapperDeserializationContext ctx, GsonObjectMapperFactory factory) {
        return new GsonMapper(factory).deserialize(ctx);
    }

    public static Object parseWithJackson1(ObjectMapperDeserializationContext ctx, Jackson1ObjectMapperFactory factory) {
        return new Jackson1Mapper(factory).deserialize(ctx);
    }

    public static Object parseWithJackson2(ObjectMapperDeserializationContext ctx, Jackson2ObjectMapperFactory factory) {
        return new Jackson2Mapper(factory).deserialize(ctx);
    }

    public static Object parseWithJackson3(ObjectMapperDeserializationContext ctx, Jackson3ObjectMapperFactory factory) {
        return new Jackson3Mapper(factory).deserialize(ctx);
    }

    public static Object parseWithJohnzon(ObjectMapperDeserializationContext ctx, JohnzonObjectMapperFactory factory) {
        return new JohnzonMapper(factory).deserialize(ctx);
    }

    public static Object parseWithJsonb(ObjectMapperDeserializationContext ctx, JsonbObjectMapperFactory factory) {
        return new JsonbMapper(factory).deserialize(ctx);
    }

    private static ObjectMapperDeserializationContext deserializationContext(ResponseBodyData responseData, Type cls, String contentType, String charset) {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();
        ctx.setType(cls);
        ctx.setCharset(charset);
        ctx.setContentType(contentType);
        ctx.setDataToDeserialize(new DataToDeserialize() {
            @Override
            public String asString() {
                return responseData.asString();
            }

            @Override
            public byte[] asByteArray() {
                return responseData.asByteArray();
            }

            @Override
            public InputStream asInputStream() {
                return responseData.asInputStream();
            }
        });
        return ctx;
    }

    private static ObjectMapperSerializationContext serializationContext(Object object, String contentType, String charset) {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setCharset(charset);
        ctx.setContentType(contentType);
        ctx.setObject(object);
        return ctx;
    }
}
