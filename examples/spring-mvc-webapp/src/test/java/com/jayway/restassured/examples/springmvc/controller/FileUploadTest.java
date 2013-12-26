package com.jayway.restassured.examples.springmvc.controller;

import com.jayway.restassured.examples.springmvc.config.MainConfiguration;
import com.jayway.restassured.examples.springmvc.support.Greeting;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import com.jayway.restassured.path.json.JsonPath;
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

import static com.jayway.restassured.RestAssured.withArgs;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
// @formatter:off
public class FileUploadTest {

    @Autowired
    protected WebApplicationContext wac;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Before
    public void configureMockMvcInstance() {
        RestAssuredMockMvc.webAppContextSetup(wac);
    }

    @After
    public void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    file_uploading_works()throws IOException {
        File file = folder.newFile("something");
        IOUtils.write("Something21", new FileOutputStream(file));

        given().
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

        given().
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
        given().
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
        given().
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


        given().
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
        given().
                multiPart("controlName1", file, "mime-type1").
                multiPart("controlName2", greeting, "application/json").
        when().
                post("/multiFileUpload").
        then().
                log().all().
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
}
// @formatter:on