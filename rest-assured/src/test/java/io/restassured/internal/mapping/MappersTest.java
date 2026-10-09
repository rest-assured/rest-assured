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

import com.google.gson.JsonObject;
import io.restassured.common.mapper.DataToDeserialize;
import io.restassured.internal.mapping.MappingTestSupport.Greeting;
import io.restassured.internal.mapping.MappingTestSupport.Unserializable;
import io.restassured.mapper.ObjectMapper;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.DefaultJakartaEEObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pins the behavior of each built-in {@link ObjectMapper} when used directly.
 */
class MappersTest {

    private static final String GREETING_JSON = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

    private final List<Type> requestedTypes = new ArrayList<>();

    private Map<String, ObjectMapper> jsonMappers() {
        Map<String, ObjectMapper> mappers = new LinkedHashMap<>();
        mappers.put("gson", new GsonMapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultGsonObjectMapperFactory().create(type, charset);
        }));
        mappers.put("jackson1", new Jackson1Mapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultJackson1ObjectMapperFactory().create(type, charset);
        }));
        mappers.put("jackson2", new Jackson2Mapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultJackson2ObjectMapperFactory().create(type, charset);
        }));
        mappers.put("jackson3", new Jackson3Mapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultJackson3ObjectMapperFactory().create(type, charset);
        }));
        mappers.put("johnzon", new JohnzonMapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultJohnzonObjectMapperFactory().create(type, charset);
        }));
        mappers.put("jsonb", new JsonbMapper((type, charset) -> {
            requestedTypes.add(type);
            return new DefaultYassonObjectMapperFactory().create(type, charset);
        }));
        return mappers;
    }

    @Test
    void json_mappers_serialize_a_bean_and_ask_the_factory_for_its_class() {
        jsonMappers().forEach((name, mapper) -> {
            requestedTypes.clear();
            assertThat(mapper.serialize(context(new Greeting("John", "Doe"), null))).as(name).isEqualTo(GREETING_JSON);
            assertThat(requestedTypes).as(name).containsExactly(Greeting.class);
        });
    }

    @Test
    void json_mappers_serialize_a_null_object_to_json_null() {
        jsonMappers().forEach((name, mapper) -> {
            requestedTypes.clear();
            assertThat(mapper.serialize(context(null, null))).as(name).isEqualTo("null");
            assertThat(requestedTypes).as(name).hasSize(1);
        });
    }

    @Test
    void json_mappers_deserialize_with_their_factory() {
        jsonMappers().forEach((name, mapper) -> {
            requestedTypes.clear();
            Object result = mapper.deserialize(deserializationContext(GREETING_JSON, Greeting.class, "UTF-8"));
            assertThat(result).as(name).isInstanceOf(Greeting.class);
            assertThat(((Greeting) result).getFirstName()).as(name).isEqualTo("John");
            assertThat(requestedTypes).as(name).containsExactly(Greeting.class);
        });
    }

    @Test
    void gson_mapper_serializes_gson_json_elements_as_json() {
        JsonObject json = new JsonObject();
        json.addProperty("a", "<b>");

        Object result = new GsonMapper(new DefaultGsonObjectMapperFactory()).serialize(context(json, null));

        assertThat(result).isEqualTo("{\"a\":\"\\u003cb\\u003e\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE", "ISO-8859-1", "utf-16le"})
    void jackson_mappers_return_the_json_as_a_string_whatever_the_charset(String charset) {
        Greeting greeting = new Greeting("Jöhn", "Dœ");
        String expected = "{\"firstName\":\"Jöhn\",\"lastName\":\"Dœ\"}";

        assertThat(new Jackson1Mapper(new DefaultJackson1ObjectMapperFactory()).serialize(context(greeting, charset))).isEqualTo(expected);
        assertThat(new Jackson2Mapper(new DefaultJackson2ObjectMapperFactory()).serialize(context(greeting, charset))).isEqualTo(expected);
        assertThat(new Jackson3Mapper(new DefaultJackson3ObjectMapperFactory()).serialize(context(greeting, charset))).isEqualTo(expected);
    }

    @Test
    void jackson_mappers_pass_the_charset_to_the_factory() {
        List<String> charsets = new ArrayList<>();
        new Jackson1Mapper((type, charset) -> {
            charsets.add(charset);
            return new DefaultJackson1ObjectMapperFactory().create(type, charset);
        }).serialize(context(new Greeting(), "UTF-16BE"));
        new Jackson2Mapper((type, charset) -> {
            charsets.add(charset);
            return new DefaultJackson2ObjectMapperFactory().create(type, charset);
        }).serialize(context(new Greeting(), "UTF-16BE"));
        new Jackson3Mapper((type, charset) -> {
            charsets.add(charset);
            return new DefaultJackson3ObjectMapperFactory().create(type, charset);
        }).serialize(context(new Greeting(), "UTF-16BE"));

        assertThat(charsets).containsExactly("UTF-16BE", "UTF-16BE", "UTF-16BE");
    }

    @Test
    void jackson_mappers_throw_mapping_exceptions_unwrapped() {
        assertThatThrownBy(() -> new Jackson1Mapper(new DefaultJackson1ObjectMapperFactory()).serialize(context(new Unserializable(), null)))
                .isInstanceOf(org.codehaus.jackson.map.JsonMappingException.class);
        assertThatThrownBy(() -> new Jackson2Mapper(new DefaultJackson2ObjectMapperFactory()).serialize(context(new Unserializable(), null)))
                .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        assertThatThrownBy(() -> new Jackson3Mapper(new DefaultJackson3ObjectMapperFactory()).serialize(context(new SelfReferencing(), null)))
                .isInstanceOf(tools.jackson.databind.DatabindException.class);
    }

    @Test
    void jakarta_ee_mapper_serializes_and_deserializes_xml() {
        JakartaEEMapper mapper = new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory());

        Object xml = mapper.serialize(context(new Greeting("John", "Doe"), null));
        Object greeting = mapper.deserialize(deserializationContext((String) xml, Greeting.class, null));

        assertThat(xml).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><greeting><firstName>John</firstName><lastName>Doe</lastName></greeting>");
        assertThat(((Greeting) greeting).getLastName()).isEqualTo("Doe");
    }

    @Test
    void jakarta_ee_mapper_sets_the_charset_as_jaxb_encoding() {
        Object xml = new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory()).serialize(context(new Greeting("John", "Doe"), "ISO-8859-1"));

        assertThat(xml).asString().startsWith("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>");
    }

    @Test
    void jakarta_ee_mapper_throws_jaxb_exceptions_unwrapped() {
        assertThatThrownBy(() -> new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory()).serialize(context(new Unserializable(), null)))
                .isInstanceOf(jakarta.xml.bind.MarshalException.class);
    }

    @Test
    void xml_mappers_fail_for_a_null_object() {
        assertThatThrownBy(() -> new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory()).serialize(context(null, null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void jaxb_mapper_marshals_with_the_charset_as_jaxb_encoding() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        Marshaller marshaller = mock(Marshaller.class);
        when(jaxbContext.createMarshaller()).thenReturn(marshaller);
        doAnswer(invocation -> {
            invocation.getArgument(1, Writer.class).write("<greeting/>");
            return null;
        }).when(marshaller).marshal(any(), any(Writer.class));
        List<Type> types = new ArrayList<>();
        Greeting greeting = new Greeting();

        Object result = new JaxbMapper((type, charset) -> {
            types.add(type);
            return jaxbContext;
        }).serialize(context(greeting, "ISO-8859-1"));

        assertThat(result).isEqualTo("<greeting/>");
        assertThat(types).containsExactly(Greeting.class);
        verify(marshaller).setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-1");
        verify(marshaller).marshal(same(greeting), any(Writer.class));
    }

    @Test
    void jaxb_mapper_does_not_set_jaxb_encoding_without_charset() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        Marshaller marshaller = mock(Marshaller.class);
        when(jaxbContext.createMarshaller()).thenReturn(marshaller);

        Object result = new JaxbMapper((type, charset) -> jaxbContext).serialize(context(new Greeting(), null));

        assertThat(result).isEqualTo("");
        verify(marshaller, never()).setProperty(any(), any());
    }

    @Test
    void jaxb_mapper_throws_jaxb_exceptions_unwrapped() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        Marshaller marshaller = mock(Marshaller.class);
        when(jaxbContext.createMarshaller()).thenReturn(marshaller);
        JAXBException exception = new JAXBException("boom");
        doThrow(exception).when(marshaller).marshal(any(), any(Writer.class));

        assertThatThrownBy(() -> new JaxbMapper((type, charset) -> jaxbContext).serialize(context(new Greeting(), null)))
                .isSameAs(exception);
    }

    public static class SelfReferencing {
        public SelfReferencing getSelf() {
            return this;
        }
    }

    private static ObjectMapperSerializationContextImpl context(Object object, String charset) {
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setObject(object);
        ctx.setCharset(charset);
        return ctx;
    }

    private static ObjectMapperDeserializationContextImpl deserializationContext(String body, Type type, String charset) {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();
        ctx.setType(type);
        ctx.setCharset(charset);
        ctx.setDataToDeserialize(new DataToDeserialize() {
            @Override
            public String asString() {
                return body;
            }

            @Override
            public byte[] asByteArray() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public InputStream asInputStream() {
                return new ByteArrayInputStream(asByteArray());
            }
        });
        return ctx;
    }
}
