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
import io.restassured.http.Method;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static io.restassured.RestAssured.withArgs;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static io.restassured.http.Method.POST;
import static io.restassured.http.Method.PUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
public class MultiPartFileUploadITest {

    @Autowired
    protected WebApplicationContext wac;

    @org.junit.jupiter.api.io.TempDir
    Path folder;

    public static Stream<Method> methods() {
        return Stream.of(POST, PUT);
    }

    @BeforeEach
    public void configureMockMvcInstance() {
        RestAssuredMockMvc.webAppContextSetup(wac);
        RestAssuredMockMvc.postProcessors(csrf().asHeader());
    }

    @AfterEach
    public void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    private File createTempFile(String name, String content) throws IOException {
        Path file = Files.createFile(folder.resolve(name));
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    // @formatter:off
    @ParameterizedTest
    @MethodSource("methods")
    public void file_uploading_works(Method method) throws IOException {
        File file = createTempFile("something", "Something21");

        RestAssuredMockMvc.given().
                multiPart(file).
        when().
                request(method, "/fileUpload").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("file"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void input_stream_uploading_works(Method method) throws IOException {
        File file = createTempFile("something", "Something21");

        RestAssuredMockMvc.given().
                multiPart("controlName", "original", new FileInputStream(file)).
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("application/octet-stream"));

    }

    @ParameterizedTest
    @MethodSource("methods")
    public void byte_array_uploading_works(Method method) {
    	RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes()).
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("application/octet-stream"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void byte_array_uploading_works_with_mime_type(Method method) {
    	RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes(), "mime-type").
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("mime-type"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void multiple_uploads_works(Method method) throws IOException {
        File file = createTempFile("something", "Something3210");

        RestAssuredMockMvc.given().
                multiPart("controlName1", "original1", "something123".getBytes(), "mime-type1").
                multiPart("controlName2", "original2", new FileInputStream(file), "mime-type2").
        when().
                request(method, "/multiFileUpload").
        then().
        		rootPath("[%d]").
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

    @ParameterizedTest
    @MethodSource("methods")
    public void object_serialization_works(Method method) throws IOException {
        File file = createTempFile("something", "Something3210");

        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        String content =
        RestAssuredMockMvc.given().
                multiPart("controlName1", file, "mime-type1").
                multiPart("controlName2", greeting, "application/json").
        when().
                request(method, "/multiFileUpload").
        then().
        		rootPath("[%d]").
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

    @ParameterizedTest
    @MethodSource("methods")
    public void file_upload_and_param_mixing_works(Method method) {
    	RestAssuredMockMvc.given().
                multiPart("controlName", "original", "something32".getBytes(), "mime-type").
                param("param", "paramValue").
        when().
                request(method, "/fileUploadWithParam").
        then().
        		rootPath("file").
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("original")).
                body("mimeType", equalTo("mime-type")).
                noRootPath().
                body("param", equalTo("paramValue"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_settings_default_control_name_using_instance_configuration(Method method) throws IOException {
        File file = createTempFile("filename.txt", "Something21");

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultControlName("something"))).
                multiPart(file).
        when().
                request(method, "/fileUploadWithControlNameEqualToSomething").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("something")).
                body("originalName", equalTo("filename.txt"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_settings_default_control_name_using_static_configuration(Method method) throws IOException {
        File file = createTempFile("filename.txt", "Something21");

        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultControlName("something"));

        RestAssuredMockMvc.given().
                multiPart(file).
        when().
                request(method, "/fileUploadWithControlNameEqualToSomething").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("something")).
                body("originalName", equalTo("filename.txt"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_settings_default_file_name_using_instance_configuration(Method method) {
        
    	// File upload with Http Method POST
    	RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("filename.txt"))).
                multiPart("controlName", "something32".getBytes()).
        when().
               request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("filename.txt"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_settings_default_file_name_using_static_configuration(Method method) {
        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("filename.txt"));

        RestAssuredMockMvc.given().
                multiPart("controlName", "something32".getBytes()).
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo("filename.txt"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_sending_multipart_without_a_filename_when_default_file_name_is_empty(Method method) {
    	RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().emptyDefaultFileName())).
                multiPart("controlName", "something32".getBytes()).
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo(""));

    }

    @ParameterizedTest
    @MethodSource("methods")
    public void allows_sending_multipart_without_a_filename_when_default_file_name_is_set(Method method) {
    	RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().with().defaultFileName("custom"))).
                multiPart("controlName", null, "something32".getBytes()).
        when().
                request(method, "/fileUpload2").
        then().
                body("size", greaterThan(10)).
                body("name", equalTo("controlName")).
                body("originalName", equalTo(""));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void multi_part_uploading_supports_specifying_default_subtype(Method method) throws Exception {
       
    	// File upload with Http Method POST
        File file = createTempFile("filename.txt", "Something21");

        RestAssuredMockMvc.given().
               	config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().defaultSubtype("mixed"))).
               	multiPart("something", file).
        when().
                request(method, "/textAndReturnHeader").
        then().
                statusCode(200).
                body("size", greaterThan(10),
                     "name", equalTo("something"),
                    "originalName", equalTo("filename.txt")).
                header("X-Request-Header", startsWith("multipart/mixed"));
    }

    @ParameterizedTest
    @MethodSource("methods")
    public void explicit_multi_part_content_type_has_precedence_over_default_subtype(Method method) throws Exception {
        File file = createTempFile("filename.txt", "Something21");

       RestAssuredMockMvc.given().
               config(RestAssuredMockMvcConfig.config().multiPartConfig(multiPartConfig().defaultSubtype("form-data"))).
               contentType("multipart/mixed").
               multiPart("something", file).
       when().
               request(method, "/textAndReturnHeader").
       then().
               statusCode(200).
               body("size", greaterThan(10),
                    "name", equalTo("something"),
                    "originalName", equalTo("filename.txt")).
               header("X-Request-Header", startsWith("multipart/mixed"));
    }
}
// @formatter:on
