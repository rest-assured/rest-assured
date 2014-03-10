package com.jayway.restassured.itest.java;

import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenXsdITest extends WithJetty {

    @Test
    public void validatesXsdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd));
    }

    @Test
    public void validatesXsdInputStream() throws Exception {
        InputStream inputstream = load("car-records.xsd");

        get("/carRecords").then().body(matchesXsd(inputstream));
    }

    @Test
    public void validatesXsdStringAndPath() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd)).and().body("records.car.find { it.@name == 'HSV Maloo' }.@year", equalTo("2006"));
    }

    @Test
    public void possibleToSpecifyAResourceResolverWhenMatchingXsd() throws IOException {
        given().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody(load("sample.xml")).build();
                    }
                }).
        when().
                get("/somewhere").
        then().
                body(matchesXsd(load("main.xsd")).with(new ClasspathResourceResolver()));
    }

    private static InputStream load(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    private static class ClasspathResourceResolver implements LSResourceResolver {

        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            return new DOMInputImpl(publicId, systemId, baseURI, load(systemId), null);
        }
    }
}