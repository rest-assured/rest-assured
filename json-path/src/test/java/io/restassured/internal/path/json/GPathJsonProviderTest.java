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

package io.restassured.internal.path.json;

import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;
import io.restassured.path.json.exception.JsonPathException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GPathJsonProviderTest {

    private final GPathJsonProvider provider = new GPathJsonProvider();

    private static PathSettings settings(NumberReturnType numberReturnType, int numberLengthLimit) {
        return new PathSettings(numberReturnType, numberLengthLimit, false, true, false, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private Object evaluate(PathInput input, PathSettings settings, String path) throws Exception {
        PathResult result = provider.evaluate(provider.parse(input, DocumentType.JSON, settings), DocumentType.JSON, path);
        assertThat(result.kind()).isEqualTo(PathResult.Kind.VALUE);
        return result.value();
    }

    @Test
    void describes_itself() {
        assertThat(provider.artifactId()).isEqualTo("json-path");
        assertThat(provider.engineName()).isEqualTo("GPATH");
        assertThat(provider.documentTypes()).containsExactly(DocumentType.JSON);
        assertThat(provider.description(DocumentType.JSON)).isEqualTo("JSON path");
    }

    @Test
    void evaluates_paths_on_text_and_on_a_stream_in_its_charset() throws Exception {
        PathSettings settings = settings(NumberReturnType.FLOAT_AND_DOUBLE, 1000);
        String json = "{\"a\":{\"b\":\"åäö\"}}";

        assertThat(evaluate(PathInput.ofText(json), settings, "a.b")).isEqualTo("åäö");
        assertThat(evaluate(PathInput.ofStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.ISO_8859_1)), "ISO-8859-1"), settings, "a.b")).isEqualTo("åäö");
        assertThat(evaluate(PathInput.ofText(json), settings, "$")).isInstanceOf(Map.class);
        assertThat(evaluate(PathInput.ofText(json), settings, "a.missing")).isNull();
    }

    @Test
    void parses_numbers_with_the_settings() throws Exception {
        assertThat(evaluate(PathInput.ofText("{\"n\":1.50}"), settings(NumberReturnType.BIG_DECIMAL, 1000), "n")).isEqualTo(new BigDecimal("1.50"));
        assertThatThrownBy(() -> provider.parse(PathInput.ofText("{\"n\":12345}"), DocumentType.JSON, settings(NumberReturnType.FLOAT_AND_DOUBLE, 3)))
                .hasMessageContaining("exceeds the maximum allowed length of 3");
    }

    @Test
    void parse_failure_is_a_json_path_exception() {
        Exception cause = new IllegalArgumentException("x");

        assertThat(provider.parseFailure(DocumentType.JSON, cause))
                .isExactlyInstanceOf(JsonPathException.class).hasMessage("Failed to parse the JSON document").hasCause(cause);
    }

    @Test
    void joins_root_paths_like_gpath() {
        assertThat(provider.joinRootPath("a.b", "c")).isEqualTo("a.b.c");
        assertThat(provider.joinRootPath("a", "[0]")).isEqualTo("a[0]");
        assertThat(provider.joinRootPath(null, "c")).isEqualTo("c");
    }
}
