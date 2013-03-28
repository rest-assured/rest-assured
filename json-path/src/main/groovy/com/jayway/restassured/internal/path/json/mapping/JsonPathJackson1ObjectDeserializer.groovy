package com.jayway.restassured.internal.path.json.mapping

import com.jayway.restassured.mapper.ObjectDeserializationContext
import com.jayway.restassured.mapper.factory.Jackson1ObjectMapperFactory
import com.jayway.restassured.path.json.mapping.JsonPathObjectDeserializer
import org.codehaus.jackson.type.JavaType

class JsonPathJackson1ObjectDeserializer implements JsonPathObjectDeserializer {

    private final Jackson1ObjectMapperFactory factory;

    JsonPathJackson1ObjectDeserializer(Jackson1ObjectMapperFactory factory) {
        this.factory = factory
    }

    private org.codehaus.jackson.map.ObjectMapper createJacksonObjectMapper(Class cls, String charset) {
        return factory.create(cls, charset)
    }

    @Override
    def <T> T deserialize(ObjectDeserializationContext ctx) {
        def object = ctx.getDataToDeserialize().asString()
        def cls = ctx.getType()
        def mapper = createJacksonObjectMapper(cls, ctx.getCharset())
        JavaType javaType = mapper.constructType(cls)
        return mapper.readValue(object, javaType) as T
    }
}
