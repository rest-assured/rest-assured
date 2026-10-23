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

package io.restassured.internal.common.spi.path;

import org.junit.jupiter.api.Test;

import static io.restassured.internal.common.spi.path.GPathRootPath.join;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class GPathRootPathTest {

    @Test
    void without_a_root_path_the_key_is_used_as_is() {
        assertThat(join(null, "a.b"), is("a.b"));
        assertThat(join("", "a.b"), is("a.b"));
        assertThat(join("", ""), is(""));
    }

    @Test
    void an_empty_key_gives_the_root_path() {
        assertThat(join("a.b", ""), is("a.b"));
        assertThat(join("a.", ""), is("a."));
    }

    @Test
    void joins_with_a_dot() {
        assertThat(join("a", "b"), is("a.b"));
        assertThat(join("a", "b.c"), is("a.b.c"));
        assertThat(join("a", "'b-c'"), is("a.'b-c'"));
        assertThat(join("a", "find { it.b == 1 }"), is("a.find { it.b == 1 }"));
    }

    @Test
    void drops_one_dot_when_both_have_one() {
        assertThat(join("a.", ".b"), is("a.b"));
        assertThat(join("a.", "..b"), is("a..b"));
    }

    @Test
    void concatenates_when_the_root_path_ends_with_a_dot_or_the_key_starts_with_an_operator() {
        assertThat(join("a.", "b"), is("a.b"));
        assertThat(join("a", ".b"), is("a.b"));
        assertThat(join("a", "?.b"), is("a?.b"));
        assertThat(join("a", "[0]"), is("a[0]"));
        assertThat(join("a", "?[0]"), is("a?[0]"));
        assertThat(join("a", "*.b"), is("a*.b"));
        assertThat(join("a.", "[0]"), is("a.[0]"));
    }
}
