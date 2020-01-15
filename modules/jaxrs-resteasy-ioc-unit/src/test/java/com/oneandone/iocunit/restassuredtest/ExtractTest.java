package com.oneandone.iocunit.restassuredtest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;

import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(GreetingResource.class)
public class ExtractTest {
    @Test
    public void
    can_extract_rest_assureds_mock_mvc_response() {
        Response response = RestAssured.given().
                param("name", "Johan").
                when().
                get("/greeting").
                then().
                statusCode(200).
                body("id", equalTo(1)).
                extract().
                response();

        assertEquals(response.<String>path("content"),"Hello, Johan!");
    }

    @Test public void
    can_extract_spring_mvcs_response() {
        Response response =

                RestAssured.given().
                        param("name", "Johan").
                        when().
                        get("/greeting").
                        then().
                        statusCode(200).
                        body("id", equalTo(1)).
                        extract().
                        response();

        assertTrue(response.getContentType().contains("application/json"));
    }
}
