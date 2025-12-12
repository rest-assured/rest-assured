package io.restassured.internal.path.json.mapping;

import io.restassured.common.mapper.DataToDeserialize;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.common.mapper.resolver.ObjectMapperResolver;
import io.restassured.internal.common.ObjectDeserializationContextImpl;
import io.restassured.path.json.config.JsonParserType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.mapper.factory.*;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class JsonObjectDeserializer {

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String json, Class<T> cls, JsonPathConfig jsonPathConfig) {
        Validate.notNull(jsonPathConfig, "JsonPath configuration wasn't specified, cannot deserialize.");
        JsonParserType mapperType = jsonPathConfig.defaultParserType();
        DataToDeserialize dataToDeserialize = new DataToDeserialize() {

            @Override
            public String asString() {
                return json;
            }

            @Override
            public byte[] asByteArray() {
                try {
                    return json.getBytes(jsonPathConfig.charset());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public InputStream asInputStream() {
                return new ByteArrayInputStream(asByteArray());
            }
        };
        ObjectDeserializationContext deserializationCtx = new ObjectDeserializationContextImpl(dataToDeserialize, cls, jsonPathConfig.charset());

        if (jsonPathConfig.hasDefaultDeserializer()) {
            return jsonPathConfig.defaultDeserializer().deserialize(deserializationCtx);
        } else if (mapperType != null || jsonPathConfig.hasDefaultParserType()) {
            JsonParserType mapperTypeToUse = mapperType == null ? jsonPathConfig.defaultParserType() : mapperType;
            return deserializeWithObjectMapper(deserializationCtx, mapperTypeToUse, jsonPathConfig);
        }

        if (ObjectMapperResolver.isJackson2InClassPath()) {
            return (T) deserializeWithJackson2(deserializationCtx, jsonPathConfig.jackson2ObjectMapperFactory());
        } else if (ObjectMapperResolver.isJackson1InClassPath()) {
            return (T) deserializeWithJackson1(deserializationCtx, jsonPathConfig.jackson1ObjectMapperFactory());
        } else if (ObjectMapperResolver.isGsonInClassPath()) {
            return (T) deserializeWithGson(deserializationCtx, jsonPathConfig.gsonObjectMapperFactory());
        } else if (ObjectMapperResolver.isJohnzonInClassPath()) {
            return (T) deserializeWithJohnzon(deserializationCtx, jsonPathConfig.johnzonObjectMapperFactory());
        } else if (ObjectMapperResolver.isYassonInClassPath()) {
            return (T) deserializeWithJsonb(deserializationCtx, jsonPathConfig.jsonbObjectMapperFactory());
        }
        throw new IllegalStateException("Cannot deserialize object because no JSON deserializer found in classpath. Please put Jackson (Databind), Gson, Jackson, or Yasson in the classpath.");
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserializeWithObjectMapper(ObjectDeserializationContext ctx, JsonParserType mapperType, JsonPathConfig config) {
        if (mapperType == JsonParserType.JACKSON_3 && ObjectMapperResolver.isJackson3InClassPath()) {
            return (T) deserializeWithJackson3(ctx, config.jackson3ObjectMapperFactory());
        } else if (mapperType == JsonParserType.JACKSON_2 && ObjectMapperResolver.isJackson2InClassPath()) {
            return (T) deserializeWithJackson2(ctx, config.jackson2ObjectMapperFactory());
        } else if (mapperType == JsonParserType.JACKSON_1 && ObjectMapperResolver.isJackson1InClassPath()) {
            return (T) deserializeWithJackson1(ctx, config.jackson1ObjectMapperFactory());
        } else if (mapperType == JsonParserType.GSON && ObjectMapperResolver.isGsonInClassPath()) {
            return (T) deserializeWithGson(ctx, config.gsonObjectMapperFactory());
        } else if (mapperType == JsonParserType.JOHNZON && ObjectMapperResolver.isJohnzonInClassPath()) {
            return (T) deserializeWithJohnzon(ctx, config.johnzonObjectMapperFactory());
        } else if (mapperType == JsonParserType.JSONB && ObjectMapperResolver.isYassonInClassPath()) {
            return (T) deserializeWithJsonb(ctx, config.jsonbObjectMapperFactory());
        } else {
            String lowerCase = mapperType.toString().toLowerCase();
            throw new IllegalArgumentException(String.format("Cannot deserialize object using %s because %s doesn't exist in the classpath.", mapperType, lowerCase));
        }
    }

    private static Object deserializeWithGson(ObjectDeserializationContext ctx, GsonObjectMapperFactory factory) {
        return new JsonPathGsonObjectDeserializer(factory).deserialize(ctx);
    }

    static Object deserializeWithJackson1(ObjectDeserializationContext ctx, Jackson1ObjectMapperFactory factory) {
        return new JsonPathJackson1ObjectDeserializer(factory).deserialize(ctx);
    }

    static Object deserializeWithJackson2(ObjectDeserializationContext ctx, Jackson2ObjectMapperFactory factory) {
        return new JsonPathJackson2ObjectDeserializer(factory).deserialize(ctx);
    }

    static Object deserializeWithJackson3(ObjectDeserializationContext ctx, Jackson3ObjectMapperFactory factory) {
        return new JsonPathJackson3ObjectDeserializer(factory).deserialize(ctx);
    }

    static Object deserializeWithJohnzon(ObjectDeserializationContext ctx, JohnzonObjectMapperFactory factory) {
        return new JsonPathJohnzonObjectDeserializer(factory).deserialize(ctx);
    }

    static Object deserializeWithJsonb(ObjectDeserializationContext ctx, JsonbObjectMapperFactory factory) {
        return new JsonPathJsonbObjectDeserializer(factory).deserialize(ctx);
    }
}
