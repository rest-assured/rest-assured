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
package io.restassured.internal.path;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.withArgs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathArgumentsTest {

    @Test
    void keeps_placeholders_without_an_argument_for_the_next_list() {
        assertThat(PathArguments.apply("%s.%s", withArgs("a"))).isEqualTo("a.%s");
        assertThat(PathArguments.applyInOrder("%s.%s", List.of(withArgs("a"), withArgs("b")))).isEqualTo("a.b");
    }

    @Test
    void no_arguments_leave_the_path_as_is() {
        assertThat(PathArguments.applyInOrder("%s", List.of())).isEqualTo("%s");
        assertThat(PathArguments.apply("%s", null)).isEqualTo("%s");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fails_for_an_argument_that_isnt_from_with_args() {
        assertThatThrownBy(() -> PathArguments.apply("%s", (List) List.of("a")))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Path arguments must be instances of io.restassured.specification.Argument");
    }
}
