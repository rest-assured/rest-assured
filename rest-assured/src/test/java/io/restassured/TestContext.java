package io.restassured;


import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Created by andrey.smirnov on 03.05.2017.
 */
@Test(threadPoolSize = 2)
public class TestContext {

    @DataProvider(parallel = true)
    public Object[][] defaultValues() {
        return new Object[][] {
                new Object[] {"ip"},
                new Object[] {"user-agent"},
                new Object[] {"headers"},
                new Object[] {"get"},
                new Object[] {"gzip"}
        };
    }

    @Test(threadPoolSize = 5, dataProvider = "defaultValues")
    public void testContextWithSeveralThreads(String basePath) {
        RequestContext.requestContext.set(new RequestContext(
                Thread.currentThread().getId(),
                "http://httpbin.org/",
                80,
                basePath,
                RestAssured.authentication,
                RestAssured.filters(),
                RestAssured.requestSpecification,
                RestAssured.urlEncodingEnabled,
                RestAssured.config(),
                RestAssured.proxy
        ));

        given()
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test(threadPoolSize = 5)
    public void testContextWithDefaultConfig() {
        RestAssured.baseURI = "http://httpbin.org/";
        RestAssured.port = 80;
        RestAssured.basePath = "/cookies";
        given()
                .log().all()
                .when()
                .get()
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

}
