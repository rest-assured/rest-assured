package io.restassured;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.specification.ResponseSpecification;

/**
 * Created by andrey.smirnov on 03.05.2017.
 */
public class ResponseContext {
    public long threadId;
    public String rootPath;
    public ResponseSpecification responseSpecification;
    public ResponseParserRegistrar responseParserRegistrar;
    public RestAssuredConfig config;

    public ResponseContext(long threadId, String rootPath, ResponseSpecification responseSpecification, RestAssuredConfig config) {
        this.threadId = threadId;
        this.rootPath = rootPath;
        this.responseSpecification = responseSpecification;
        this.responseParserRegistrar = resolveResponseParserRegistrar();
        this.config = config;
    }

    public static ResponseContext defaultResponseContext = new ResponseContext(
            0L,
            RestAssured.rootPath,
            RestAssured.responseSpecification,
            RestAssured.config()
    );

    public static ThreadLocal<ResponseContext> responseContext = new ThreadLocal<ResponseContext>();

    public static void setContext(ResponseContext context) {
        responseContext.set(context);
    }

    private static ResponseParserRegistrar resolveResponseParserRegistrar() {
        ResponseParserRegistrar responseParserRegistrar = RestAssured.getResponseParserRegistrar();
        if (RestAssured.defaultParser != null) {
            responseParserRegistrar.registerDefaultParser(RestAssured.defaultParser);
        }
        return responseParserRegistrar;
    }

}
