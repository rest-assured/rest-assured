package com.jayway.restassured.internal.path;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ObjectConverterTest {

    @Test public void
    integer_is_supported() {
        // When
        final boolean supported = ObjectConverter.canConvert(null, Integer.class);

        // Then
        assertThat(supported, is(true));
    }

    @Test public void
    checks_if_is_castable() {
        // When
        final boolean supported = ObjectConverter.canConvert((Number) 22, Integer.class);

        // Then
        assertThat(supported, is(true));
    }

    @Test public void
    returns_false_when_object_is_not_castable() {
        // When
        final boolean supported = ObjectConverter.canConvert(new ArrayList(), Integer.class);

        // Then
        assertThat(supported, is(false));
    }
}
