package io.restassured.internal.path.json.mapping;

import groovy.lang.Closure;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;
import jakarta.json.bind.Jsonb;
import org.codehaus.groovy.runtime.IOGroovyMethods;

import java.io.Reader;
import java.lang.reflect.Type;

public class JsonPathJsonbObjectDeserializer implements JsonPathObjectDeserializer {
    public JsonPathJsonbObjectDeserializer(JsonbObjectMapperFactory factory) {
        AssertParameter.notNull(factory, "JsonbObjectMapperFactory");
        this.factory = factory;
    }

    @Override
    public <T> T deserialize(ObjectDeserializationContext context) {
        final Type cls = context.getType();
        try (Jsonb mapper = factory.create(cls, context.getCharset())) {
            return IOGroovyMethods.withReader(context.getDataToDeserialize().asInputStream(), new Closure<T>(this, this) {
                public Object doCall(Object reader) {
                    return mapper.fromJson((Reader) reader, cls);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final JsonbObjectMapperFactory factory;
}
