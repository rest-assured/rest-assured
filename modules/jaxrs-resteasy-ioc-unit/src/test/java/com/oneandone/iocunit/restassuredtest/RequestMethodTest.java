package com.oneandone.iocunit.restassuredtest;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.request;
import static io.restassured.http.Method.GET;
import static io.restassured.http.Method.POST;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;
import com.oneandone.iocunit.restassuredtest.http.PostResource;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses({PostResource.class, GreetingResource.class})
public class RequestMethodTest {

    private ExpectedException exception = ExpectedException.none();

    @Rule
    public ExpectedException getExceptionRule() {
        return exception;
    }

    @Test
    public void
    request_method_accepts_enum_verb() {
        given().
                param("name", "Johan").
                when().
                request(POST, "/greetingPost").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    request_method_accepts_enum_verb_and_unnamed_path_params() {
        given().
                queryParam("name", "John").
                when().
                request(GET, "/{x}", "greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test public void
    request_method_accepts_string_verb() {
        given().
                param("name", "Johan").
                when().
                request("post", "/greetingPost").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    request_method_accepts_string_verb_and_unnamed_path_params() {
        given().
                queryParam("name", "John").
                when().
                request("GEt", "/{x}", "greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test public void
    static_request_method_accepts_string_verb() {

        request("  gEt ", "/greeting").then().body("id", equalTo(1)).body("content", equalTo("Hello, World!"));

    }

    @Test public void
    static_request_method_accepts_enum_verb_and_path_params() {

        request(GET, "/{greeting}", "greeting").then().body("id", equalTo(1)).body("content", equalTo("Hello, World!"));

    }

    @Test public void
    throws_iae_when_http_verb_is_not_supported_by_mock_mvc() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("HTTP method 'CONNECT' is not supported");

        given().request("connect", "/greeting");
    }
}
