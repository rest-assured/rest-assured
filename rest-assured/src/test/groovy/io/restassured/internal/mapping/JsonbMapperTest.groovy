/*
 * Copyright (c) [j]karef GmbA year .
 */

package io.restassured.internal.mapping

import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach

import jakarta.json.bind.JsonbBuilder

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*

class JsonbMapperTest {

    private JsonbObjectMapperFactory mockFactory = mock(JsonbObjectMapperFactory.class)
    private ObjectMapperSerializationContext mockContext = mock(ObjectMapperSerializationContext.class)

    private JsonbMapper underTest

    @BeforeEach
    void setUp() {
        when(mockFactory.create(any(), any())).thenReturn(JsonbBuilder.create())
        underTest = new JsonbMapper(mockFactory)
    }

    @Test
    void shouldSerializeStringIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn("hello world")

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo('"hello world"')

        resetMocks()
    }

    @Test
    void shouldSerializeNullIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn(null)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo('null')

        resetMocks()
    }

    @Test
    void shouldSerializeObjectIntoJson() {
        def obj = new Object()
        when(mockContext.getObjectToSerialize()).thenReturn(obj)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo('{}')

        resetMocks()
    }

    @Test
    void shouldSerializeCollectionIntoJson() {
        def list = ["a", "b", "c"]
        when(mockContext.getObjectToSerialize()).thenReturn(list)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo('["a","b","c"]')

        resetMocks()
    }

    @Test
    void shouldSerializeMapToJson() {
        def map = [:]
        when(mockContext.getObjectToSerialize()).thenReturn(map)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo('{}')

        resetMocks()
    }

    private void verifyMocks() {
        verify(mockFactory).create(any(), any())
        verify(mockContext).getObjectToSerialize()
        verify(mockContext).getCharset()
    }

    private void resetMocks() {
        reset(mockFactory)
        reset(mockContext)
    }

}
