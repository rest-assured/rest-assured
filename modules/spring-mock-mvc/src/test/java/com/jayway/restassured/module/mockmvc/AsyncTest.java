package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.PostAsyncController;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.jayway.restassured.module.mockmvc.config.MockMvcAsyncConfig.withTimeout;
import static com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class AsyncTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc = standaloneSetup(new PostAsyncController()).build();
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    can_supply_string_as_body_for_async_post_with_config_in_given() {
        given().
                config(newConfig().asyncConfig(withTimeout(10, TimeUnit.MILLISECONDS))).
                body("a string").
        when().
                post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    exception_will_be_thrown_if_async_data_has_not_been_provided_in_defined_time_with_config_in_given() {
        // given
        Exception exception = null;

        // when
        try {
            given().
                    config(newConfig().asyncConfig(withTimeout(0, TimeUnit.MILLISECONDS))).
                    body("a string").
            when().
                    post("/tooLongAwaiting").
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
        given().
                body("a string").
        when().
            async().and().then().
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
            given().
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
