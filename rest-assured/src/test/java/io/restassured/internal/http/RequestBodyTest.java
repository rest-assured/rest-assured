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

package io.restassured.internal.http;

import io.restassured.common.spi.http.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RequestBodyTest {

    @TempDir
    Path tmpDir;

    @Test
    void bytes_body_is_repeatable_and_has_the_length_of_the_bytes() {
        byte[] bytes = {1, 2, 3};

        RequestBody body = RequestBody.bytes(bytes, "application/octet-stream");

        assertThat(body.getType()).isEqualTo(RequestBody.Type.BYTES);
        assertThat(body.getBytes()).isSameAs(bytes);
        assertThat(body.getContentType()).isEqualTo("application/octet-stream");
        assertThat(body.getContentLength()).isEqualTo(3);
        assertThat(body.isRepeatable()).isTrue();
    }

    @Test
    void stream_body_is_not_repeatable_and_keeps_an_unknown_length() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1});

        RequestBody body = RequestBody.stream(stream, RequestBody.UNKNOWN_LENGTH, null);

        assertThat(body.getType()).isEqualTo(RequestBody.Type.STREAM);
        assertThat(body.getStream()).isSameAs(stream);
        assertThat(body.getContentType()).isNull();
        assertThat(body.getContentLength()).isEqualTo(-1);
        assertThat(body.isRepeatable()).isFalse();
    }

    @Test
    void file_body_is_repeatable_and_has_the_length_of_the_file() throws Exception {
        File file = Files.write(tmpDir.resolve("body.txt"), new byte[]{1, 2}).toFile();

        RequestBody body = RequestBody.file(file, "text/plain");

        assertThat(body.getType()).isEqualTo(RequestBody.Type.FILE);
        assertThat(body.getFile()).isEqualTo(file);
        assertThat(body.getContentLength()).isEqualTo(2);
        assertThat(body.isRepeatable()).isTrue();
    }

    @Test
    void writer_body_has_the_given_length_and_repeatability() {
        RequestBody.ContentWriter writer = out -> out.write(1);

        RequestBody body = RequestBody.writer(writer, 1, false, "multipart/form-data; boundary=b");

        assertThat(body.getType()).isEqualTo(RequestBody.Type.WRITER);
        assertThat(body.getWriter()).isSameAs(writer);
        assertThat(body.getContentLength()).isEqualTo(1);
        assertThat(body.isRepeatable()).isFalse();
    }

    @Test
    void content_of_another_type_cant_be_read() {
        RequestBody body = RequestBody.bytes(new byte[0], null);

        Throwable t = catchThrowable(body::getStream);

        assertThat(t).isInstanceOf(IllegalStateException.class).hasMessage("The request body is of type BYTES, not STREAM.");
    }

    @Test
    void null_content_is_rejected() {
        assertThat(catchThrowable(() -> RequestBody.bytes(null, null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(catchThrowable(() -> RequestBody.stream(null, 0, null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(catchThrowable(() -> RequestBody.file(null, null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(catchThrowable(() -> RequestBody.writer(null, 0, true, null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bodies_with_equal_bytes_are_equal() {
        RequestBody body = RequestBody.bytes(new byte[]{1, 2}, "text/plain");

        assertThat(body).isEqualTo(RequestBody.bytes(new byte[]{1, 2}, "text/plain"))
                .hasSameHashCodeAs(RequestBody.bytes(new byte[]{1, 2}, "text/plain"))
                .isNotEqualTo(RequestBody.bytes(new byte[]{1, 3}, "text/plain"))
                .isNotEqualTo(RequestBody.bytes(new byte[]{1, 2}, "application/json"));
    }
}
