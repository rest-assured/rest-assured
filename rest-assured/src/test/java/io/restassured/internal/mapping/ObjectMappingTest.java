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

import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.mapping.MappingTestSupport.Greeting;
import io.restassured.internal.mapping.MappingTestSupport.Unserializable;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.DefaultJakartaEEObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.internal.mapping.MappingTestSupport.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pins how {@link ObjectMapping} picks a mapper and what it returns.
 */
class ObjectMappingTest {

    private static final String GREETING_JSON = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";
    private static final String GREETING_XML_BODY = "<greeting><firstName>John</firstName><lastName>Doe</lastName></greeting>";

    private final List<String> usedMappers = new ArrayList<>();
    private final List<Type> requestedTypes = new ArrayList<>();
    private final List<String> requestedCharsets = new ArrayList<>();

    // Serialization: which mapper is chosen

    @ParameterizedTest
    @EnumSource(ObjectMapperType.class)
    void explicit_mapper_type_selects_that_mapper_for_serialization(ObjectMapperType type) {
        String result = ObjectMapping.serialize(new Greeting("John", "Doe"), "application/json", "UTF-8", type, recording(new ObjectMapperConfig()), new EncoderConfig());

        assertThat(usedMappers).containsExactly(type.name());
        assertThat(requestedTypes).containsExactly(Greeting.class);
        assertThat(requestedCharsets).containsExactly("UTF-8");
        assertThat(result).isEqualTo(expectedSerializedGreeting(type));
    }

    @Test
    void explicit_mapper_type_wins_over_content_type() {
        String result = ObjectMapping.serialize(new Greeting("John", "Doe"), "application/xml", null, ObjectMapperType.GSON, recording(new ObjectMapperConfig()), new EncoderConfig());

        assertThat(usedMappers).containsExactly("GSON");
        assertThat(result).isEqualTo(GREETING_JSON);
    }

    @Test
    void default_mapper_type_is_used_when_no_explicit_mapper_type_is_given() {
        ObjectMapping.serialize(new Greeting("John", "Doe"), "application/json", null, null, recording(new ObjectMapperConfig(ObjectMapperType.JOHNZON)), new EncoderConfig());

        assertThat(usedMappers).containsExactly("JOHNZON");
    }

    @Test
    void explicit_mapper_type_wins_over_default_mapper_type() {
        ObjectMapping.serialize(new Greeting("John", "Doe"), "application/json", null, ObjectMapperType.JSONB, recording(new ObjectMapperConfig(ObjectMapperType.JOHNZON)), new EncoderConfig());

        assertThat(usedMappers).containsExactly("JSONB");
    }

    @Test
    void default_object_mapper_wins_over_explicit_mapper_type_and_gets_a_fresh_context() {
        AtomicReference<ObjectMapperSerializationContext> ctx = new AtomicReference<>();
        ObjectMapper mapper = serializingMapper(c -> {
            ctx.set(c);
            return "custom";
        });
        Greeting greeting = new Greeting("John", "Doe");

        String result = ObjectMapping.serialize(greeting, "application/json; charset=UTF-16", "UTF-16", ObjectMapperType.GSON, recording(new ObjectMapperConfig(mapper)), new EncoderConfig());

        assertThat(result).isEqualTo("custom");
        assertThat(usedMappers).isEmpty();
        assertThat(ctx.get()).isInstanceOf(ObjectMapperSerializationContextImpl.class);
        assertThat(ctx.get().getObjectToSerialize()).isSameAs(greeting);
        assertThat(ctx.get().getContentType()).isEqualTo("application/json; charset=UTF-16");
        assertThat(ctx.get().getCharset()).isEqualTo("UTF-16");
    }

    @ParameterizedTest
    @ValueSource(strings = {"*/*", "application/json", "APPLICATION/JSON", "application/vnd.api+json; charset=UTF-8", "text/json"})
    void json_like_or_unspecified_content_type_serializes_with_the_first_json_mapper_in_classpath(String contentType) {
        String result = ObjectMapping.serialize(new Greeting("John", "Doe"), contentType, null, null, recording(new ObjectMapperConfig()), new EncoderConfig());

        assertThat(usedMappers).containsExactly("JACKSON_3");
        assertThat(result).isEqualTo(GREETING_JSON);
    }

