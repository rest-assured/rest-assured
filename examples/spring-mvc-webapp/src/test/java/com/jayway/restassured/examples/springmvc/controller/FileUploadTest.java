package com.jayway.restassured.examples.springmvc.controller;

import com.jayway.restassured.examples.springmvc.config.MainConfiguration;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
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

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

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
}
// @formatter:on