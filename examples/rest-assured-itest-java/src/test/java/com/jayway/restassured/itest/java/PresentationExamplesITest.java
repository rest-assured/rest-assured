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

import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.path.xml.XmlPath;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PresentationExamplesITest extends WithJetty {

    @Test
    public void simpleJsonValidation() throws Exception {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/greetJSON");
    }

    @Test
    public void simpleXmlValidation() throws Exception {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/greetXML");
    }

    @Test
    public void simpleJsonValidationWithJsonPath() throws Exception {
        final String body = get("/greetJSON?firstName=John&lastName=Doe").asString();

        final JsonPath json = new JsonPath(body).setRoot("greeting");
        final String firstName = json.getString("firstName");
        final String lastName = json.getString("lastName");

        assertThat(firstName, equalTo("John"));
        assertThat(lastName, equalTo("Doe"));
    }

    @Test
    public void simpleXmlValidationWithXmlPath() throws Exception {
        final String body = get("/greetXML?firstName=John&lastName=Doe").asString();

        final XmlPath xml = new XmlPath(body).setRoot("greeting");
        final String firstName = xml.getString("firstName");
        final String lastName = xml.getString("lastName");

        assertThat(firstName, equalTo("John"));
        assertThat(lastName, equalTo("Doe"));
    }
}