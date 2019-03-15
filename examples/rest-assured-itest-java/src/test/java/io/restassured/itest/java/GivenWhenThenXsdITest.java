/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.itest.java;

import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.matchesXsd;
import static io.restassured.matcher.RestAssuredMatchers.matchesXsdInClasspath;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenXsdITest extends WithJetty {

    @Test
    public void validatesXsdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd));
    }

    @Test
    public void validatesXsdStringInClasspathWhenPathStartsWithSlash() throws Exception {
        get("/carRecords").then().body(matchesXsdInClasspath("/car-records.xsd"));
    }

    @Test
    public void validatesXsdStringInClasspathWhenPathDoesntStartWithSlash() throws Exception {
        get("/carRecords").then().body(matchesXsdInClasspath("car-records.xsd"));
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
            
            return new LSInput() {
                @Override
                public Reader getCharacterStream() {
                    return null;
                }

                @Override
                public void setCharacterStream(Reader characterStream) {

                }

                @Override
                public InputStream getByteStream() {
                    return load(systemId);
                }

                @Override
                public void setByteStream(InputStream byteStream) {

                }

                @Override
                public String getStringData() {
                    return null;
                }

                @Override
                public void setStringData(String stringData) {

                }

                @Override
                public String getSystemId() {
                    return systemId;
                }

                @Override
                public void setSystemId(String systemId) {

                }

                @Override
                public String getPublicId() {
                    return publicId;
                }

                @Override
                public void setPublicId(String publicId) {

                }

                @Override
                public String getBaseURI() {
                    return baseURI;
                }

                @Override
                public void setBaseURI(String baseURI) {

                }

                @Override
                public String getEncoding() {
                    return null;
                }

                @Override
                public void setEncoding(String encoding) {

                }

                @Override
                public boolean getCertifiedText() {
                    return false;
                }

                @Override
                public void setCertifiedText(boolean certifiedText) {

                }
            };
        }
    }
}