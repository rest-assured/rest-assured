package com.jayway.restassured.internal.path.json.mapping

import com.jayway.restassured.mapper.ObjectDeserializationContext
import com.jayway.restassured.mapper.factory.Jackson2ObjectMapperFactory
import com.jayway.restassured.path.json.mapping.JsonPathObjectDeserializer

class JsonPathJackson2ObjectDeserializer implements JsonPathObjectDeserializer {

    private final Jackson2ObjectMapperFactory factory;

    JsonPathJackson2ObjectDeserializer(Jackson2ObjectMapperFactory factory) {
        this.factory = factory
    }

    private com.fasterxml.jackson.databind.ObjectMapper createJackson2ObjectMapper(Class cls, String charset) {
        return factory.create(cls, charset)
    }

    @Override
    def <T> T deserialize(ObjectDeserializationContext context) {
        def object = context.getDataToDeserialize().asString()
        def cls = context.getType()
        def mapper = createJackson2ObjectMapper(cls, context.getCharset())
        com.fasterxml.jackson.databind.JavaType javaType = mapper.constructType(cls)
        return mapper.readValue(object, javaType) as T
    }
}
