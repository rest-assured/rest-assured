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

package com.oneandone.iocunit.restassuredtest;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

import java.io.PrintStream;
import java.io.StringWriter;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.PutResource;
import com.oneandone.iocunit.restassuredtest.support.Greeting;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;

@RunWith(IocUnitRunner.class)
@SutClasses(PutResource.class)
public class PutTest {

    @Test
    public void
    doesnt_automatically_adds_x_www_form_urlencoded_as_content_type_when_putting_params() {
        StringWriter writer = new StringWriter();
        PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.given().
                config(RestAssured.config().logConfig(new LogConfig(captor, true))).
                param("name", "Johan").
                when().
                put("/greetingPut").
                then().
                log().all().
                statusCode(400); // statusCode(415);

        //assertThat(writer.toString(), equalTo(String.format("415 Content type 'null' not supported%n" +
        //                                                     "Accept: application/x-www-form-urlencoded%n")));
        Assert.assertTrue(writer.toString().contains("RESTEASY003320"));  // NUllpointerexception in Resteasy 3.0.19
    }

    @Test
    public void
    automatically_adds_x_www_form_urlencoded_as_content_type_when_putting_form_params() {
        RestAssured.given().
                formParam("name", "Johan").
                when().
                put("/greetingPut").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test
    public void
    can_supply_string_as_body_for_put() {
        RestAssured.given().
                body("a string").
                when().
                put("/stringBody").
                then().
                body(equalTo("a string"));
    }

    @Test
    public void
    can_supply_object_as_body_and_serialize_as_json() {
        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        RestAssured.given().
                contentType(JSON).
                body(greeting).
                when().
                put("/jsonReflect").
                then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }
}
