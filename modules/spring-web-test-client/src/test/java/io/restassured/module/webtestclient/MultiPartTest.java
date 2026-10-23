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

package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.setup.MultiPartController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig.newConfig;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;

public class MultiPartTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    public static void configureWebTestClientInstance() {
        RestAssuredWebTestClient.webTestClient(WebTestClient.bindToController(new MultiPartController()).build());
    }

    @AfterAll
    public static void restRestAssured() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    public void
    each_part_is_sent_with_its_control_name_file_name_content_type_and_content() throws IOException {
        File file = Files.writeString(tempDir.resolve("upload.txt"), "file content", UTF_8).toFile();

        given()
                .multiPart("text", "åäö")
                .multiPart("json", Map.of("a", 1), "application/json")
                .multiPart("data", "data.bin", "bytes".getBytes(UTF_8))
                .multiPart("pdf", "data.pdf", "pdf".getBytes(UTF_8), "application/pdf")
                .multiPart("doc", file)
                .multiPart("stream", "stream.csv", new ByteArrayInputStream("a,b".getBytes(UTF_8)), "text/csv")
                .when()
                .post("/multipart")
                .then()
                .statusCode(200)
                .body(equalTo(String.join("\n",
                        "text | null | text/plain | " + utf8("åäö"),
                        "json | null | application/json | {\"a\":1}",
                        "data | data.bin | application/octet-stream | bytes",
                        "pdf | data.pdf | application/pdf | pdf",
                        "doc | upload.txt | application/octet-stream | file content",
                        "stream | stream.csv | text/csv | a,b")));
    }

    @Test
    public void
    string_part_without_charset_is_encoded_in_the_configured_default_charset() {
        // Not text/plain, which Spring decodes as a form field (as UTF-8 without a charset) instead of passing on its bytes
        given()
                .config(newConfig().multiPartConfig(multiPartConfig().defaultCharset(ISO_8859_1)))
                .multiPart("text", "åäö", "text/csv")
                .when()
                .post("/multipart")
                .then()
                .statusCode(200)
                .body(equalTo("text | null | text/csv | åäö"));
    }

    /**
     * @return the bytes of the text encoded as UTF-8, one char per byte, as {@link MultiPartController} returns them
     */
    private static String utf8(String text) {
        return new String(text.getBytes(UTF_8), ISO_8859_1);
    }
}
