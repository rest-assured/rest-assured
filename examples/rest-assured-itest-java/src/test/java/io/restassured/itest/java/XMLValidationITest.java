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

import io.restassured.itest.java.support.WithJetty;
import io.restassured.matcher.RestAssuredMatchers;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.util.Locale;

import static io.restassured.RestAssured.expect;

public class XMLValidationITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final Locale INITIAL_LOCALE = Locale.getDefault();

    @BeforeClass
    public static void setUpBeforeClass(){
      Locale.setDefault(Locale.ENGLISH);
    }

    @AfterClass
    public static void tearDownAfterClass(){
      Locale.setDefault(INITIAL_LOCALE);
    }

    @Test
    public void validatesXsdInputStream() throws Exception {
        final InputStream xsd = getClass().getResourceAsStream("/car-records.xsd");

        expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/carRecords");
    }

    @Test
    public void validatesXsdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/carRecords");
    }

    @Test
    public void throwsExceptionWhenXsdValidationFails() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("Cannot find the declaration of element 'shopping'.");

        final InputStream xsd = getClass().getResourceAsStream("/car-records.xsd");

        expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/shopping");
    }

    @Test
    public void validatesDtdInputStream() throws Exception {
        final InputStream dtd = getClass().getResourceAsStream("/videos.dtd");

        expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/videos");
    }

    @Test
    public void validatesDtdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/videos.dtd");
        final String dtd = IOUtils.toString(inputstream);

        expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/videos");
    }

    @Test
    public void validatesDtdStringInClasspathWhenPathStartsWithSlash() throws Exception {
        expect().body(RestAssuredMatchers.matchesDtdInClasspath("/videos.dtd")).when().get("/videos");
    }

    @Test
    public void validatesDtdStringInClasspathWhenPathDoesntStartsWithSlash() throws Exception {
        expect().body(RestAssuredMatchers.matchesDtdInClasspath("videos.dtd")).when().get("/videos");
    }

    @Test
    public void throwsExceptionWhenDtdValidationFails() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("Element type \"shopping\" must be declared.");

        final InputStream dtd = getClass().getResourceAsStream("/videos.dtd");

        expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/shopping");
    }

    @Test
    public void throwsExceptionWhenUsingDtdForXsd() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("The markup in the document preceding the root element must be well-formed.");

        final InputStream dtd = getClass().getResourceAsStream("/videos.dtd");

        expect().body(RestAssuredMatchers.matchesXsd(dtd)).when().get("/carRecords");
    }

    @Test
    public void throwsExceptionWhenUsingXsdForDtd() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("The markup declarations contained or pointed to by the document type declaration must be well-formed.");

        final InputStream xsd = getClass().getResourceAsStream("/car-records.xsd");

        expect().body(RestAssuredMatchers.matchesDtd(xsd)).when().get("/videos");
    }
}
