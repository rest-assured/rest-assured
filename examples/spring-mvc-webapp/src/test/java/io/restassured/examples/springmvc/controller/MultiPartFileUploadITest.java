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
import io.restassured.examples.springmvc.support.Greeting;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.path.json.JsonPath;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.restassured.RestAssured.withArgs;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
// @formatter:off
public class MultiPartFileUploadITest {

    @Autowired
    protected WebApplicationContext wac;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void configureMockMvcInstance() {
        RestAssuredMockMvc.webAppContextSetup(wac);
        RestAssuredMockMvc.postProcessors(csrf().asHeader());
    }

    @After
    public void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    file_uploading_works()throws IOException {
        File file = folder.newFile("something");
        IOUtils.write("Something21", new FileOutputStream(file));

        RestAssuredMockMvc.given().
                multiPart(file).
        when().
                post("/fileUpload").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("file"));
    }

    @Test public void
    input_stream_uploading_works()throws IOException {
        File file = folder.newFile("something");
        IOUtils.write("Something21", new FileOutputStream(file));

        RestAssuredMockMvc.given().
                multiPart("controlName", "original", new FileInputStream(file)).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original"));
    }

    @Test public void
    byte_array_uploading_works()throws IOException {
        RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes()).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original"));
    }

    @Test public void
    byte_array_uploading_works_with_mime_type()throws IOException {
        RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes(), "mime-type").
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("mime-type"));
    }

    @Test public void
    multiple_uploads_works()throws IOException {
        File file = folder.newFile("something");
        IOUtils.write("Something3210", new FileOutputStream(file));


        RestAssuredMockMvc.given().
                multiPart("controlName1", "original1", "something123".getBytes(), "mime-type1").
                multiPart("controlName2", "original2", new FileInputStream(file), "mime-type2").
        when().
                post("/multiFileUpload").
        then().
                root("[%d]").
                body("size", withArgs(0), is(12)).
                body("name", withArgs(0), equalTo("controlName1")).
                body("originalName", withArgs(0), equalTo("original1")).
                body("mimeType", withArgs(0), equalTo("mime-type1")).
                body("content", withArgs(0), equalTo("something123")).
                body("size", withArgs(1), is(13)).
                body("name", withArgs(1), equalTo("controlName2")).
                body("originalName", withArgs(1), equalTo("original2")).
                body("mimeType", withArgs(1), equalTo("mime-type2")).
                body("content", withArgs(1), equalTo("Something3210"));
    }

    @Test public void
    object_serialization_works() throws IOException {
        File file = folder.newFile("something");
        IOUtils.write("Something3210", new FileOutputStream(file));

        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        String content =
        RestAssuredMockMvc.given().
                multiPart("controlName1", file, "mime-type1").
                multiPart("controlName2", greeting, "application/json").
        when().
                post("/multiFileUpload").
        then().
                root("[%d]").
                body("size", withArgs(0), is(13)).
                body("name", withArgs(0), equalTo("controlName1")).
                body("originalName", withArgs(0), equalTo("something")).
                body("mimeType", withArgs(0), equalTo("mime-type1")).
                body("content", withArgs(0), equalTo("Something3210")).
                body("size", withArgs(1), greaterThan(10)).
                body("name", withArgs(1), equalTo("controlName2")).
                body("originalName", withArgs(1), equalTo("file")).
                body("mimeType", withArgs(1), equalTo("application/json")).
                body("content", withArgs(1), notNullValue()).
        extract().
                path("[1].content");

        JsonPath jsonPath = new JsonPath(content);

        assertThat(jsonPath.getString("firstName"), equalTo("John"));
        assertThat(jsonPath.getString("lastName"), equalTo("Doe"));
    }

    @Test public void
    file_upload_and_param_mixing_works() {
        RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes(), "mime-type").
                param("param", "paramValue").
        when().
                post("/fileUploadWithParam").
        then().
                root("file").
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("mime-type")).
                noRoot().
                body("param", equalTo("paramValue"));
    }

    @Test public void
    allows_settings_default_control_name_using_instance_configuration() throws IOException {
        File file = folder.newFile("filename.txt");
        IOUtils.write("Something21", new FileOutputStream(file));

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultControlName("something"))).
                multiPart(file).
        when().
                post("/fileUploadWithControlNameEqualToSomething").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("something")).
                body("originalName", equalTo("filename.txt"));
    }

    @Test public void
    allows_settings_default_control_name_using_static_configuration() throws IOException {
        File file = folder.newFile("filename.txt");
        IOUtils.write("Something21", new FileOutputStream(file));

        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultControlName("something"));

        RestAssuredMockMvc.given().
                multiPart(file).
        when().
                post("/fileUploadWithControlNameEqualToSomething").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("something")).
                body("originalName", equalTo("filename.txt"));
    }

    @Test public void
    allows_settings_default_file_name_using_instance_configuration() throws IOException {
        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("filename.txt"))).
                multiPart("controlName", "something32".getBytes()).
        when().
               post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("filename.txt"));
    }

    @Test public void
    allows_settings_default_file_name_using_static_configuration() throws IOException {
        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("filename.txt"));

        RestAssuredMockMvc.given().
                multiPart("controlName", "something32".getBytes()).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("filename.txt"));
    }

    @Test public void
    allows_sending_multipart_without_a_filename_when_default_file_name_is_empty() throws IOException {
        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().emptyDefaultFileName())).
                multiPart("controlName", "something32".getBytes()).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo(""));
    }

    @Test public void
    allows_sending_multipart_without_a_filename_when_default_file_name_is_set() throws IOException {
        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("custom"))).
                multiPart("controlName", null, "something32".getBytes()).
        when().
                post("/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo(""));
    }

    @Test public void
    multi_part_uploading_supports_specifying_default_subtype() throws Exception {
       // When
        File file = folder.newFile("filename.txt");
        IOUtils.write("Something21", new FileOutputStream(file));

       RestAssuredMockMvc.given().
               config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().defaultSubtype("mixed"))).
               multiPart("something", file).
       when().
               post("/textAndReturnHeader").
       then().
               statusCode(200).
               body("size", greaterThan(10),
                    "name", equalTo("something"),
                    "originalName", equalTo("filename.txt")).
               header("X-Request-Header", startsWith("multipart/mixed"));
    }

    @Test public void
    explicit_multi_part_content_type_has_precedence_over_default_subtype() throws Exception {
       // When
        File file = folder.newFile("filename.txt");
        IOUtils.write("Something21", new FileOutputStream(file));

       RestAssuredMockMvc.given().
               config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().defaultSubtype("form-data"))).
               contentType("multipart/mixed").
               multiPart("something", file).
       when().
               post("/textAndReturnHeader").
       then().
               statusCode(200).
               body("size", greaterThan(10),
                    "name", equalTo("something"),
                    "originalName", equalTo("filename.txt")).
               header("X-Request-Header", startsWith("multipart/mixed"));
    }
}
// @formatter:on