    @Test
    void null_content_type_serializes_with_the_first_mapper_in_classpath() {
        ObjectMapping.serialize(new Greeting("John", "Doe"), null, null, null, recording(new ObjectMapperConfig()), new EncoderConfig());

        assertThat(usedMappers).containsExactly("JACKSON_3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "TEXT/XML", "application/soap+xml; charset=UTF-8"})
    void xml_like_content_type_serializes_with_the_first_xml_mapper_in_classpath(String contentType) {
        String result = ObjectMapping.serialize(new Greeting("John", "Doe"), contentType, null, null, recording(new ObjectMapperConfig()), new EncoderConfig());

        assertThat(usedMappers).containsExactly("JAKARTA_EE");
        assertThat(result).endsWith(GREETING_XML_BODY);
    }

    @Test
    void content_type_registered_as_json_in_encoder_config_serializes_as_json() {
        EncoderConfig encoderConfig = new EncoderConfig().encodeContentTypeAs("application/custom", ContentType.JSON);

        String result = ObjectMapping.serialize(new Greeting("John", "Doe"), "Application/Custom; charset=UTF-8", null, null, recording(new ObjectMapperConfig()), encoderConfig);

        assertThat(usedMappers).containsExactly("JACKSON_3");
        assertThat(result).isEqualTo(GREETING_JSON);
    }

    @Test
    void content_type_registered_as_xml_in_encoder_config_serializes_as_xml() {
        EncoderConfig encoderConfig = new EncoderConfig().encodeContentTypeAs("application/custom", ContentType.XML);

        ObjectMapping.serialize(new Greeting("John", "Doe"), "application/custom", null, null, recording(new ObjectMapperConfig()), encoderConfig);

        assertThat(usedMappers).containsExactly("JAKARTA_EE");
    }

    @Test
    void unsupported_content_type_is_rejected() {
        assertThatThrownBy(() -> ObjectMapping.serialize(new Greeting("John", "Doe"), "Text/Plain", null, null, new ObjectMapperConfig(), new EncoderConfig()))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize because cannot determine how to serialize content-type Text/Plain");
    }

    @Test
    void unsupported_content_type_registered_in_encoder_config_is_rejected_with_the_encoder_name() {
        EncoderConfig encoderConfig = new EncoderConfig().encodeContentTypeAs("application/custom", ContentType.TEXT);

        assertThatThrownBy(() -> ObjectMapping.serialize(new Greeting("John", "Doe"), "application/custom; charset=UTF-8", null, null, new ObjectMapperConfig(), encoderConfig))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize because cannot determine how to serialize content-type application/custom; charset=UTF-8 as TEXT (no serializer supports this format)");
    }

    @Test
    void serialize_rejects_null_arguments() {
        assertThatThrownBy(() -> ObjectMapping.serialize(null, null, null, null, new ObjectMapperConfig(), new EncoderConfig()))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("String to serialize cannot be null");
        assertThatThrownBy(() -> ObjectMapping.serialize("x", null, null, null, null, new EncoderConfig()))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object mapper configuration not found, cannot serialize object. cannot be null");
        assertThatThrownBy(() -> ObjectMapping.serialize("x", null, null, null, new ObjectMapperConfig(), null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Encoder configuration not found, cannot serialize object. cannot be null");
    }

    @Test
    void checked_exceptions_from_a_mapper_propagate_unwrapped() {
        assertThatThrownBy(() -> ObjectMapping.serialize(new Unserializable(), "application/json", null, ObjectMapperType.JACKSON_2, new ObjectMapperConfig(), new EncoderConfig()))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidDefinitionException.class);
        assertThatThrownBy(() -> ObjectMapping.serialize(new Unserializable(), "application/json", null, ObjectMapperType.JACKSON_1, new ObjectMapperConfig(), new EncoderConfig()))
                .isInstanceOf(org.codehaus.jackson.map.JsonMappingException.class);
        assertThatThrownBy(() -> ObjectMapping.serialize(new Unserializable(), "application/xml", null, ObjectMapperType.JAKARTA_EE, new ObjectMapperConfig(), new EncoderConfig()))
                .isInstanceOf(jakarta.xml.bind.JAXBException.class);

        JAXBException jaxbException = new JAXBException("boom");
        ObjectMapperConfig config = new ObjectMapperConfig().jaxbObjectMapperFactory((cls, charset) -> {
            try {
                JAXBContext context = mock(JAXBContext.class);
                when(context.createMarshaller()).thenThrow(jaxbException);
                return context;
            } catch (JAXBException e) {
                throw new AssertionError(e);
            }
        });
        assertThatThrownBy(() -> ObjectMapping.serialize(new Greeting(), "application/xml", null, ObjectMapperType.JAXB, config, new EncoderConfig()))
                .isSameAs(jaxbException);
    }

    // Serialization: what a custom default object mapper's result becomes (Groovy coerces the declared String return)

    @Test
    void string_returned_by_default_object_mapper_is_returned_as_is() {
        assertThat(serializeWithDefaultMapperReturning("a string")).isEqualTo("a string");
    }

    @Test
    void null_returned_by_default_object_mapper_is_returned_as_null() {
        assertThat(serializeWithDefaultMapperReturning(null)).isNull();
    }

    @Test
    void byte_array_returned_by_default_object_mapper_is_rendered_as_a_list_of_numbers() {
        assertThat(serializeWithDefaultMapperReturning(new byte[]{65, 66})).isEqualTo("[65, 66]");
        assertThat(serializeWithDefaultMapperReturning(new byte[0])).isEqualTo("[]");
    }

    @Test
    void other_primitive_arrays_returned_by_default_object_mapper_are_rendered_as_lists() {
        assertThat(serializeWithDefaultMapperReturning(new int[]{1, 2})).isEqualTo("[1, 2]");
        assertThat(serializeWithDefaultMapperReturning(new boolean[]{true, false})).isEqualTo("[true, false]");
        assertThat(serializeWithDefaultMapperReturning(new double[]{1.5})).isEqualTo("[1.5]");
    }

    @Test
    void char_array_returned_by_default_object_mapper_becomes_the_string_of_its_chars() {
        assertThat(serializeWithDefaultMapperReturning(new char[]{'a', 'b'})).isEqualTo("ab");
    }

    @Test
    void object_array_returned_by_default_object_mapper_is_rendered_as_a_list() {
        assertThat(serializeWithDefaultMapperReturning(new Object[]{"a", 1, null, new byte[]{1}})).isEqualTo("[a, 1, null, [1]]");
        Object[] selfReferencing = new Object[1];
        selfReferencing[0] = selfReferencing;
        assertThat(serializeWithDefaultMapperReturning(selfReferencing)).isEqualTo("[(this array)]");
    }

    @Test
    void map_returned_by_default_object_mapper_is_rendered_in_groovy_map_syntax() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", "two");
        map.put(null, null);
        map.put("nested", Collections.singletonMap("x", Arrays.asList(1, 2)));
        map.put("chars", new char[]{'h', 'i'});

        assertThat(serializeWithDefaultMapperReturning(map)).isEqualTo("[a:1, b:two, null:null, nested:[x:[1, 2]], chars:hi]");
    }

    @Test
    void empty_and_self_referencing_maps_returned_by_default_object_mapper() {
        assertThat(serializeWithDefaultMapperReturning(new HashMap<>())).isEqualTo("[:]");
        Map<Object, Object> self = new LinkedHashMap<>();
        self.put("me", self);
        assertThat(serializeWithDefaultMapperReturning(self)).isEqualTo("[me:(this Map)]");
    }

    @Test
    void collections_returned_by_default_object_mapper_are_rendered_in_groovy_list_syntax() {
        assertThat(serializeWithDefaultMapperReturning(Arrays.asList(1, "two", null, Collections.emptyMap()))).isEqualTo("[1, two, null, [:]]");
        assertThat(serializeWithDefaultMapperReturning(new LinkedHashSet<>(Arrays.asList("a", "b")))).isEqualTo("[a, b]");
        assertThat(serializeWithDefaultMapperReturning(new ArrayList<>())).isEqualTo("[]");
        List<Object> self = new ArrayList<>();
        self.add(self);
        assertThat(serializeWithDefaultMapperReturning(self)).isEqualTo("[(this Collection)]");
    }

    @Test
    void other_objects_returned_by_default_object_mapper_use_their_to_string() {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
        assertThat(serializeWithDefaultMapperReturning(stream)).isEqualTo(stream.toString());
        assertThat(serializeWithDefaultMapperReturning(5)).isEqualTo("5");
        assertThat(serializeWithDefaultMapperReturning(new StringBuilder("sb"))).isEqualTo("sb");
        assertThat(serializeWithDefaultMapperReturning(ContentType.JSON)).isEqualTo(ContentType.JSON.toString());
        assertThat(serializeWithDefaultMapperReturning(Optional.of("x"))).isEqualTo("Optional[x]");
    }

    @Test
    void groovy_ranges_returned_by_default_object_mapper_use_their_to_string() {
        assertThat(serializeWithDefaultMapperReturning(new groovy.lang.IntRange(1, 3))).isEqualTo("1..3");
        assertThat(serializeWithDefaultMapperReturning(Collections.singletonMap("k", new groovy.lang.IntRange(false, 1, 3)))).isEqualTo("[k:1..<3]");
    }

    @Test
    void dom_element_returned_by_default_object_mapper_is_pretty_printed_as_xml() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("greeting");
        Element child = document.createElement("firstName");
        child.setTextContent("John");
        root.appendChild(child);
        document.appendChild(root);

        assertThat(serializeWithDefaultMapperReturning(root)).isEqualTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><greeting>" + System.lineSeparator() +
                        "  <firstName>John</firstName>" + System.lineSeparator() +
                        "</greeting>" + System.lineSeparator());
    }

