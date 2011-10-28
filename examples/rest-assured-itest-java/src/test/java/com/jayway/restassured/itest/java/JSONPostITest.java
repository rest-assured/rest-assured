/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static groovyx.net.http.ContentType.JSON;
import static groovyx.net.http.ContentType.URLENC;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.ArrayUtils.toObject;
import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class JSONPostITest extends WithJetty {

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().post("/greet");
    }

    @Test
    public void parametersAcceptsIntArguments() throws Exception {
        given().parameters("firstName", 1234, "lastName", 5678).expect().body("greeting", equalTo("Greetings 1234 5678")).when().post("/greet");
    }

    @Test
    public void formParametersAcceptsIntArguments() throws Exception {
        given().formParameters("firstName", 1234, "lastName", 5678).expect().body("greeting", equalTo("Greetings 1234 5678")).when().post("/greet");
    }

    @Test
    public void formParamsAcceptsIntArguments() throws Exception {
        given().formParams("firstName", 1234, "lastName", 5678).expect().body("greeting", equalTo("Greetings 1234 5678")).when().post("/greet");
    }

    @Test
    public void formParamAcceptsIntArguments() throws Exception {
        given().
                formParam("firstName", 1234).
                formParam("lastName", 5678).
        expect().
                body("greeting", equalTo("Greetings 1234 5678")).
        when().
                post("/greet");
    }

    @Test
    public void bodyWithSingleHamcrestMatching() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(containsString("greeting")).when().post("/greet");
    }

    @Test
    public void bodyWithSingleHamcrestMatchingUsingPathParams() throws Exception {
        expect().body(containsString("greeting")).when().post("/greet?firstName=John&lastName=Doe");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().post("/greet");
    }

    @Test
    public void usingRequestSpecWithParamsWorksWithPost() throws Exception {
        RestAssured.requestSpecification = new RequestSpecBuilder().addParam("firstName", "John").addParam("lastName", "Doe").build();
        try {
            expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().post("/greet");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void requestContentType() throws Exception {
        final RequestSpecification requestSpecification = given().contentType(URLENC).with().parameters("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(JSON).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).post("/greet");
    }

    @Test
    public void uriNotFoundTWhenPost() throws Exception {
        expect().statusCode(404).and().body(equalTo("Not found")).when().post("/lotto");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeaders() throws Exception {
        given().headers("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().post("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingStringBodyForPost() throws Exception {
        given().request().body("some body").then().expect().response().body(equalTo("some body")).when().post("/body");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingJsonBodyForPost() throws Exception {
        given().body("{ \"message\" : \"hello world\"}").with().contentType(JSON).then().expect().body(equalTo("hello world")).when().post("/jsonBody");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingStringForPost() throws Exception {
        given().body("tjo").and().expect().body(equalTo("tjo")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingIntForPost() throws Exception {
        given().body(2).and().expect().body(equalTo("2")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingFloatForPost() throws Exception {
        given().body(2f).and().expect().body(equalTo("2.0")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingDoubleForPost() throws Exception {
        given().body(2d).and().expect().body(equalTo("2.0")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingShortForPost() throws Exception {
        given().body((short) 2).and().expect().body(equalTo("2")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingBooleanForPost() throws Exception {
        given().body(true).and().expect().body(equalTo("true")).when().post("/reflect");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingJsonContentForPost() throws Exception {
        given().content("{ \"message\" : \"hello world\"}").with().contentType(JSON).and().expect().body(equalTo("hello world")).when().post("/jsonBody");
    }

    @Test
    public void allowsSpecifyingDefaultRequestContentType() throws Exception {
        RestAssured.requestContentType(JSON);
        try {
            given().body("{ \"message\" : \"hello world\"}").then().expect().body(equalTo("hello world")).when().post("/jsonBody");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void allowsSpecifyingDefaultRequestContentTypeAsString() throws Exception {
        RestAssured.requestContentType("application/json");
        try {
            given().body("{ \"message\" : \"hello world\"}").then().expect().body(equalTo("hello world")).when().post("/jsonBody");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void requestSpecificationAllowsSpecifyingJsonBodyAsStringForPost() throws Exception {
        given().body("{ \"message\" : \"hello world\"}").with().contentType("application/json").then().expect().body(equalTo("hello world")).when().post("/jsonBody");
    }

    @Test
    public void responseSpecificationAllowsSpecifyingJsonBodyForPost() throws Exception {
        given().body("{ \"message\" : \"hello world\"}").expect().contentType(JSON).and().body(equalTo("hello world")).when().post("/jsonBodyAcceptHeader");
    }

    @Test
    public void responseSpecificationAllowsSpecifyingJsonBodyAsStringForPost() throws Exception {
        given().body("{ \"message\" : \"hello world\"}").expect().contentType("application/json").and().body(equalTo("hello world")).when().post("/jsonBodyAcceptHeader");
    }

    @Test
    public void multiValueParametersSupportsAppendingWhenPassingInList() throws Exception {
        with().param("list", "1").param("list", asList("2", "3")).expect().body("list", equalTo("1,2,3")).when().post("/multiValueParam");
    }

    @Test
    public void allowsSpecifyingDefaultResponseContentType() throws Exception {
        RestAssured.responseContentType(JSON);
        try {
            given().body("{ \"message\" : \"hello world\"}").expect().body(equalTo("hello world")).when().post("/jsonBodyAcceptHeader");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void allowsSpecifyingDefaultResponseContentTypeAsString() throws Exception {
        RestAssured.responseContentType("application/json");
        try {
            given().body("{ \"message\" : \"hello world\"}").expect().body(equalTo("hello world")).when().post("/jsonBodyAcceptHeader");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void supportsReturningPostBody() throws Exception {
        final String body = with().parameters("firstName", "John", "lastName", "Doe").when().post("/greet").asString();

        final JsonPath jsonPath = new JsonPath(body);
        assertThat(jsonPath.getString("greeting"), equalTo("Greetings John Doe"));
    }

    @Test
    public void supportsGettingResponseBodyWhenStatusCodeIs401() throws Exception {
        final Response response = post("/secured/hello");

        assertThat(response.getBody().asString(), containsString("401 UNAUTHORIZED"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingBinaryBodyForPost() throws Exception {
        byte[] body = { 23, 42, 127, 123};
        given().body(body).then().expect().body(equalTo("23, 42, 127, 123")).when().post("/binaryBody");
    }

    @Test
    public void requestSpecificationWithContentTypeOctetStreamAllowsSpecifyingBinaryBodyForPost() throws Exception {
        byte[] bytes = "somestring".getBytes();
        final String expectedResponseBody = join(toObject(bytes), ", ");

        given().
                contentType("application/octet-stream").
                body(bytes).
        expect().
                statusCode(200).
                body(is(expectedResponseBody)).
        when().
                post("/binaryBody");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().post("/cookie");
    }
}
