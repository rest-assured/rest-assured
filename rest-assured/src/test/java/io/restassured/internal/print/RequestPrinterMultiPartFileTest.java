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

package io.restassured.internal.print;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.LogConfig;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a {@link File} multipart is logged (issue #1819): the content of a text file is logged, a binary file is logged
 * as its path.
 */
class RequestPrinterMultiPartFileTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0x0d, 'I', 'H', 'D', 'R', (byte) 0xff, (byte) 0xd8, 0x01, 0x02};

    private static final Filter DONT_SEND = (req, res, ctx) -> new ResponseBuilder().setStatusCode(200).setBody("").build();

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Test
    void binary_file_part_with_default_mime_type_is_logged_as_its_path() throws IOException {
        File file = file("image.png", PNG_BYTES);

        logParams().multiPart("file", file).post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = image.png", "application/octet-stream", file.toString()));
    }

    @Test
    void binary_file_part_with_octet_stream_mime_type_is_logged_as_its_path() throws IOException {
        File file = file("data.bin", PNG_BYTES);

        logParams().multiPart("file", file, "application/octet-stream").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.bin", "application/octet-stream", file.toString()));
    }

    @Test
    void image_file_part_is_logged_as_its_path() throws IOException {
        File file = file("image.png", PNG_BYTES);

        logParams().multiPart("file", file, "image/png").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = image.png", "image/png", file.toString()))
                .doesNotContain("PNG").doesNotContain("IHDR");
    }

    @Test
    void json_file_part_is_logged_prettified() throws IOException {
        File file = file("data.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));

        logParams().multiPart("file", file, "application/json").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.json", "application/json", "{%n\t\t\t\t    \"a\": 1%n\t\t\t\t}"));
    }

    @Test
    void xml_file_part_is_logged_prettified() throws IOException {
        File file = file("data.xml", "<a><b>1</b></a>".getBytes(StandardCharsets.UTF_8));

        logParams().multiPart("file", file, "application/vnd.x+xml").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.xml", "application/vnd.x+xml", "<a>%n\t\t\t\t  <b>1</b>%n\t\t\t\t</a>"));
    }

    @Test
    void text_file_part_is_logged_with_its_content() throws IOException {
        File file = file("data.csv", "a;b,1;2".getBytes(StandardCharsets.UTF_8));

        logParams().multiPart("file", file, "text/csv").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.csv", "text/csv", "a;b,1;2"));
    }

    @Test
    void text_file_part_is_read_with_the_charset_of_the_part() throws IOException {
        File file = file("data.txt", "Grüße".getBytes(StandardCharsets.ISO_8859_1));

        logParams().multiPart(new MultiPartSpecBuilder(file).controlName("file").fileName("data.txt").mimeType("text/plain").charset(StandardCharsets.ISO_8859_1).build()).post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.txt", "text/plain", "Grüße"));
    }

    @Test
    void text_file_part_is_read_with_the_charset_of_the_mime_type() throws IOException {
        File file = file("data.txt", "Grüße".getBytes(StandardCharsets.ISO_8859_1));

        logParams().multiPart("file", file, "text/plain; charset=ISO-8859-1").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.txt", "text/plain; charset=ISO-8859-1", "Grüße"));
    }

    @Test
    void file_part_with_wildcard_mime_type_is_logged_as_its_path() throws IOException {
        File file = file("data.bin", PNG_BYTES);

        logParams().multiPart("file", file, "*/*").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.bin", "*/*", file.toString()));
    }

    @Test
    void text_file_part_without_charset_is_read_as_utf8() throws IOException {
        File file = file("data.txt", "Grüße".getBytes(StandardCharsets.UTF_8));

        logParams().multiPart("file", file, "text/plain").post("/upload");

        assertThat(log()).isEqualTo(multiPartLog("name = file; filename = data.txt", "text/plain", "Grüße"));
    }

    private RequestSpecification logParams() {
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
        return given().config(config().logConfig(new LogConfig(stream, true))).log().params().filter(DONT_SEND);
    }

    private String log() {
        String log = out.toString(StandardCharsets.UTF_8);
        return log.substring(log.indexOf("Multiparts:"));
    }

    private File file(String name, byte[] content) throws IOException {
        return Files.write(tempDir.resolve(name), content).toFile();
    }

    private static String multiPartLog(String disposition, String mimeType, String content) {
        return ("Multiparts:\t\t------------%n" +
                "\t\t\t\tContent-Disposition: form-data; " + disposition + "%n" +
                "\t\t\t\tContent-Type: " + mimeType + "%n" +
                "%n" +
                "\t\t\t\t" + content + "%n").replace("%n", System.lineSeparator());
    }
}
