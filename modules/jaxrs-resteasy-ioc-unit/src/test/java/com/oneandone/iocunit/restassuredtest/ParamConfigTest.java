package com.oneandone.iocunit.restassuredtest;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.MultiValueResource;

import io.restassured.RestAssured;
import io.restassured.config.ParamConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(MultiValueResource.class)
public class ParamConfigTest {
    @Test
    public void
    merges_request_params_by_default() {
        given().
                param("list", "value1").
                param("list", "value2").
                when().
                get("/multiValueParam").
                then().
                body("list", equalTo("value1,value2"));
    }

    public static RestAssuredConfig config() {
        return RestAssured.config();
    }


    @Test
    public void
    merges_query_params_by_default() {
        RestAssured.given().
                queryParam("list", "value1").
                queryParam("list", "value2").
                when().
                get("/multiValueParam").
                then().
                body("list", equalTo("value1,value2"));
    }

    @Test public void
    merges_form_params_by_default() {
        RestAssured.given().
                formParam("list", "value1").
                formParam("list", "value2").
                when().
                post("/multiValueParam").
                then().
                body("list", equalTo("value1,value2"));
    }

    @Test public void
    replaces_request_params_when_configured_to_do_so() {
        RestAssured.given().
                config(config().paramConfig(paramConfig().requestParamsUpdateStrategy(REPLACE))).
                param("list", "value1").
                param("list", "value2").
                queryParam("list2", "value3").
                queryParam("list2", "value4").
                formParam("list3", "value5").
                formParam("list3", "value6").
                when().
                post("/threeMultiValueParam").
                then().
                body("list", equalTo("value2")).
                body("list2", equalTo("value3,value4")).
                body("list3", equalTo("value5,value6"));
    }

    private ParamConfig paramConfig() {
        return new ParamConfig();
    }

    @Test public void
    replaces_query_params_when_configured_to_do_so() {
        RestAssured.given().
                config(config().paramConfig(paramConfig().queryParamsUpdateStrategy(REPLACE))).
                param("list", "value1").
                param("list", "value2").
                queryParam("list2", "value3").
                queryParam("list2", "value4").
                formParam("list3", "value5").
                formParam("list3", "value6").
                when().
                post("/threeMultiValueParam").
                then().
                body("list", equalTo("value1,value2")).
                body("list2", equalTo("value4")).
                body("list3", equalTo("value5,value6"));
    }

    @Test public void
    replaces_form_params_when_configured_to_do_so() {
        RestAssured.given().
                config(config().paramConfig(paramConfig().formParamsUpdateStrategy(REPLACE))).
                param("list", "value1").
                param("list", "value2").
                queryParam("list2", "value3").
                queryParam("list2", "value4").
                formParam("list3", "value5").
                formParam("list3", "value6").
                when().
                post("/threeMultiValueParam").
                then().
                body("list", equalTo("value1,value2")).
                body("list2", equalTo("value3,value4")).
                body("list3", equalTo("value6"));
    }

    @Test public void
    replaces_all_parameters_when_configured_to_do_so() {
        RestAssured.given().
                config(config().paramConfig(paramConfig().replaceAllParameters())).
                param("list", "value1").
                param("list", "value2").
                queryParam("list2", "value3").
                queryParam("list2", "value4").
                formParam("list3", "value5").
                formParam("list3", "value6").
                when().
                post("/threeMultiValueParam").
                then().
                body("list", equalTo("value2")).
                body("list2", equalTo("value4")).
                body("list3", equalTo("value6"));
    }

    @Test public void
    merges_all_parameters_when_configured_to_do_so() {
        RestAssured.config = config().paramConfig(paramConfig().replaceAllParameters());

        RestAssured.given().
                config(config().paramConfig(paramConfig().mergeAllParameters())).
                param("list", "value1").
                param("list", "value2").
                queryParam("list2", "value3").
                queryParam("list2", "value4").
                formParam("list3", "value5").
                formParam("list3", "value6").
                when().
                post("/threeMultiValueParam").
                then().
                body("list", equalTo("value1,value2")).
                body("list2", equalTo("value3,value4")).
                body("list3", equalTo("value5,value6"));
    }
}
