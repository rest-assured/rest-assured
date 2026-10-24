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
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.WithJetty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.*;

public class HttpClientConfigITest extends WithJetty {

    @Test
    public void anHttpClient4ParameterThatRestAssuredDoesntUseAnyMoreFailsTheRequest() {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().setParam("http.default-headers", List.of("header1: value1")));
        try {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> given().param("url", "/hello").get("/multiHeaderReflect"));

            assertThat(thrown.getMessage(), startsWith("The Apache HttpClient 4 parameter \"http.default-headers\" of the HttpClientConfig "
                    + "isn't used any more"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void theRedirectParamsOfTheHttpClientConfigDontLimitRedirectsBecauseTheRedirectConfigHasPrecedence() throws Exception {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().setParam("http.protocol.handle-redirects", true).and().setParam("http.protocol.max-redirects", 0));
        try {
            given().
                    param("url", "/hello").
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

}
