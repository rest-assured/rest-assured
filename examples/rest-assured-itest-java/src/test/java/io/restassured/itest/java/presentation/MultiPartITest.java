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

package io.restassured.itest.java.presentation;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static org.hamcrest.Matchers.*;

public class MultiPartITest extends WithJetty {

    @Test
    @Ignore("Only used for presentation purposes")
    public void simpleFileUploading() throws Exception {
        // When
        given().
                multiPart(new File("/home/johan/devtools/java/projects/rest-assured/test.txt")).
        expect().
                body(equalTo("I'm a test file")).
        when().
                post("/multipart/file");
    }

    @Test
    public void byteArrayUploading() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes))).
        when().
                post("/multipart/file");
    }

    @Test
    public void byteArrayUploadingWhenUsingMultiPartSpecification() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart(new MultiPartSpecBuilder(bytes).build()).
        expect().
                statusCode(200).
                body(is(new String(bytes))).
        when().
                post("/multipart/file");
    }


    @Test
    public void textUploadingWhenUsingMultiPartSpecificationAndCharset() throws Exception {
        // Given
        final String string = IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart(new MultiPartSpecBuilder(string).with().charset("UTF-8").and().with().controlName("other").
                        and().with().mimeType("application/vnd.some+json").build()).
        expect().
                statusCode(200).
                body(is(string)).
        when().
                post("/multipart/string");
    }

    @Test
    public void textUploadingWhenUsingMultiPartSpecificationAndCharsetAndHeaders() throws Exception {
        // Given
        final String string = IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart(new MultiPartSpecBuilder(string).with().charset("UTF-8").and().with().controlName("other").
                        and().with().mimeType("application/vnd.some+json").
                        and().with().header("X-Header-1", "Value1").
                        and().with().header("X-Header-2", "Value2").build()).
        expect().
                statusCode(200).
                body(is(string)).
        when().
                post("/multipart/string");
    }

    @Test
    public void inputStreamUploadingUsingMultiPartSpecification() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(is).with().and().with().controlName("file").
                        and().with().mimeType("application/vnd.some+json").and().with().fileName("my-file").build()).
        expect().
                statusCode(200).
                body(is(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd")))).
        when().
                post("/multipart/file");
    }

    @Test
    public void controlNameInMultiPartSpecBuilderHasPrecedenceOverDefault() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                queryParam("controlName", "file2").
                multiPart(new MultiPartSpecBuilder(is).controlName("file2").build()).
        when().
                post("/multipart/file").
        then().
                statusCode(200).
                body(is(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"))));
    }

    @Test
    public void controlNameInMultiPartSpecBuilderHasPrecedenceOverDefaultWhenConfigured() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultControlName("something-else"))).
                queryParam("controlName", "file2").
                multiPart(new MultiPartSpecBuilder(is).controlName("file2").build()).
        when().
                post("/multipart/file").
        then().
                statusCode(200).
                body(is(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"))));
    }

    @Test
    public void defaultControlNameIsUsedWhenNoControlNameIsDefinedInMultiPartSpecBuilder() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                queryParam("controlName", "file2").
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultControlName("file2"))).
                multiPart(new MultiPartSpecBuilder(is).build()).
        when().
                post("/multipart/file").
        then().
                statusCode(200).
                body(is(IOUtils.toString(getClass().getResourceAsStream("/car-records.xsd"))));
    }

    @Test
    public void fileNameInMultiPartSpecBuilderHasPrecedenceOverDefault() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(is).fileName("file2").build()).
        when().
                post("/multipart/filename").
        then().
                statusCode(200).
                body(equalTo("file2"));
    }

    @Test
    public void fileNameInMultiPartSpecBuilderHasPrecedenceOverDefaultWhenConfigured() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultFileName("something-else"))).
                multiPart(new MultiPartSpecBuilder(is).fileName("file2.txt").build()).
        when().
                post("/multipart/filename").
        then().
                statusCode(200).
                body(equalTo("file2.txt"));;
    }

    @Test
    public void defaultFileNameIsUsedWhenNoFileNameIsDefinedInMultiPartSpecBuilder() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("file-2.txt"))).
                multiPart(new MultiPartSpecBuilder(is).build()).
        when().
                post("/multipart/filename").
        then().
                statusCode(200).
                body(equalTo("file-2.txt"));
    }

    @Test
    public void defaultFileNameCanBeNull() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().with().emptyDefaultFileName())).
                multiPart(new MultiPartSpecBuilder(is).build()).
        when().
                post("/multipart/filename").
        then().
                body(emptyString()).
                statusCode(200);
    }

    @Test
    public void fileNameCanBeNullInMultiPartSpec() throws Exception {
        // Given
        final InputStream is = getClass().getResourceAsStream("/car-records.xsd");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(is).with().emptyFileName().build()).
        when().
                post("/multipart/filename").
        then().
                body(emptyString()).
                statusCode(200);
    }

    @Test @Ignore("For some reason this tests fails occasionally at Travis")
    public void returnsErrorWhenMultipartPatchReturnsFailureStatusCode() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        when().
                patch("/multipart/file400").
        then().
                statusCode(400).
                body("error", equalTo("message"));
    }

    @Test
    public void returnsErrorWhenMultipartPostReturnsFailureStatusCode() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        when().
                post("/multipart/file400").
        then().
                statusCode(400).
                body("error", equalTo("message"));
    }

    @Test
    public void returnsErrorWhenMultipartPutReturnsFailureStatusCode() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        when().
                put("/multipart/file400").
        then().
                statusCode(400).
                body("error", equalTo("message"));
    }

    @Test
    public void usesDefaultBoundaryFromMultipartConfigWhenNoBoundaryIsDefinedInContentType() throws Exception {
        // When
        given().
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultBoundary("abcdef"))).
                multiPart("file", "myFile", "content".getBytes("UTF-8")).
        when().
                post("/headersWithValues").
        then().
                statusCode(200).
                body("Content-Type", contains(endsWith("boundary=abcdef")));
    }

    @Test
    public void usesBoundaryFromContentTypeWhenDefaultBoundaryIsDefinedInMultipartConfig() throws Exception {
        // When
        given().
                contentType("multipart/mixed; boundary=johndoe").
                config(RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultBoundary("abcdef"))).
                multiPart("file", "myFile", "content".getBytes("UTF-8")).
        when().
                post("/headersWithValues").
        then().
                statusCode(200).
                body("Content-Type", contains("multipart/mixed; boundary=johndoe"));
    }

    @Test
    public void usesBoundaryFromContentTypeWhenNoDefaultBoundaryIsDefinedInMultipartConfig() throws Exception {
        // When
        given().
                contentType("multipart/mixed; boundary=johndoe").
                multiPart("file", "myFile", "content".getBytes("UTF-8")).
        when().
                post("/headersWithValues").
        then().
                statusCode(200).
                body("Content-Type", contains("multipart/mixed; boundary=johndoe"));
    }

    @Test
    public void allowPassingMultiPartsWithContentTypeContainingMultiPartPlusSubtype() throws Exception {
        // When
        given().
                contentType("application/x-hub-multipart+xml; boundary=johndoe").
                multiPart("file", "myFile", "content".getBytes("UTF-8")).
        when().
                post("/headersWithValues").
        then().
                statusCode(200).
                body("Content-Type", contains("application/x-hub-multipart+xml; boundary=johndoe"));
    }
}
