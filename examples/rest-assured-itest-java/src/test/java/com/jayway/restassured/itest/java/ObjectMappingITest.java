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

import com.jayway.restassured.itest.java.objects.Greeting;
import com.jayway.restassured.itest.java.objects.Message;
import com.jayway.restassured.itest.java.objects.ScalatraObject;
import com.jayway.restassured.itest.java.support.WithJetty;
import groovyx.net.http.ContentType;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.parsing.Parser.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ObjectMappingITest extends WithJetty {

    @Test
    public void mapResponseToObjectUsingJackson() throws Exception {
        final ScalatraObject object = get("/hello").as(ScalatraObject.class);

        assertThat(object.getHello(), equalTo("Hello Scalatra"));
    }

    @Test
    public void mapResponseToObjectUsingJaxb() throws Exception {
        final Greeting object = given().parameters("firstName", "John", "lastName", "Doe").when().get("/greetXML").as(Greeting.class);

        assertThat(object.getFirstName(), equalTo("John"));
        assertThat(object.getLastName(), equalTo("Doe"));
    }

    @Test
    public void mapResponseToObjectUsingJacksonWhenNoContentTypeIsDefined() throws Exception {
        final Message message =
                expect().
                        defaultParser(JSON).
                when().
                        get("/noContentTypeJsonCompatible").as(Message.class);

        assertThat(message.getMessage(), equalTo("It works"));
    }

    @Test
    public void whenNoRequestContentTypeIsSpecifiedThenRestAssuredSerializesToJSON() throws Exception {
        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        final ScalatraObject actual = expect().defaultParser(JSON).given().body(object).when().post("/reflect").as(ScalatraObject.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsJsonThenRestAssuredSerializesToJSON() throws Exception {
        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        final ScalatraObject actual = given().contentType(ContentType.JSON).and().body(object).when().post("/reflect").as(ScalatraObject.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlThenRestAssuredSerializesToJSON() throws Exception {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType(ContentType.XML).and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlAndCharsetIsUTF16ThenRestAssuredSerializesToJSON() throws Exception {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/xml; charset=UTF-16").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsJsonAndCharsetIsUTF16ThenRestAssuredSerializesToJSON() throws Exception {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/json; charset=UTF-16").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }
}
