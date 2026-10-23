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

package io.restassured.path.jayway;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JaywayPathTest {
    private static final String JSON = "{\"store\":{\"book\":[{\"title\":\"t0\",\"price\":0.5},{\"title\":\"t1\",\"price\":1.5}]}}";

    @Test
    void reads_paths() {
        DocumentContext document = JaywayPath.from(JSON);

        List<String> titles = document.read("$.store.book[*].title");
        assertThat(titles).containsExactly("t0", "t1");
        assertThat((Float) document.read("store.book[1].price")).isEqualTo(1.5f);
        assertThat(document.read("store.book[1].price", BigDecimal.class)).isEqualTo(new BigDecimal("1.5"));
        assertThat(document.read("store.book.length()", Long.class)).isEqualTo(2L);
        assertThat(document.read("store.book[0].price", String.class)).isEqualTo("0.5");
    }

    @Test
    void writes_json() {
        assertThat(JaywayPath.from(JSON).set("$.store.book[0].title", "a\"b").jsonString())
                .isEqualTo("{\"store\":{\"book\":[{\"title\":\"a\\\"b\",\"price\":0.5},{\"title\":\"t1\",\"price\":1.5}]}}");
    }

    @Test
    void invalid_json_fails() {
        assertThatThrownBy(() -> JaywayPath.from("{a:1}")).isInstanceOf(InvalidJsonException.class);
    }
}
