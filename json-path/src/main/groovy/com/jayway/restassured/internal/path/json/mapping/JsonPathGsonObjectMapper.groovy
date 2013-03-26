package com.jayway.restassured.internal.path.json.mapping

import com.jayway.restassured.mapper.factory.GsonObjectMapperFactory
import com.jayway.restassured.path.json.mapping.JsonPathObjectMapper

import java.nio.charset.Charset

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull

class JsonPathGsonObjectMapper implements JsonPathObjectMapper {
    private final GsonObjectMapperFactory factory

    JsonPathGsonObjectMapper(GsonObjectMapperFactory factory) {
        notNull(factory, "GsonObjectMapperFactory")
        this.factory = factory;
    }

    @Override
    def <T> T toObject(Class<T> objectType, String json) {
        return factory.create(objectType, Charset.defaultCharset().toString()).fromJson(json, objectType);
    }
}
