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

package io.restassured.internal.multipart;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MultiPartSpecificationImplTest {

    @Test
    void setters_and_getters() {
        Map<String, String> headers = new TreeMap<>();
        headers.put("a", "b");
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setContent("content");
        spec.setControlName("ctrl");
        spec.setMimeType("text/plain");
        spec.setCharset("UTF-8");
        spec.setFileName("file.txt");
        spec.setControlNameSpecifiedExplicitly(true);
        spec.setFileNameSpecifiedExplicitly(true);
        spec.setHeaders(headers);

        assertThat(spec.getContent()).isEqualTo("content");
        assertThat(spec.getControlName()).isEqualTo("ctrl");
        assertThat(spec.getMimeType()).isEqualTo("text/plain");
        assertThat(spec.getCharset()).isEqualTo("UTF-8");
        assertThat(spec.getFileName()).isEqualTo("file.txt");
        assertThat(spec.hasFileName()).isTrue();
        assertThat(spec.isControlNameSpecifiedExplicitly()).isTrue();
        assertThat(spec.getControlNameSpecifiedExplicitly()).isTrue();
        assertThat(spec.isFileNameSpecifiedExplicitly()).isTrue();
        assertThat(spec.getFileNameSpecifiedExplicitly()).isTrue();
        assertThat(spec.getHeaders()).isEqualTo(headers);
    }

    @Test
    void defaults() {
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();

        assertThat(spec.getContent()).isNull();
        assertThat(spec.getControlName()).isNull();
        assertThat(spec.getMimeType()).isNull();
        assertThat(spec.getCharset()).isNull();
        assertThat(spec.getFileName()).isNull();
        assertThat(spec.hasFileName()).isFalse();
        assertThat(spec.isControlNameSpecifiedExplicitly()).isFalse();
        assertThat(spec.isFileNameSpecifiedExplicitly()).isFalse();
    }

    @Test
    void headers_are_unmodifiable_view() {
        Map<String, String> headers = new LinkedHashMap<>();
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setHeaders(headers);
        headers.put("a", "b");

        assertThat(spec.getHeaders()).containsEntry("a", "b");
        assertThat(catchThrowable(() -> spec.getHeaders().put("c", "d"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void get_headers_throws_npe_when_headers_are_null() {
        assertThat(catchThrowable(() -> new MultiPartSpecificationImpl().getHeaders())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void file_name_is_trimmed_to_null() {
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();

        spec.setFileName("  file.txt ");
        assertThat(spec.getFileName()).isEqualTo("file.txt");
        spec.setFileName("   ");
        assertThat(spec.getFileName()).isNull();
        assertThat(spec.hasFileName()).isFalse();
        spec.setFileName("");
        assertThat(spec.getFileName()).isNull();
    }

    @Test
    void to_string_with_everything_unset() {
        assertThat(new MultiPartSpecificationImpl().toString())
                .isEqualTo("controlName=<none>, mimeType=<none>, charset=<none>, fileName=<none>, content=null, headers=null");
    }

    @Test
    void to_string_treats_empty_strings_as_none() {
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setControlName("");
        spec.setMimeType("");
        spec.setCharset("");
        spec.setContent("");
        spec.setHeaders(new LinkedHashMap<>());

        assertThat(spec.toString()).isEqualTo("controlName=<none>, mimeType=<none>, charset=<none>, fileName=<none>, content=, headers=[:]");
    }

    @Test
    void to_string_with_everything_set() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("a", "b");
        headers.put("c", "d");
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setControlName("ctrl");
        spec.setMimeType("text/plain");
        spec.setCharset("UTF-8");
        spec.setFileName("file.txt");
        spec.setContent("content");
        spec.setHeaders(headers);

        assertThat(spec.toString()).isEqualTo("controlName=ctrl, mimeType=text/plain, charset=UTF-8, fileName=file.txt, content=content, headers=[a:b, c:d]");
    }

    @Test
    void to_string_renders_content_the_groovy_way() {
        assertThat(withContent(new ByteArrayInputStream(new byte[]{1}))).endsWith("content=<inputstream>, headers=null");
        assertThat(withContent(new byte[]{65, 66})).endsWith("content=[65, 66], headers=null");
        assertThat(withContent(new char[]{'h', 'i'})).endsWith("content=hi, headers=null");
        assertThat(withContent(new Object[]{"a", null})).endsWith("content=[a, null], headers=null");
        assertThat(withContent(Arrays.asList("a", 1))).endsWith("content=[a, 1], headers=null");
        assertThat(withContent(Collections.singletonMap("a", 1))).endsWith("content=[a:1], headers=null");
        assertThat(withContent(new File("dir", "file.txt"))).endsWith("content=" + new File("dir", "file.txt") + ", headers=null");
        assertThat(withContent(42)).endsWith("content=42, headers=null");
    }

    @Test
    void to_string_reads_reader_content() {
        StringReader reader = new StringReader("from reader");

        assertThat(withContent(reader)).endsWith("content=from reader, headers=null");
        assertThat(catchThrowable(reader::read)).hasMessage("Stream closed");
    }

    private static String withContent(Object content) {
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setContent(content);
        return spec.toString();
    }
}
