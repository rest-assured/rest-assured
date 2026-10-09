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

package io.restassured.internal.common.assertion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssertParameterTest {

    @Test
    void returns_the_object_when_not_null() {
        List<String> list = new ArrayList<>();
        List<String> returnedByName = AssertParameter.notNull(list, "list");
        List<String> returnedByClass = AssertParameter.notNull(list, List.class);

        assertThat(returnedByName, sameInstance(list));
        assertThat(returnedByClass, sameInstance(list));
    }

    @Test
    void throws_with_parameter_name_when_null() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AssertParameter.notNull(null, "Some parameter"));

        assertThat(e.getMessage(), is("Some parameter cannot be null"));
    }

    @Test
    void throws_with_simple_class_name_when_null() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AssertParameter.notNull(null, java.util.Map.Entry.class));

        assertThat(e.getMessage(), is("Entry cannot be null"));
    }

    @Test
    void null_parameter_name_is_printed_as_null() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AssertParameter.notNull(null, (String) null));

        assertThat(e.getMessage(), is("null cannot be null"));
    }
}
