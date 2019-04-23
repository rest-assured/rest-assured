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

package io.restassured.itest.java.presentation;

import io.restassured.itest.java.support.WithJetty;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import org.junit.Test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class SimplePathITest extends WithJetty {

    @Test
    public void simpleJsonValidationWithJsonPath() {
        final String body = get("/greetJSON?firstName=John&lastName=Doe").asString();

        final JsonPath json = new JsonPath(body).setRootPath("greeting");
        final String firstName = json.getString("firstName");
        final String lastName = json.getString("lastName");

        assertThat(firstName, equalTo("John"));
        assertThat(lastName, equalTo("Doe"));
    }

    @Test
    public void simpleXmlValidationWithXmlPath() {
        final String body = get("/greetXML?firstName=John&lastName=Doe").asString();

        final XmlPath xml = new XmlPath(body).setRootPath("greeting");
        final String firstName = xml.getString("firstName");
        final String lastName = xml.getString("lastName");

        assertThat(firstName, equalTo("John"));
        assertThat(lastName, equalTo("Doe"));
    }

}
