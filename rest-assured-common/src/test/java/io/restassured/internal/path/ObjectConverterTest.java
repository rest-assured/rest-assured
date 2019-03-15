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

package io.restassured.internal.path;

import io.restassured.internal.common.path.ObjectConverter;
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
