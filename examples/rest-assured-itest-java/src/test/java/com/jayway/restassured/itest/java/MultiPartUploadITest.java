/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class MultiPartUploadITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void multiPartUploadingWorksForByteArrays() throws Exception {
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
   public void multiPartUploadingWorksForStrings() throws Exception {
       // When
       given().
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
       when().
               post("/multipart/text");
    }

   @Test
   public void multiPartUploadingWorksForMultipleStrings() throws Exception {
       // When
       given().
               multiPart("text", "Some text").
               multiPart("text", "Some other text").
       expect().
               statusCode(200).
               body(is("Some text,Some other text")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndStrings() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                multiPart("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndFormParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                formParam("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForFormParamsAndByteArray() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                formParam("text", "Some text").
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                param("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForParamsAndByteArray() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                param("text", "Some text").
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void bytesAndFormParamUploadingWorkUsingRequestBuilder() throws Exception {
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));
        final RequestSpecification spec = new RequestSpecBuilder().addMultiPart("file", "myFile", bytes).addFormParam("text", "Some text").build();

        // When
        given().
                spec(spec).
        expect().
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");

    }

    @Test
    public void multiPartUploadingDoesntWorkForPut() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Sorry, multi part form data is only available for POST");

        given().
                multiPart("text", "sometext").
        when().
                put("/multipart/file");
    }
}
