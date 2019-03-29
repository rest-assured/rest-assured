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
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.FailureConfig.failureConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ConfigITest extends WithJetty {

    @Test
    public void configCanBeSetPerRequest() throws Exception {
        given().
                config(RestAssuredConfig.newConfig().redirect(RedirectConfig.redirectConfig().followRedirects(false))).
                param("url", "/hello").
        expect().
                statusCode(302).
                header("Location", is("http://localhost:8080/hello")).
        when().
                get("/redirect");
    }

    @Test
    public void supportsSpecifyingDefaultContentCharset() throws Exception {
        given().
                config(RestAssuredConfig.newConfig().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("US-ASCII"))).
                body("Something {\\+£???").
        expect().
                header("Content-Type", is("text/plain; charset=US-ASCII")).
        when().
                post("/reflect");
    }

    @Test
    public void supportsConfiguringJsonConfigProperties() throws Exception {
        given().
                config(RestAssuredConfig.newConfig().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))).
        expect().
                rootPath("store.book").
                body("price.min()", is(new BigDecimal("8.95"))).
                body("price.max()", is(new BigDecimal("22.99"))).
        when().
                get("/jsonStore");
    }

    @Test
    public void supportsConfiguringJsonConfigStatically() throws Exception {
        RestAssured.config = RestAssuredConfig.newConfig().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        try {
        expect().
                rootPath("store.book").
                body("price.min()", is(new BigDecimal("8.95"))).
                body("price.max()", is(new BigDecimal("22.99"))).
        when().
                get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void configurationsDefinedGloballyAreAppliedWhenUsingResponseSpecBuilders() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        ResponseValidationFailureListener failureListener = (reqSpec, respSpec, resp) -> atomicBoolean.set(true);

        try {
            given().config(RestAssuredConfig.config().failureConfig(failureConfig().failureListeners(failureListener)))
                    .get("http://jsonplaceholder.typicode.com/todos/1").then()
                    .spec(new ResponseSpecBuilder().expectStatusCode(400).build());
            fail("Should throw exception");
        } catch (Error ignored) {
        }

        assertThat(atomicBoolean.get(), is(true));
    }

    @Test
    public void configurationsDefinedInDslAreAppliedWhenUsingResponseSpecBuilders() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        ResponseValidationFailureListener failureListener = (reqSpec, respSpec, resp) -> atomicBoolean.set(true);

        try {
            given().config(RestAssuredConfig.config().failureConfig(failureConfig().failureListeners(failureListener))).get("http://jsonplaceholder.typicode.com/todos/1")
                    .then().spec(new ResponseSpecBuilder().expectStatusCode(400).build());
            fail("Should throw exception");
        } catch (Error ignored) {
        }

        assertThat(atomicBoolean.get(), is(true));
    }
}
