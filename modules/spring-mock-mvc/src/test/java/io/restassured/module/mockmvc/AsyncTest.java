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

package io.restassured.module.mockmvc;

import io.restassured.module.mockmvc.http.PostAsyncController;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.newConfig;
import static io.restassured.module.spring.commons.config.AsyncConfig.withTimeout;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class AsyncTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc(standaloneSetup(new PostAsyncController()).build());
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    can_supply_string_as_body_for_async_post_with_config_in_given() {
        RestAssuredMockMvc.given().
                config(newConfig().asyncConfig(withTimeout(10, TimeUnit.SECONDS))).
                body("a string").
        when().
                async().post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    can_configure_timeout_with_time_unit_using_the_async_dsl() {
        RestAssuredMockMvc.given().
                body("a string").
        when().
                async().with().timeout(2, DAYS).then().post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    can_configure_timeout_in_milliseconds_using_the_async_dsl() {
        RestAssuredMockMvc.given().
                body("a string").
        when().
                async().timeout(600).then().post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    exception_will_be_thrown_if_async_data_has_not_been_provided_in_defined_time_with_config_in_given() {
        // given
        Exception exception = null;

        // when
        try {
            RestAssuredMockMvc.given().
                    config(newConfig().asyncConfig(withTimeout(0, TimeUnit.MILLISECONDS))).
                    body("a string").
            when().
                    async().post("/tooLongAwaiting").
            then().
                    body(equalTo("a string"));
        } catch (IllegalStateException e) {
            exception = e;
        }

        // then
        assertThat(exception).isNotNull().hasMessageContaining("was not set during the specified timeToWait=0");
    }

    @Test public void
    can_supply_string_as_body_for_async_post() {
        RestAssuredMockMvc.given().
                body("a string").
        when().
            async().and().then().
                post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    can_supply_string_as_body_for_async_post_without_syntactic_sugar() {
        RestAssuredMockMvc.given().
                body("a string").
        when().
            async().
                post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    exception_will_be_thrown_if_async_data_has_not_been_provided_in_defined_time() {
        // given
        Exception exception = null;

        // when
        try {
            RestAssuredMockMvc.given().
                    body("a string").
            when().
                async().
                    with().timeout(0, TimeUnit.MILLISECONDS).and().then().
                    post("/tooLongAwaiting").
            then().
                    body(equalTo("a string"));
        } catch (IllegalStateException e) {
            exception = e;
        }

        // then
        assertThat(exception).isNotNull().hasMessageContaining("was not set during the specified timeToWait=0");
    }
}
