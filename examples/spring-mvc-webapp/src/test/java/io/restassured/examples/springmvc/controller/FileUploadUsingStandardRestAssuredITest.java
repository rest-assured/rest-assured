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

package io.restassured.examples.springmvc.controller;

import io.restassured.examples.springmvc.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class FileUploadUsingStandardRestAssuredITest extends WithJetty {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test public void
    file_uploading_works_using_standard_rest_assured() throws IOException {
        File something = folder.newFile("something");
        IOUtils.write("Something21", new FileOutputStream(something));

        given().
                multiPart(something).
        when().
                post("/fileUpload").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("file"));
    }

    @Test public void
    file_uploading_works_using_standard_rest_assured2() {
        given().
                multiPart("controlName", "fileName", new ByteArrayInputStream("Something21".getBytes())).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("fileName"));
    }
}
