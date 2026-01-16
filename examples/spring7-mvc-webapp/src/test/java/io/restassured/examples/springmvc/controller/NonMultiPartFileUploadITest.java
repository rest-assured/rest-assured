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

import io.restassured.examples.springmvc.config.MainConfiguration;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
// @formatter:off
public class NonMultiPartFileUploadITest {

    @Autowired
    protected WebApplicationContext wac;

    @TempDir
    Path folder;

    @BeforeEach
    public void configureMockMvcInstance() {
        RestAssuredMockMvc.postProcessors(csrf().asHeader());
        RestAssuredMockMvc.webAppContextSetup(wac);
    }

    @AfterEach
    public void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    file_uploading_works() throws IOException {
        Path tempFile = Files.createFile(folder.resolve("something"));
        Files.write(tempFile, "Something21".getBytes(StandardCharsets.UTF_8));
        File file = tempFile.toFile();

        RestAssuredMockMvc.given().
                contentType(ContentType.BINARY).
                body(file).
        when().
                post("/nonMultipartFileUpload").
        then().
                statusCode(200).
                body("size", greaterThan(10)).
                body("content", equalTo("Something21"));
    }
}
// @formatter:on
