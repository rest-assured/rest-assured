/*
 * Copyright 2026 the original author or authors.
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

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the accessors that Java and Groovy callers (core, spring-commons, MultiPartSpecBuilder) use on the
 * object mapper context implementations.
 */
class ObjectMapperContextImplTest {

    @Test
    void serialization_context_exposes_what_was_set() {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        Object object = new Object();
        ctx.setObject(object);
        ctx.setContentType("application/json");
        ctx.setCharset("UTF-8");

        assertThat(ctx.getObject()).isSameAs(object);
        assertThat(ctx.getObjectToSerialize()).isSameAs(object);
        assertThat(ctx.getContentType()).isEqualTo("application/json");
        assertThat(ctx.getCharset()).isEqualTo("UTF-8");
    }

    @Test
    void serialization_context_is_empty_by_default() {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();

        assertThat(ctx.getObject()).isNull();
        assertThat(ctx.getObjectToSerialize()).isNull();
        assertThat(ctx.getContentType()).isNull();
        assertThat(ctx.getCharset()).isNull();
    }

    @Test
    void serialization_context_renders_non_string_content_type_and_charset_like_groovy() {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setContentType(ContentType.JSON);
        ctx.setCharset(new byte[]{65});

        assertThat(ctx.getContentType()).isEqualTo("application/json");
        assertThat(ctx.getCharset()).isEqualTo("[65]");
    }

    @Test
    void serialization_context_returns_object_as_expected_type() {
        ObjectMapperSerializationContextImpl impl = new ObjectMapperSerializationContextImpl();
        impl.setObject("hello");
        ObjectMapperSerializationContext ctx = impl;

        String asString = ctx.getObjectToSerializeAs(String.class);
        CharSequence asCharSequence = ctx.getObjectToSerializeAs(CharSequence.class);

        assertThat(asString).isEqualTo("hello");
        assertThat(asCharSequence).isEqualTo("hello");
    }

    @Test
    void serialization_context_rejects_object_of_other_type() {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setObject(1);

        assertThatThrownBy(() -> ctx.getObjectToSerializeAs(String.class))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object to serialize is not of required type class java.lang.String");
    }

    @Test
    void serialization_context_with_null_object() {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();

        Object asObject = ctx.getObjectToSerializeAs(Object.class);

        assertThat(asObject).isNull();
        assertThatThrownBy(() -> ctx.getObjectToSerializeAs(String.class))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object to serialize is not of required type class java.lang.String");
    }

    @Test
    void deserialization_context_exposes_what_was_set() {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();
        ctx.setContentType("application/xml");
        ctx.setCharset("UTF-16");
        ctx.setType(String.class);

        assertThat(ctx.getContentType()).isEqualTo("application/xml");
        assertThat(ctx.getCharset()).isEqualTo("UTF-16");
        assertThat(ctx.getType()).isEqualTo(String.class);
        assertThat(ctx.getDataToDeserialize()).isNull();
    }

    @Test
    void deserialization_context_renders_non_string_content_type_like_groovy() {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();

        assertThat(ctx.getContentType()).isNull();
        ctx.setContentType(Arrays.asList("a", 1));
        assertThat(ctx.getContentType()).isEqualTo("[a, 1]");
    }
}
