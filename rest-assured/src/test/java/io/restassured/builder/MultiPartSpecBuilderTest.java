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

package io.restassured.builder;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiPartSpecBuilderTest {

    @Test
    public void charset_Charset_rejects_byte_array_content() {
        assertThatThrownBy(() ->
                new MultiPartSpecBuilder(new byte[]{1}).charset(StandardCharsets.UTF_8)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("charset");
    }

    @Test
    public void charset_Charset_rejects_input_stream_content() {
        assertThatThrownBy(() ->
                new MultiPartSpecBuilder(new ByteArrayInputStream(new byte[]{1})).charset(StandardCharsets.UTF_8)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("charset");
    }

    @Test
    public void charset_String_rejects_byte_array_content() {
        assertThatThrownBy(() ->
                new MultiPartSpecBuilder(new byte[]{1}).charset("UTF-8")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("charset");
    }
}
