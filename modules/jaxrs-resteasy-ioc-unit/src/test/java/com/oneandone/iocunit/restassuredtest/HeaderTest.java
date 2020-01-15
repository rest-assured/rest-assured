package com.oneandone.iocunit.restassuredtest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.HeaderResource;
import com.oneandone.iocunit.restassuredtest.http.RedirectResource;

import io.restassured.http.Header;
import io.restassured.http.Headers;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses({RedirectResource.class, HeaderResource.class})
public class HeaderTest {

    private ExpectedException exception = ExpectedException.none();

    @Rule
    public ExpectedException getExceptionRule() {
        return exception;
    }

    @Test
    public void
    can_send_header_using_header_class() {
        given().
                header(new Header("headerName", "John Doe")).
                when().
                get("/header").
                then().
                statusCode(200).
                body("headerName", equalTo("John Doe"));
    }

    @Test public void
    can_use_mapping_function_when_validating_header_value() {
        given().
                header(new Header("headerName", "200")).
                when().
                get("/header").
                then().
                header("Content-Length", Integer::parseInt, lessThanOrEqualTo(1000));
    }

    @Test public void
    validate_may_fail_when_using_mapping_function_when_validating_header_value() {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected header \"Content-Length\" was not a value greater than <1000>, was \"45\". Headers are:");

        given().
                header(new Header("headerName", "200")).
                when().
                get("/header").
                then().
                header("Content-Length", Integer::parseInt, greaterThan(1000));
    }

    @Test public void
    can_send_header_using_header_name_and_value() {
        given().
                header("headerName", "John Doe").
                when().
                get("/header").
                then().
                statusCode(200).
                body("headerName", equalTo("John Doe"));
    }

    @Test public void
    can_send_multiple_headers() {
        given().
                header("headerName", "John Doe").
                header("user-agent", "rest assured").
                when().
                get("/header").
                then().
                statusCode(200).
                body("headerName", equalTo("John Doe")).
                body("user-agent", equalTo("rest assured"));
    }

    @Test public void
    can_send_headers_using_map() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("headerName", "John Doe");
        headers.put("user-agent", "rest assured");

        given().
                headers(headers).
                when().
                get("/header").
                then().
                statusCode(200).
                body("headerName", equalTo("John Doe")).
                body("user-agent", equalTo("rest assured"));
    }

    @Test public void
    can_send_headers_using_headers_class() {
        given().
                headers(new Headers(new Header("headerName", "John Doe"), new Header("user-agent", "rest assured"))).
                when().
                get("/header").
                then().
                statusCode(200).
                body("headerName", equalTo("John Doe")).
                body("user-agent", equalTo("rest assured"));
    }

    @Test
    public void canUseResponseAwareMatchersForHeaderValidation() {
        given().
                when().
                get("/redirect").
                then().
                statusCode(301).
                header("Location", response-> endsWith("/redirect/"+response.path("id")));
    }
}

