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

package com.jayway.restassured.itest.java.presentation;

import com.jayway.restassured.builder.MultiPartSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
}
