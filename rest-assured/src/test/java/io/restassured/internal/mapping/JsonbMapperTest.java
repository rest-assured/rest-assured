/*
 * Copyright (c) [j]karef GmbA year .
 */

package io.restassured.internal.mapping;

import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JsonbMapperTest {

    private final JsonbObjectMapperFactory mockFactory = mock(JsonbObjectMapperFactory.class);
    private final ObjectMapperSerializationContext mockContext = mock(ObjectMapperSerializationContext.class);

    private JsonbMapper underTest;

    @BeforeEach
    void setUp() {
        when(mockFactory.create(any(), any())).thenReturn(JsonbBuilder.create());
        underTest = new JsonbMapper(mockFactory);
    }

    @Test
    void shouldSerializeStringIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn("hello world");

        final Object result = underTest.serialize(mockContext);

        verifyMocks();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("\"hello world\"");
    }

    @Test
    void shouldSerializeNullIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn(null);

        final Object result = underTest.serialize(mockContext);

        verifyMocks();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("null");
    }

    @Test
    void shouldSerializeObjectIntoJson() {
        Object obj = new Object();
        when(mockContext.getObjectToSerialize()).thenReturn(obj);

        final Object result = underTest.serialize(mockContext);

        verifyMocks();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void shouldSerializeCollectionIntoJson() {
        List<String> list = Arrays.asList("a", "b", "c");
        when(mockContext.getObjectToSerialize()).thenReturn(list);

        final Object result = underTest.serialize(mockContext);

        verifyMocks();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("[\"a\",\"b\",\"c\"]");
    }

    @Test
    void shouldSerializeMapToJson() {
        Map<Object, Object> map = new HashMap<>();
        when(mockContext.getObjectToSerialize()).thenReturn(map);

        final Object result = underTest.serialize(mockContext);

        verifyMocks();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("{}");
    }

    private void verifyMocks() {
        verify(mockFactory).create(any(), any());
        verify(mockContext).getObjectToSerialize();
        verify(mockContext).getCharset();
    }
}
