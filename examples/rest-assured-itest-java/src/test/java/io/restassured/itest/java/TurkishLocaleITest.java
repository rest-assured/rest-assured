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

import io.restassured.itest.java.support.WithJetty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.request;
import static org.hamcrest.Matchers.equalTo;

/**
 * In the Turkish locale {@code "FAILURE".toLowerCase()} yields {@code "faılure"} (dotless i),
 * which used to break the lookup of the failure response handler and cause an exception
 * for every response with status code >= 400. Likewise {@code "OPTIONS".toLowerCase()} yields
 * {@code "optıons"} and {@code "options".toUpperCase()} yields {@code "OPTİONS"}.
 */
public class TurkishLocaleITest extends WithJetty {

    private static final Locale INITIAL_LOCALE = Locale.getDefault();

    @BeforeAll
    public static void setUpBeforeClass() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterAll
    public static void tearDownAfterClass() {
        Locale.setDefault(INITIAL_LOCALE);
    }

    @Test
    public void canAssertOn401InTurkishLocale() {
        given().auth().none().expect().statusCode(401).when().get("/secured/hello");
    }

    @Test
    public void canAssertOn404InTurkishLocale() {
        expect().statusCode(404).when().get("/does-not-exist");
    }

    @Test
    public void canSendOptionsRequestFromFilterInTurkishLocale() {
        given().
                filter((requestSpec, responseSpec, ctx) -> ctx.send(given().body("a body"))).
        expect().
                body(equalTo("a body")).
        when().
                options("/returnBodyAsBody");
    }

    @Test
    public void canSendRequestWithLowerCaseMethodNameInTurkishLocale() {
        given().body("a body").expect().body(equalTo("a body")).when().request(" options ", "/returnBodyAsBody");
    }
}