    @Test
    void exception_from_to_string_of_returned_object_propagates() {
        IllegalStateException boom = new IllegalStateException("boom");
        Object badToString = new Object() {
            @Override
            public String toString() {
                throw boom;
            }
        };

        assertThatThrownBy(() -> serializeWithDefaultMapperReturning(Collections.singletonList(badToString))).isSameAs(boom);
    }

    // Deserialization: which mapper is chosen

    @ParameterizedTest
    @EnumSource(ObjectMapperType.class)
    void explicit_mapper_type_selects_that_mapper_for_deserialization(ObjectMapperType type) {
        boolean xml = type == ObjectMapperType.JAXB || type == ObjectMapperType.JAKARTA_EE;
        String body = xml ? GREETING_XML_BODY : GREETING_JSON;

        Object result = ObjectMapping.deserialize(body(body), Greeting.class, null, null, "UTF-8", type, recording(new ObjectMapperConfig()));

        assertThat(usedMappers).containsExactly(type.name());
        assertThat(requestedTypes).containsExactly(Greeting.class);
        assertThat(requestedCharsets).containsExactly("UTF-8");
        if (type == ObjectMapperType.JAXB) {
            assertThat(result).isSameAs(JAXB_UNMARSHALLED);
        } else {
            assertThat(result).isInstanceOf(Greeting.class);
            assertThat(((Greeting) result).getFirstName()).isEqualTo("John");
            assertThat(((Greeting) result).getLastName()).isEqualTo("Doe");
        }
    }

