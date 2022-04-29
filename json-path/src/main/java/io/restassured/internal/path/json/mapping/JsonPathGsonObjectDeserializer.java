package io.restassured.internal.path.json.mapping;

import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;

public class JsonPathGsonObjectDeserializer implements JsonPathObjectDeserializer {
    public JsonPathGsonObjectDeserializer(GsonObjectMapperFactory factory) {
        AssertParameter.notNull(factory, "GsonObjectMapperFactory");
        this.factory = factory;
    }

    @Override
    public <T> T deserialize(ObjectDeserializationContext ctx) {
        return factory.create(ctx.getType(), ctx.getCharset()).fromJson(ctx.getDataToDeserialize().asString(), ctx.getType());
    }

    private final GsonObjectMapperFactory factory;
}
