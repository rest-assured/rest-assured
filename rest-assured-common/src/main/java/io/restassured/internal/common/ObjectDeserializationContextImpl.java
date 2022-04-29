package io.restassured.internal.common;

import io.restassured.common.mapper.DataToDeserialize;
import io.restassured.common.mapper.ObjectDeserializationContext;

import java.lang.reflect.Type;

public class ObjectDeserializationContextImpl implements ObjectDeserializationContext {

    private DataToDeserialize dataToDeserialize;
    private Type type;
    private String charset;

    // Used by groovy
    public ObjectDeserializationContextImpl() {
    }

    public ObjectDeserializationContextImpl(DataToDeserialize dataToDeserialize, Type type, String charset) {
        this.dataToDeserialize = dataToDeserialize;
        this.type = type;
        this.charset = charset;
    }

    @Override
    public DataToDeserialize getDataToDeserialize() {
        return dataToDeserialize;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getCharset() {
        return charset;
    }
}