    @Test
    void explicit_mapper_type_wins_over_content_type_and_default_mapper_type_for_deserialization() {
        ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, "application/xml", null, null, ObjectMapperType.GSON, recording(new ObjectMapperConfig(ObjectMapperType.JOHNZON)));

        assertThat(usedMappers).containsExactly("GSON");
    }

    @Test
    void default_mapper_type_is_used_for_deserialization_when_no_explicit_type_is_given() {
        ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, "application/xml", null, null, null, recording(new ObjectMapperConfig(ObjectMapperType.JSONB)));

        assertThat(usedMappers).containsExactly("JSONB");
    }

    @Test
    void default_object_mapper_result_is_returned_untouched_whatever_the_requested_type() {
        AtomicReference<ObjectMapperDeserializationContext> ctx = new AtomicReference<>();
        Map<String, Object> mapperResult = Collections.singletonMap("a", 1);
        ObjectMapper mapper = new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                ctx.set(context);
                return mapperResult;
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                throw new UnsupportedOperationException();
            }
        };

        Object result = ObjectMapping.deserialize(body("the body"), String.class, "application/json", "application/xml", "UTF-16", ObjectMapperType.GSON, recording(new ObjectMapperConfig(mapper)));

        assertThat(result).isSameAs(mapperResult);
        assertThat(usedMappers).isEmpty();
        assertThat(ctx.get()).isInstanceOf(ObjectMapperDeserializationContextImpl.class);
        assertThat(ctx.get().getType()).isEqualTo(String.class);
        assertThat(ctx.get().getContentType()).isEqualTo("application/json");
        assertThat(ctx.get().getCharset()).isEqualTo("UTF-16");
        assertThat(ctx.get().getDataToDeserialize().asString()).isEqualTo("the body");
        assertThat(ctx.get().getDataToDeserialize().asByteArray()).isEqualTo("the body".getBytes());
        assertThat(ctx.get().getDataToDeserialize().asInputStream()).hasContent("the body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/vnd.api+JSON; charset=UTF-8"})
    void json_content_type_deserializes_with_the_first_json_mapper_in_classpath(String contentType) {
        ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, contentType, "application/xml", null, null, recording(new ObjectMapperConfig()));

        assertThat(usedMappers).containsExactly("JACKSON_3");
    }

    @Test
    void xml_content_type_deserializes_with_the_first_xml_mapper_in_classpath() {
        Object result = ObjectMapping.deserialize(body(GREETING_XML_BODY), Greeting.class, "text/XML", "application/json", null, null, recording(new ObjectMapperConfig()));

        assertThat(usedMappers).containsExactly("JAKARTA_EE");
        assertThat(result).isInstanceOf(Greeting.class);
    }

    @Test
    void default_content_type_is_used_for_deserialization_when_content_type_is_neither_json_nor_xml() {
        ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, "text/plain", "application/JSON", null, null, recording(new ObjectMapperConfig()));
        ObjectMapping.deserialize(body(GREETING_XML_BODY), Greeting.class, null, "application/xml", null, null, recording(new ObjectMapperConfig()));

        assertThat(usedMappers).containsExactly("JACKSON_3", "JAKARTA_EE");
    }

    @Test
    void deserialization_fails_when_neither_content_type_nor_default_content_type_is_supported() {
        assertThatThrownBy(() -> ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, "text/plain", "text/html", null, null, new ObjectMapperConfig()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot parse object because no supported Content-Type was specified in response. Content-Type was 'text/plain'.");
        assertThatThrownBy(() -> ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, null, null, null, null, new ObjectMapperConfig()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot parse object because no supported Content-Type was specified in response. Content-Type was 'null'.");
    }

    @Test
    void deserialization_requires_an_object_mapper_config() {
        assertThatThrownBy(() -> ObjectMapping.deserialize(body(GREETING_JSON), Greeting.class, "application/json", null, null, null, null))
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("String mapper configuration wasn't found, cannot deserialize.");
    }

    @Test
    void public_parse_helpers_deserialize_with_the_given_factory() {
        ObjectMapperDeserializationContextImpl ctx = new ObjectMapperDeserializationContextImpl();
        ctx.setType(Greeting.class);
        ctx.setContentType("application/json");
        ctx.setDataToDeserialize(new io.restassured.common.mapper.DataToDeserialize() {
            @Override
            public String asString() {
                return GREETING_JSON;
            }

            @Override
            public byte[] asByteArray() {
                return GREETING_JSON.getBytes();
            }

            @Override
            public java.io.InputStream asInputStream() {
                return new ByteArrayInputStream(GREETING_JSON.getBytes());
            }
        });

        assertThat(ObjectMapping.parseWithJackson1(ctx, new DefaultJackson1ObjectMapperFactory())).isInstanceOf(Greeting.class);
        assertThat(ObjectMapping.parseWithJackson2(ctx, new DefaultJackson2ObjectMapperFactory())).isInstanceOf(Greeting.class);
        assertThat(ObjectMapping.parseWithJackson3(ctx, new DefaultJackson3ObjectMapperFactory())).isInstanceOf(Greeting.class);
        assertThat(ObjectMapping.parseWithJohnzon(ctx, new DefaultJohnzonObjectMapperFactory())).isInstanceOf(Greeting.class);
        assertThat(ObjectMapping.parseWithJsonb(ctx, new DefaultYassonObjectMapperFactory())).isInstanceOf(Greeting.class);
    }

    // Helpers

    private static final Object JAXB_UNMARSHALLED = new Object();

    private static String expectedSerializedGreeting(ObjectMapperType type) {
        switch (type) {
            case JAXB:
                return "<jaxb/>";
            case JAKARTA_EE:
                return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + GREETING_XML_BODY;
            default:
                return GREETING_JSON;
        }
    }

    private static String serializeWithDefaultMapperReturning(Object value) {
        return ObjectMapping.serialize("ignored", "application/json", null, null, new ObjectMapperConfig(serializingMapper(ctx -> value)), new EncoderConfig());
    }

    private static ObjectMapper serializingMapper(java.util.function.Function<ObjectMapperSerializationContext, Object> serializer) {
        return new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                return serializer.apply(context);
            }
        };
    }

    private void record(String mapper, Type type, String charset) {
        usedMappers.add(mapper);
        requestedTypes.add(type);
        requestedCharsets.add(charset);
    }

    /**
     * Wraps every mapper factory of the given config so that the test can see which mapper was used.
     */
    private ObjectMapperConfig recording(ObjectMapperConfig base) {
        ObjectMapperConfig config = spy(base);
        doReturn((Jackson1ObjectMapperFactory) (type, charset) -> {
            record("JACKSON_1", type, charset);
            return new DefaultJackson1ObjectMapperFactory().create(type, charset);
        }).when(config).jackson1ObjectMapperFactory();
        doReturn((Jackson2ObjectMapperFactory) (type, charset) -> {
            record("JACKSON_2", type, charset);
            return new DefaultJackson2ObjectMapperFactory().create(type, charset);
        }).when(config).jackson2ObjectMapperFactory();
        doReturn((Jackson3ObjectMapperFactory) (type, charset) -> {
            record("JACKSON_3", type, charset);
            return new DefaultJackson3ObjectMapperFactory().create(type, charset);
        }).when(config).jackson3ObjectMapperFactory();
        doReturn((GsonObjectMapperFactory) (type, charset) -> {
            record("GSON", type, charset);
            return new DefaultGsonObjectMapperFactory().create(type, charset);
        }).when(config).gsonObjectMapperFactory();
        doReturn((JohnzonObjectMapperFactory) (type, charset) -> {
            record("JOHNZON", type, charset);
            return new DefaultJohnzonObjectMapperFactory().create(type, charset);
        }).when(config).johnzonObjectMapperFactory();
        doReturn((JsonbObjectMapperFactory) (type, charset) -> {
            record("JSONB", type, charset);
            return new DefaultYassonObjectMapperFactory().create(type, charset);
        }).when(config).jsonbObjectMapperFactory();
        doReturn((JakartaEEObjectMapperFactory) (type, charset) -> {
            record("JAKARTA_EE", type, charset);
            return new DefaultJakartaEEObjectMapperFactory().create(type, charset);
        }).when(config).jakartaEEObjectMapperFactory();
        doReturn((JAXBObjectMapperFactory) (type, charset) -> {
            record("JAXB", type, charset);
            return fakeJaxbContext();
        }).when(config).jaxbObjectMapperFactory();
        return config;
    }

    /**
     * There is no javax.xml.bind implementation on the test classpath, so JAXB is faked.
     */
    private static JAXBContext fakeJaxbContext() {
        try {
            JAXBContext context = mock(JAXBContext.class);
            Marshaller marshaller = mock(Marshaller.class);
            Unmarshaller unmarshaller = mock(Unmarshaller.class);
            when(context.createMarshaller()).thenReturn(marshaller);
            when(context.createUnmarshaller()).thenReturn(unmarshaller);
            doAnswer(invocation -> {
                invocation.getArgument(1, Writer.class).write("<jaxb/>");
                return null;
            }).when(marshaller).marshal(any(), any(Writer.class));
            when(unmarshaller.unmarshal(any(Reader.class))).thenReturn(JAXB_UNMARSHALLED);
            return context;
        } catch (JAXBException e) {
            throw new AssertionError(e);
        }
    }
}
