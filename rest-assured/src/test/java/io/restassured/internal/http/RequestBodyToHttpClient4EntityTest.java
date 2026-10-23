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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.RequestContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RequestBodyToHttpClient4EntityTest {

    @TempDir
    Path tmpDir;

    @Test
    void bytes_body_is_a_repeatable_entity_sent_with_a_content_length() throws Exception {
        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.bytes(new byte[]{'a', 'b'}, "text/plain"));

        assertThat(entity.isRepeatable()).isTrue();
        assertThat(entity.isStreaming()).isFalse();
        assertThat(entity.getContentType().getValue()).isEqualTo("text/plain");
        assertThat(entity.getContent()).hasBinaryContent(new byte[]{'a', 'b'});
        assertThat(entity.getContent()).hasBinaryContent(new byte[]{'a', 'b'});
        assertThat(headersSentWith(entity)).isEqualTo("Content-Length: 2, Content-Type: text/plain");
    }

    @Test
    void stream_body_of_known_length_is_a_non_repeatable_entity_sent_with_a_content_length() {
        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.stream(new ByteArrayInputStream(new byte[]{1}), 1, "application/octet-stream"));

        assertThat(entity.isRepeatable()).isFalse();
        assertThat(entity.isStreaming()).isTrue();
        assertThat(headersSentWith(entity)).isEqualTo("Content-Length: 1, Content-Type: application/octet-stream");
    }

    @Test
    void stream_body_of_unknown_length_is_sent_chunked() {
        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.stream(new ByteArrayInputStream(new byte[]{1}), RequestBody.UNKNOWN_LENGTH, "text/plain"));

        assertThat(headersSentWith(entity)).isEqualTo("Transfer-Encoding: chunked, Content-Type: text/plain");
    }

    @Test
    void body_without_content_type_is_sent_without_one() {
        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.bytes(new byte[]{1}, null));

        assertThat(entity.getContentType()).isNull();
        assertThat(headersSentWith(entity)).isEqualTo("Content-Length: 1");
    }

    @Test
    void file_body_is_streamed_once_with_the_length_of_the_file() throws Exception {
        File file = Files.write(tmpDir.resolve("body.txt"), new byte[]{'x', 'y', 'z'}).toFile();

        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.file(file, "text/plain"));

        assertThat(entity.isRepeatable()).isFalse();
        assertThat(entity.getContent()).hasBinaryContent(new byte[]{'x', 'y', 'z'});
        assertThat(headersSentWith(entity)).isEqualTo("Content-Length: 3, Content-Type: text/plain");
    }

    @Test
    void missing_file_throws_runtime_exception_naming_the_file() {
        File file = tmpDir.resolve("missing.txt").toFile();

        Throwable t = catchThrowable(() -> RequestBodyToHttpClient4Entity.toHttpEntity(RequestBody.file(file, null)));

        assertThat(t).isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("File " + file.getPath() + " not found")
                .hasCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    void writer_body_is_written_by_the_writer_and_has_its_length_and_repeatability() throws Exception {
        RequestBody body = RequestBody.writer(out -> out.write(new byte[]{'w'}), 1, true, "multipart/form-data; boundary=b");

        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(body);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);

        assertThat(out.toByteArray()).containsExactly('w');
        assertThat(entity.isRepeatable()).isTrue();
        assertThat(entity.isStreaming()).isFalse();
        assertThat(headersSentWith(entity)).isEqualTo("Content-Length: 1, Content-Type: multipart/form-data; boundary=b");
        assertThat(catchThrowable(entity::getContent)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void writer_body_of_unknown_length_is_not_repeatable_and_sent_chunked() {
        RequestBody body = RequestBody.writer(out -> out.write('w'), RequestBody.UNKNOWN_LENGTH, false, "multipart/form-data; boundary=b");

        HttpEntity entity = RequestBodyToHttpClient4Entity.toHttpEntity(body);

        assertThat(entity.isRepeatable()).isFalse();
        assertThat(entity.isStreaming()).isTrue();
        assertThat(headersSentWith(entity)).isEqualTo("Transfer-Encoding: chunked, Content-Type: multipart/form-data; boundary=b");
    }

    // The headers HttpClient 4 adds for the entity before sending the request
    private static String headersSentWith(HttpEntity entity) {
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/");
        request.setEntity(entity);
        try {
            new RequestContent().process(request, new BasicHttpContext());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return Arrays.stream(request.getAllHeaders()).map(Header::toString).collect(Collectors.joining(", "));
    }
}
