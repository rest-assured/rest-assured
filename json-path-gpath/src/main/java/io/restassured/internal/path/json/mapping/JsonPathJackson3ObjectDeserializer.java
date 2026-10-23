package io.restassured.internal.path.json.mapping;

import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.path.json.mapper.factory.Jackson3ObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

public class JsonPathJackson3ObjectDeserializer implements JsonPathObjectDeserializer {
    public JsonPathJackson3ObjectDeserializer(Jackson3ObjectMapperFactory factory) {
        this.factory = factory;
    }

    private ObjectMapper createJackson3JsonMapper(Type cls, String charset) {
        return factory.create(cls, charset);
    }

    @Override
    public <T> T deserialize(ObjectDeserializationContext context) {
        String object = context.getDataToDeserialize().asString();
        Type cls = context.getType();
        ObjectMapper mapper = createJackson3JsonMapper(cls, context.getCharset());
        JavaType javaType = mapper.constructType(cls);
        return mapper.readValue(object, javaType);
    }

    private final Jackson3ObjectMapperFactory factory;
}
