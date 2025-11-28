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

import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.mapping.Jackson3Mapper;
import io.restassured.itest.java.objects.Greeting;
import io.restassured.itest.java.objects.GreetingRootMixin;
import io.restassured.itest.java.objects.GreetingWrapper;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.path.json.mapper.factory.DefaultJackson3ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.mapper.ObjectMapperType.JACKSON_3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

public class Jackson3ITest extends WithJetty {

   @Test
   void serializes_json_body_using_jackson3_with_default_config() {
       given().
               contentType(ContentType.JSON).
               body(new Message("hello world")).
       when().
                post("/jsonBody").
       then().
               statusCode(200).
               body(equalTo("hello world"));
   }

   @Test
   void serializes_json_body_using_jackson3_with_explicit_default_om_type() {
       given().
               config(RestAssuredConfig.config().objectMapperConfig(objectMapperConfig().defaultObjectMapperType(JACKSON_3))).
               contentType(ContentType.JSON).
               body(new Message("hello world")).
       when().
                post("/jsonBody").
       then().
               statusCode(200).
               body(equalTo("hello world"));
   }

   @Test
   void serializes_json_body_using_jackson3_with_explicit_default_om() {
       given().
               config(RestAssuredConfig.config().objectMapperConfig(objectMapperConfig().defaultObjectMapper(new Jackson3Mapper(new DefaultJackson3ObjectMapperFactory())))).
               contentType(ContentType.JSON).
               body(new Message("hello world")).
       when().
                post("/jsonBody").
       then().
               statusCode(200).
               body(equalTo("hello world"));
   }

   @Test
   void deserializes_json_body_using_jackson3_with_default_config() {
       var greetingWrapper =
       given().
               queryParam("firstName", "John").
               queryParam("lastName", "Doe").
       when().
                get("/greetJSON").
       then().
               statusCode(200).
               body("greeting.firstName", equalTo("John")).
               body("greeting.lastName", equalTo("Doe")).
        extract()
               .body().as(GreetingWrapper.class);

       var greeting = greetingWrapper.greeting();

       assertAll(
               () -> assertThat(greeting.firstName()).isEqualTo("John"),
               () -> assertThat(greeting.lastName()).isEqualTo("Doe")
       );
   }

   @Test
   void deserializes_json_body_using_jackson3_with_custom_config() {
       var greeting =
       given().
               config(RestAssuredConfig.config().objectMapperConfig(objectMapperConfig().jackson3ObjectMapperFactory((cls, charset) ->
                       // No need for GreetingWrapper with these setting
                       JsonMapper.builder()
                               .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                               .addMixIn(Greeting.class, GreetingRootMixin.class)
                               .build()))
               ).
               queryParam("firstName", "John").
               queryParam("lastName", "Doe").
       when().
                get("/greetJSON").
       then().
               statusCode(200).
               body("greeting.firstName", equalTo("John")).
               body("greeting.lastName", equalTo("Doe")).
        extract()
               .body().as(Greeting.class);

       assertAll(
               () -> assertThat(greeting.firstName()).isEqualTo("John"),
               () -> assertThat(greeting.lastName()).isEqualTo("Doe")
       );
   }
}