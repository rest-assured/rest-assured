package com.oneandone.iocunit.restassuredtest;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;

import io.restassured.RestAssured;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(GreetingResource.class)
public class GetTest {
    @Test
    public void
    unnamed_path_params_works() {
        RestAssured.given().
                when().
                get("/greeting?name={name}", "Johan").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }
}
