package com.oneandone.iocunit.restassuredtest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.BasePathResource;

import io.restassured.RestAssured;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(BasePathResource.class)
public class BasePathTest {
    @Test
    public void
    base_path_is_prepended_to_path() {

        RestAssured.basePath = "/my-path";
        given().
                param("name", "Johan").
                when().
                get("/greetingPath").
                then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    default_base_path_is_slash() {
        given().
                param("name", "Johan").
                when().
                get().
                then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    double_slashes_are_prevented() {
        RestAssured.basePath = "/my-path";

        given().
                param("name", "Johan").
                when().
                get("/greetingPath").
                then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    base_path_can_end_with_slash_and_path_doesnt_have_to_begin_with_slash() {
        RestAssured.basePath = "/my-path";

        given().
                param("name", "Johan").
                when().
                get("greetingPath").
                then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    base_path_doesnt_have_to_end_with_slash_even_though_path_doesnt_begin_with_slash2() {
        RestAssured.basePath = "/my-path";

        given().
                param("name", "Johan").
                when().
                get("greetingPath").
                then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }
}
