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

import io.restassured.itest.java.presentation.filter.CustomAuthFilter;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class CustomAuthDemoITest extends WithJetty {

    @Test
    public void customAuthDemo() throws Exception {
        given().
                filter(new CustomAuthFilter()).
        expect().
                body("message", equalTo("I'm secret")).
        when().
                get("/custom-auth/secretMessage");

    }

    @Test
    public void customAuthDemo2() throws Exception {
        given().
                filter(new CustomAuthFilter()).
        expect().
                body("message", equalTo("I'm also secret")).
        when().
                get("/custom-auth/secretMessage2");
    }
}
