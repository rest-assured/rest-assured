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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.itest.java.objects.Greeting;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.objects.ScalatraObject;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.parsing.Parser.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ObjectMappingITest extends WithJetty {

    @Test
    public void mapResponseToObjectUsingJackson() {
        final ScalatraObject object = get("/hello").as(ScalatraObject.class);

        assertThat(object.getHello(), equalTo("Hello Scalatra"));
    }

    @Test
    public void mapResponseToObjectUsingJakartaEESinceItIsDefaultWhenFoundInClasspath() {
        final Greeting object = given().params("firstName", "John", "lastName", "Doe").when().get("/greetXML").as(Greeting.class);

        assertThat(object.getFirstName(), equalTo("John"));
        assertThat(object.getLastName(), equalTo("Doe"));
    }

    @Test
    public void mapResponseToObjectUsingJacksonWhenNoContentTypeIsDefined() {
        final Message message =
                expect().
                        defaultParser(JSON).
                        when().
                        get("/noContentTypeJsonCompatible").as(Message.class);

        assertThat(message.getMessage(), equalTo("It works"));
    }

    @Test
    public void contentTypesEndingWithPlusJsonWorksForJsonObjectMapping() {
        final Message message = get("/mimeTypeWithPlusJson").as(Message.class);

        assertThat(message.getMessage(), equalTo("It works"));
    }

    @Test
    public void whenNoRequestContentTypeIsSpecifiedThenRestAssuredSerializesToJSON() {
        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        final ScalatraObject actual = expect().defaultParser(JSON).given().body(object).when().post("/reflect").as(ScalatraObject.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsJsonThenRestAssuredSerializesToJSON() {
        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        final ScalatraObject actual = given().contentType(ContentType.JSON).and().body(object).when().post("/reflect").as(ScalatraObject.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlThenRestAssuredSerializesToXMLUsingDefaultJakartaEE() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType(ContentType.XML).and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlThenRestAssuredSerializesToXMLUsingExplicitJaxb() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType(ContentType.XML).and().body(object, ObjectMapperType.JAXB).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlAndCharsetIsUsAsciiThenRestAssuredSerializesToXMLUsingDefaultJakartaEE() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/xml; charset=US-ASCII").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlAndCharsetIsUsAsciiThenRestAssuredSerializesToXMLUsingExplicitJaxb() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/xml; charset=US-ASCII").and().body(object, ObjectMapperType.JAXB).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsJsonAndCharsetIsUsAsciiThenRestAssuredSerializesToJSON() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/json; charset=US-ASCII").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlAndCharsetIsUtf16ThenRestAssuredSerializesToXML() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/xml; charset=UTF-16").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void whenRequestContentTypeIsXmlAndDefaultCharsetIsUtf16ThenRestAssuredSerializesToXML() {
        RestAssured.config = RestAssuredConfig.config().encoderConfig(encoderConfig().defaultContentCharset("UTF-16"));
        try {
            final Greeting object = new Greeting();
            object.setFirstName("John");
            object.setLastName("Doe");
            final Greeting actual = given().contentType("application/xml").and().body(object).when().post("/reflect").as(Greeting.class);
            assertThat(object, equalTo(actual));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenRequestContentTypeIsJsonAndCharsetIsUtf16ThenRestAssuredSerializesToJSON() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().contentType("application/json; charset=UTF-16").and().body(object).when().post("/reflect").as(Greeting.class);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void mapResponseToObjectUsingJaxbWithJaxObjectMapperDefined() {
        final Greeting object = given().params("firstName", "John", "lastName", "Doe").when().get("/greetXML").as(Greeting.class, ObjectMapperType.JAXB);

        assertThat(object.getFirstName(), equalTo("John"));
        assertThat(object.getLastName(), equalTo("Doe"));
    }

    @Test
    public void mapResponseToObjectUsingJackson1WithJacksonObjectMapperDefined() {
        final ScalatraObject object = get("/hello").as(ScalatraObject.class, ObjectMapperType.JACKSON_1);

        assertThat(object.getHello(), equalTo("Hello Scalatra"));
    }

    @Test
    public void mapResponseToObjectUsingJackson2WithJacksonObjectMapperDefined() {
        final ScalatraObject object = get("/hello").as(ScalatraObject.class, ObjectMapperType.JACKSON_2);

        assertThat(object.getHello(), equalTo("Hello Scalatra"));
    }

    @Test
    public void mapResponseToObjectUsingGsonWithGsonObjectMapperDefined() {
        final ScalatraObject object = get("/hello").as(ScalatraObject.class, ObjectMapperType.GSON);

        assertThat(object.getHello(), equalTo("Hello Scalatra"));
    }

    @Test
    public void serializesUsingJAXBWhenJAXBObjectMapperIsSpecified() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().body(object, ObjectMapperType.JAXB).when().post("/reflect").as(Greeting.class, ObjectMapperType.JAXB);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void serializesUsingJAXBWhenJAXBObjectMapperIsSpecifiedForPatchVerb() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().body(object, ObjectMapperType.JAXB).when().patch("/reflect").as(Greeting.class, ObjectMapperType.JAXB);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void serializesUsingGsonWhenGsonObjectMapperIsSpecified() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().body(object, ObjectMapperType.GSON).when().post("/reflect").as(Greeting.class, ObjectMapperType.GSON);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void serializesUsingJacksonWhenJacksonObjectMapperIsSpecified() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");
        final Greeting actual = given().body(object, ObjectMapperType.JACKSON_1).when().post("/reflect").as(Greeting.class, ObjectMapperType.JACKSON_1);
        assertThat(object, equalTo(actual));
    }

    @Test
    public void serializesNormalParams() {
        final Greeting object = new Greeting();
        object.setFirstName("John");
        object.setLastName("Doe");

        final Greeting actual =
                given().
                        contentType(ContentType.JSON).
                        param("something", "something").
                        param("serialized", object).
                        when().
                        put("/serializedJsonParameter").as(Greeting.class);

        assertThat(actual, equalTo(object));
    }
}
