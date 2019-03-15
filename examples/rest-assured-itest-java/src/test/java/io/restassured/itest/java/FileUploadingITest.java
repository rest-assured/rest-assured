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

package io.restassured.itest.java;

import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class FileUploadingITest extends WithJetty {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test public void
    can_upload_json_from_file() throws IOException {
        // Given
        File file = folder.newFile("my.json");
        FileUtils.writeStringToFile(file, "{ \"message\" : \"hello world\"}");

        // When
        given().
                contentType(ContentType.JSON).
                body(file).
        when().
                post("/jsonBody").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    can_upload_xml_from_file() throws IOException {
        // Given
        File file = folder.newFile("my.xml");
        FileUtils.writeStringToFile(file, "<tag attr='value'>");

        // When
        given().
                contentType(ContentType.XML).
                body(file).
        when().
                post("/validateContentTypeIsDefinedAndReturnBody").
        then().
                statusCode(200).
                contentType(ContentType.XML).
                body(equalTo("<tag attr='value'>"));
    }

    @Test public void
    can_upload_text_from_file() throws IOException {
        // Given
        File file = folder.newFile("my.txt");
        FileUtils.writeStringToFile(file, "Hello World");

        // When
        given().
                contentType(ContentType.TEXT).
                body(file).
        when().
                post("/reflect").
        then().
                statusCode(200).
                body(equalTo("Hello World"));
    }

    @Test public void
    can_upload_binary_from_file() throws IOException {
        // Given
        File file = folder.newFile("my.txt");
        FileUtils.writeStringToFile(file, "Hello World");

        // When
        given().
                contentType(ContentType.BINARY).
                body(file).
        when().
                post("/reflect").
        then().
                statusCode(200).
                body(equalTo("Hello World"));
    }

    @Test public void
    can_upload_file_with_custom_content_type() throws IOException {
        // Given
        File file = folder.newFile("my.txt");
        FileUtils.writeStringToFile(file, "Hello World");

        // When
        given().
                contentType("application/something").
                body(file).
        when().
                post("/reflect").
        then().
                statusCode(200).
                body(equalTo("Hello World"));
    }
}
