/*
 * Copyright (c) [j]karef GmbH year .
 */

package io.restassured.internal.mapping

import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory
import org.junit.Before
import org.junit.Test

import javax.json.bind.JsonbBuilder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

class JsonbMapperTest {

    private JsonbObjectMapperFactory mockFactory = mock(JsonbObjectMapperFactory.class)
    private ObjectMapperSerializationContext mockContext = mock(ObjectMapperSerializationContext.class)

    private JsonbMapper underTest

    @Before
    def void setUp() {
        when(mockFactory.create(any(), any())).thenReturn(JsonbBuilder.create())
        underTest = new JsonbMapper(mockFactory);
    }

    @Test
    def void shouldSerializeStringIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn("hello world");

        final Object result = underTest.serialize(mockContext);

        verifyMocks()

        assertNotNull(result)
        assertEquals("\"hello world\"", result)

        resetMocks()
    }

    @Test
    def void shouldSerializeNullIntoJson() {
        when(mockContext.getObjectToSerialize()).thenReturn(null)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertNotNull(result)
        assertEquals("null", result)

        resetMocks()
    }

    @Test
    def void shouldSerializeObjectIntoJson() {
        def obj = new Object()
        when(mockContext.getObjectToSerialize()).thenReturn(obj)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertNotNull(result)
        assertEquals("{}", result)

        resetMocks()
    }

    @Test
    def void shouldSerializeCollectionIntoJson() {
        def list = ["a", "b", "c"]
        when(mockContext.getObjectToSerialize()).thenReturn(list)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertNotNull(result)
        assertEquals("[\"a\",\"b\",\"c\"]", result)

        resetMocks()
    }

    @Test
    def void shouldSerializeMapToJson() {
        def map = [:]
        when(mockContext.getObjectToSerialize()).thenReturn(map)

        final Object result = underTest.serialize(mockContext)

        verifyMocks()

        assertNotNull(result)
        assertEquals("{}", result)

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
