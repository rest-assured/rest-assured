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
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NonMultiPartUploadITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void uploadingWorksForByteArraysWithPost() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                contentType(ContentType.BINARY).
                body(bytes).
        when().
                post("/file").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void uploadingWorksForByteArraysWithPut() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                body(bytes).
        when().
                put("/file").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void uploadingWorksForByteArraysWithoutExplicitContentType() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                body(bytes).
        when().
                post("/file").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void uploadingWorksForFileWithPost() throws Exception {
        // Given
        final File file = new File(getClass().getResource("/car-records.xsd").toURI());

        // When
        given().
                contentType(ContentType.BINARY).
                body(file).
        when().
                post("/file").
        then().
                statusCode(200).
                body(equalTo(FileUtils.readFileToString(file)));
    }

    @Test
    public void uploadingWorksForFileWithPut() throws Exception {
        // Given
        final File file = new File(getClass().getResource("/car-records.xsd").toURI());

        // When
        given().
                contentType(ContentType.BINARY).
                body(file).
        when().
                put("/file").
        then().
                statusCode(200).
                body(equalTo(FileUtils.readFileToString(file)));
    }

    @Test
    public void uploadingWorksForInputStreamWithPost() throws Exception {
        // Given
        InputStream inputStream = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                contentType(ContentType.BINARY).
                body(inputStream).
        when().
                post("/file").
        then().
                statusCode(200).
                body(equalTo(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"))));
    }

    @Test
    public void uploadingWorksForInputStreamWithPut() throws Exception {
        // Given
        InputStream inputStream = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                contentType(ContentType.BINARY).
                body(inputStream).
        when().
                put("/file").
        then().
                statusCode(200).
                body(equalTo(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"))));
    }

    @Test
    public void uploadingWorksForFileWithPostAndBody() throws Exception {
        // Given
        final File file = new File(getClass().getResource("/car-records.xsd").toURI());

        // When
        given().
                contentType(ContentType.BINARY).
                body(file).
        when().
                post("/file").
        then().
                statusCode(200).
                body(equalTo(FileUtils.readFileToString(file)));
    }
}
