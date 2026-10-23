package io.restassured.internal.path.json.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;

import java.lang.reflect.Type;

public class JsonPathJackson2ObjectDeserializer implements JsonPathObjectDeserializer {
    public JsonPathJackson2ObjectDeserializer(Jackson2ObjectMapperFactory factory) {
        this.factory = factory;
    }

    private ObjectMapper createJackson2ObjectMapper(Type cls, String charset) {
        return factory.create(cls, charset);
    }

    @Override
    public <T> T deserialize(ObjectDeserializationContext context) {
        String object = context.getDataToDeserialize().asString();
        Type cls = context.getType();
        ObjectMapper mapper = createJackson2ObjectMapper(cls, context.getCharset());
        JavaType javaType = mapper.constructType(cls);
        try {
            return mapper.readValue(object, javaType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private final Jackson2ObjectMapperFactory factory;
}
