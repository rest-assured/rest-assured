package io.restassured;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.internal.log.LogRepository;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;

import java.util.List;

/**
 * Created by andrey.smirnov on 03.05.2017.
 */
public class RequestContext {

    public long threadId;
    public String baseURI;
    public int port;
    public String basePath;
    public AuthenticationScheme authentication;
    public List<Filter> filters;
    public RequestSpecification requestSpecification;
    public boolean urlEncodingEnabled;
    public RestAssuredConfig config;
    public ProxySpecification proxy;

    public RequestContext(long threadId, String baseURI, int port, String basePath, AuthenticationScheme authentication, List<Filter> filters, RequestSpecification requestSpecification, boolean urlEncodingEnabled, RestAssuredConfig config, ProxySpecification proxy) {
        this.threadId = threadId;
        this.baseURI = baseURI;
        this.port = port;
        this.basePath = basePath;
        this.authentication = authentication;
        this.filters = filters;
        this.requestSpecification = requestSpecification;
        this.urlEncodingEnabled = urlEncodingEnabled;
        this.config = config;
        this.proxy = proxy;
    }

    public static RequestContext defaultRequestContext = new RequestContext(
            0L,
            RestAssured.baseURI,
            RestAssured.port,
            RestAssured.basePath,
            RestAssured.authentication,
            RestAssured.filters(),
            RestAssured.requestSpecification,
            RestAssured.urlEncodingEnabled,
            RestAssured.config(),
            RestAssured.proxy
    );

    public static ThreadLocal<RequestContext> requestContext = new ThreadLocal<RequestContext>();

    public static void setContext(RequestContext context) {
        requestContext.set(context);
    }

}
