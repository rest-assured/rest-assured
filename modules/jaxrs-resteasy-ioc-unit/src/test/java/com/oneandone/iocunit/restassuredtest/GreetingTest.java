package com.oneandone.iocunit.restassuredtest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(GreetingResource.class)
public class GreetingTest {
    @Test
    public void
    uses_predefined_mock_mvc_instance() throws Exception {
        given().
                param("name", "Johan").
                when().
                get("/greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    param_with_int() throws Exception {
        given().
            param("name", 1).
                when().
                get("/greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, 1!"));
    }

    @Test public void
    uses_predefined_standalone() throws Exception {
        given().
                param("name", "Johan").
                when().
                get("/greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    uses_static_mock_mvc() throws Exception {
            given().
                    param("name", "Johan").
                    when().
                    get("/greeting").
                    then().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan!"));

            given().
                    param("name", "Erik").
                    when().
                    get("/greeting").
                    then().
                    body("id", equalTo(2)).
                    body("content", equalTo("Hello, Erik!"));
    }

    // from MockMvcPathParamTest
    @Test
    public void unnamed_path_param_works() throws Exception {
            given().
                queryParam("name", "John").
                when().
                get("/{x}", "greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }


}
