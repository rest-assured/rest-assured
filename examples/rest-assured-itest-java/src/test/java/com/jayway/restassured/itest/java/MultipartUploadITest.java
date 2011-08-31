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

import com.jayway.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.authentication.FormAuthConfig.springSecurity;

public class MultipartUploadITest {

    @Test
    @Ignore
    public void test() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        RestAssured.port = 9090;

        // When
        given().
                auth().form("admin", "admin", springSecurity()).
                multiPart("file", new File("/home/johan/Downloads/verizon.rss")).
        expect().
                log().
                statusCode(200).
        when().
                post("/backend/file/upload");
    }
}
