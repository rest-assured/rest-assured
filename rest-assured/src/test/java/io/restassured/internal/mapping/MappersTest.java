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
import io.restassured.common.mapper.TypeRef;
import io.restassured.internal.mapping.MappingTestSupport.Greeting;
import io.restassured.internal.mapping.MappingTestSupport.Unserializable;
import io.restassured.mapper.ObjectMapper;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.DefaultJakartaEEObjectMapperFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
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
            // The Groovy implementation passed Groovy's internal NullObject class here
            assertThat(requestedTypes).as(name).containsExactly(Object.class);
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

    @Test
    void gson_johnzon_and_jsonb_mappers_fail_for_a_null_factory_and_jackson_mappers_dont() {
        assertThatThrownBy(() -> new GsonMapper(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JohnzonMapper(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JsonbMapper(null)).isInstanceOf(IllegalArgumentException.class);
        new Jackson2Mapper(null);
        new Jackson3Mapper(null);
    }

    @Test
    void json_mappers_deserialize_generic_types() {
        Type listOfGreetings = new TypeRef<List<Greeting>>() {
        }.getType();

        jsonMappers().forEach((name, mapper) -> {
            Object result = mapper.deserialize(deserializationContext("[" + GREETING_JSON + "]", listOfGreetings, "UTF-8"));
            assertThat(result).as(name).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
        });
    }

    @Test
    void johnzon_and_jsonb_mappers_read_the_body_stream_with_the_platform_default_charset() {
        byte[] latin1 = "{\"firstName\":\"Jöhn\",\"lastName\":\"Doe\"}".getBytes(StandardCharsets.ISO_8859_1);
        String expectedFirstName = new String("Jöhn".getBytes(StandardCharsets.ISO_8859_1), Charset.defaultCharset());

        Object johnzon = new JohnzonMapper(new DefaultJohnzonObjectMapperFactory()).deserialize(deserializationContext(latin1, Greeting.class, "ISO-8859-1"));
        Object jsonb = new JsonbMapper(new DefaultYassonObjectMapperFactory()).deserialize(deserializationContext(latin1, Greeting.class, "ISO-8859-1"));

        assertThat(((Greeting) johnzon).getFirstName()).isEqualTo(expectedFirstName);
        assertThat(((Greeting) jsonb).getFirstName()).isEqualTo(expectedFirstName);
    }

    @Test
    void json_mappers_fail_for_invalid_json_like_before() {
        Map<String, Class<? extends Throwable>> failures = new LinkedHashMap<>();
        jsonMappers().forEach((name, mapper) -> {
            Throwable thrown = catchThrowable(() -> mapper.deserialize(deserializationContext("{\"firstName\":", Greeting.class, "UTF-8")));
            failures.put(name, thrown.getClass());
            if (thrown.getClass() == RuntimeException.class) {
                failures.put(name + " cause", thrown.getCause().getClass());
            }
        });

        assertThat(failures).containsExactly(
                entry("gson", com.google.gson.JsonSyntaxException.class),
                entry("jackson1", java.io.UncheckedIOException.class),
                entry("jackson2", RuntimeException.class),
                entry("jackson2 cause", com.fasterxml.jackson.core.io.JsonEOFException.class),
                entry("jackson3", tools.jackson.core.exc.UnexpectedEndOfInputException.class),
                entry("johnzon", jakarta.json.stream.JsonParsingException.class),
                entry("jsonb", RuntimeException.class),
                entry("jsonb cause", jakarta.json.bind.JsonbException.class));
    }

    @Test
    void jakarta_ee_mapper_deserializes_a_class_without_xml_root_element_from_any_root() {
        Object result = new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory())
                .deserialize(deserializationContext("<anything><firstName>John</firstName></anything>", NoRootElement.class, null));

        assertThat(((NoRootElement) result).firstName).isEqualTo("John");
    }

    @Test
    void jakarta_ee_mapper_throws_unmarshal_exceptions_unwrapped() {
        assertThatThrownBy(() -> new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory()).deserialize(deserializationContext("<greeting>", Greeting.class, null)))
                .isInstanceOf(jakarta.xml.bind.UnmarshalException.class);
    }

    @Test
    void jaxb_mapper_unmarshals_a_root_element_from_a_reader_and_another_class_as_declared_type() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        Unmarshaller unmarshaller = mock(Unmarshaller.class);
        when(jaxbContext.createUnmarshaller()).thenReturn(unmarshaller);
        Greeting greeting = new Greeting("John", "Doe");
        NoRootElement noRootElement = new NoRootElement();
        when(unmarshaller.unmarshal(any(Reader.class))).thenReturn(greeting);
        when(unmarshaller.unmarshal(any(Source.class), eq(NoRootElement.class))).thenReturn(new JAXBElement<>(new QName("x"), NoRootElement.class, noRootElement));
        JaxbMapper mapper = new JaxbMapper((type, charset) -> jaxbContext);

        assertThat(mapper.deserialize(deserializationContext("<greeting/>", Greeting.class, null))).isSameAs(greeting);
        assertThat(mapper.deserialize(deserializationContext("<x/>", NoRootElement.class, null))).isSameAs(noRootElement);
    }

    @Test
    void jaxb_mapper_throws_unmarshal_exceptions_unwrapped() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        Unmarshaller unmarshaller = mock(Unmarshaller.class);
        when(jaxbContext.createUnmarshaller()).thenReturn(unmarshaller);
        JAXBException exception = new JAXBException("boom");
        when(unmarshaller.unmarshal(any(Reader.class))).thenThrow(exception);

        assertThatThrownBy(() -> new JaxbMapper((type, charset) -> jaxbContext).deserialize(deserializationContext("<greeting/>", Greeting.class, null)))
                .isSameAs(exception);
    }

    @Test
    void jaxb_and_jakarta_ee_mappers_fail_for_a_type_that_isnt_a_class() {
        Type listOfGreetings = new TypeRef<List<Greeting>>() {
        }.getType();
        String message = "JAXB can only deserialize to a class, not to java.util.List<" + Greeting.class.getName() + ">.";

        assertThatThrownBy(() -> new JaxbMapper(new DefaultJAXBObjectMapperFactory()).deserialize(deserializationContext("<greeting/>", listOfGreetings, null)))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(message);
        assertThatThrownBy(() -> new JakartaEEMapper(new DefaultJakartaEEObjectMapperFactory()).deserialize(deserializationContext("<greeting/>", listOfGreetings, null)))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    public static class NoRootElement {
        public String firstName;
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

    private static ObjectMapperDeserializationContextImpl deserializationContext(byte[] body, Type type, String charset) {
        ObjectMapperDeserializationContextImpl ctx = deserializationContext(new String(body, StandardCharsets.UTF_8), type, charset);
        ctx.setDataToDeserialize(new DataToDeserialize() {
            @Override
            public String asString() {
                throw new AssertionError("Must read the stream");
            }

            @Override
            public byte[] asByteArray() {
                return body;
            }

            @Override
            public InputStream asInputStream() {
                return new ByteArrayInputStream(body);
            }
        });
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
