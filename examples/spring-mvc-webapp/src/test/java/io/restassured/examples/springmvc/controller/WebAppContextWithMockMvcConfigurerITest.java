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

package io.restassured.examples.springmvc.controller;

import io.restassured.examples.springmvc.config.MainConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
public class WebAppContextWithMockMvcConfigurerITest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Autowired
    private WebApplicationContext context;

    @Test public void
    web_app_context_setup_allows_passing_in_mock_mvc_configurers_using_the_dsl() {
        RestAssuredMockMvc.given().
                webAppContextSetup(context, springSecurity()).
                postProcessors(httpBasic("username", "password")).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    web_app_context_setup_allows_passing_in_mock_mvc_configurers_statically() {
        RestAssuredMockMvc.webAppContextSetup(context, springSecurity());

        try {
            RestAssuredMockMvc.given().
                    postProcessors(httpBasic("username", "password")).
                    param("name", "Johan").
            when().
                    get("/secured/greeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!")).
                    expect(authenticated().withUsername("username"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }
}
