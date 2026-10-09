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

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RestAssuredMultiPartEntityTest {

    @Test
    void sub_type_is_required() {
        Throwable t = catchThrowable(() -> new RestAssuredMultiPartEntity(null, null, HttpMultipartMode.STRICT, "b"));

        assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessage("Multipart sub type cannot be null");
    }

    @Test
    void content_type_has_trimmed_sub_type_boundary_and_trimmed_charset() {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity(" mixed ", " UTF-16 ", HttpMultipartMode.STRICT, "BOUNDARY");

        assertThat(entity.getContentType().getName()).isEqualTo("Content-Type");
        assertThat(entity.getContentType().getValue()).isEqualTo("multipart/mixed; boundary=BOUNDARY; charset=UTF-16");
        assertThat(entity.getContentEncoding()).isNull();
    }

    @Test
    void content_type_without_charset() {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", null, HttpMultipartMode.BROWSER_COMPATIBLE, "BOUNDARY");

        assertThat(entity.getContentType().getValue()).isEqualTo("multipart/form-data; boundary=BOUNDARY");
    }

    @Test
    void writes_all_parts() throws IOException {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", "UTF-8", HttpMultipartMode.STRICT, "BOUNDARY");
        entity.addPart("one", new StringBody("first", ContentType.TEXT_PLAIN));
        entity.addPart(FormBodyPartBuilder.create("two", new StringBody("second", ContentType.APPLICATION_JSON)).addField("X-A", "a").build());

        String written = written(entity);

        assertThat(written).isEqualTo("--BOUNDARY\r\n" +
                "Content-Disposition: form-data; name=\"one\"\r\n" +
                "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "\r\n" +
                "first\r\n" +
                "--BOUNDARY\r\n" +
                "X-A: a\r\n" +
                "Content-Disposition: form-data; name=\"two\"\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "\r\n" +
                "second\r\n" +
                "--BOUNDARY--\r\n");
        assertThat(entity.getContentLength()).isEqualTo(written.length());
        assertThat(entity.isRepeatable()).isTrue();
        assertThat(entity.isStreaming()).isFalse();
        assertThat(entity.isChunked()).isFalse();
    }

    @Test
    void adding_a_part_rebuilds_the_entity() throws IOException {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", null, HttpMultipartMode.STRICT, "BOUNDARY");
        entity.addPart("one", new StringBody("first", ContentType.TEXT_PLAIN));
        long lengthWithOnePart = entity.getContentLength();

        entity.addPart("two", new StringBody("second", ContentType.TEXT_PLAIN));

        assertThat(entity.getContentLength()).isGreaterThan(lengthWithOnePart).isEqualTo(written(entity).length());
    }

    @Test
    void input_stream_parts_make_the_entity_streaming_and_of_unknown_length() throws IOException {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", null, HttpMultipartMode.STRICT, "BOUNDARY");
        entity.addPart("one", new InputStreamBody(new ByteArrayInputStream(new byte[]{65}), "file.bin"));

        assertThat(entity.isRepeatable()).isFalse();
        assertThat(entity.isStreaming()).isTrue();
        assertThat(entity.isChunked()).isTrue();
        assertThat(entity.getContentLength()).isEqualTo(-1);
        assertThat(catchThrowable(entity::consumeContent)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Streaming entity does not implement #consumeContent()");
    }

    @Test
    void consume_content_does_nothing_for_repeatable_entity() throws IOException {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", null, HttpMultipartMode.STRICT, "BOUNDARY");
        entity.addPart("one", new StringBody("first", ContentType.TEXT_PLAIN));

        entity.consumeContent();
    }

    @Test
    void get_content_is_not_supported() {
        RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity("form-data", null, HttpMultipartMode.STRICT, "BOUNDARY");

        assertThat(catchThrowable(entity::getContent)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Multipart form entity does not implement #getContent()");
    }

    private static String written(RestAssuredMultiPartEntity entity) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}
