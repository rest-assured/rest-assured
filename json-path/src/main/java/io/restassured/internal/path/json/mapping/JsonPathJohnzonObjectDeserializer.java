package io.restassured.internal.path.json.mapping;

import groovy.lang.Closure;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;
import org.apache.johnzon.mapper.Mapper;
import org.codehaus.groovy.runtime.IOGroovyMethods;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;

public class JsonPathJohnzonObjectDeserializer implements JsonPathObjectDeserializer {
    public JsonPathJohnzonObjectDeserializer(JohnzonObjectMapperFactory factory) {
        AssertParameter.notNull(factory, "JohnzonObjectMapperFactory");
        this.factory = factory;
    }

    @Override
    public <T> T deserialize(ObjectDeserializationContext context) {
        final Type cls = context.getType();
        try (Mapper mapper = factory.create(cls, context.getCharset())) {
            return IOGroovyMethods.withReader(context.getDataToDeserialize().asInputStream(), new Closure<T>(this, this) {
                public Object doCall(Object reader) {
                    return mapper.readObject((Reader) reader, cls);
                }

            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final JohnzonObjectMapperFactory factory;
}
