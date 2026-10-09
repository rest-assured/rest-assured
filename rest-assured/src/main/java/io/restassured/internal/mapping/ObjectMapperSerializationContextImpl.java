/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.internal.mapping;

import io.restassured.internal.util.GroovyStringConversion;
import io.restassured.mapper.ObjectMapperSerializationContext;

public class ObjectMapperSerializationContextImpl implements ObjectMapperSerializationContext {

    private Object object;
    // Object-typed, like the Groovy properties they replace; the getters convert to String the way Groovy did
    private Object contentType;
    private Object charset;

    @Override
    public Object getObjectToSerialize() {
        return object;
    }

    @Override
    public <T> T getObjectToSerializeAs(Class<T> expectedType) {
        // For null, Groovy checked against its NullObject class, which only Object.class is assignable from
        if (object == null ? expectedType != Object.class : !expectedType.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Object to serialize is not of required type " + expectedType);
        }
        return expectedType.cast(object);
    }

    @Override
    public String getContentType() {
        return GroovyStringConversion.castToString(contentType);
    }

    @Override
    public String getCharset() {
        return GroovyStringConversion.castToString(charset);
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setContentType(Object contentType) {
        this.contentType = contentType;
    }

    public void setCharset(Object charset) {
        this.charset = charset;
    }
}
