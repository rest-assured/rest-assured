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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static io.restassured.RestAssured.expect;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XMLValidationITest extends WithJetty {

    private static final Locale INITIAL_LOCALE = Locale.getDefault();

    @BeforeAll
    public static void setUpBeforeClass(){
      Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    public static void tearDownAfterClass(){
      Locale.setDefault(INITIAL_LOCALE);
    }

    @Test
    void validatesXsdInputStream() throws Exception {
        try (InputStream xsd = getClass().getResourceAsStream("/car-records.xsd")) {
            expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/carRecords");
        }
    }

    @Test
    void validatesXsdString() throws Exception {
        try (InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd")) {
            final String xsd = IOUtils.toString(inputstream, StandardCharsets.UTF_8);
            expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/carRecords");
        }
    }

    @Test
    void throwsExceptionWhenXsdValidationFails() throws Exception {
        try (InputStream xsd = getClass().getResourceAsStream("/car-records.xsd")) {
            assertThatThrownBy(() ->
                expect().body(RestAssuredMatchers.matchesXsd(xsd)).when().get("/shopping")
            )
            .isInstanceOf(SAXParseException.class)
            .hasMessageContaining("Cannot find the declaration of element 'shopping'.");
        }
    }

    @Test
    void validatesDtdInputStream() throws Exception {
        try (InputStream dtd = getClass().getResourceAsStream("/videos.dtd")) {
            expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/videos");
        }
    }

    @Test
    void validatesDtdString() throws Exception {
        try (InputStream inputstream = getClass().getResourceAsStream("/videos.dtd")) {
            final String dtd = IOUtils.toString(inputstream, StandardCharsets.UTF_8);
            expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/videos");
        }
    }

    @Test
    void validatesDtdStringInClasspathWhenPathStartsWithSlash() {
        expect().body(RestAssuredMatchers.matchesDtdInClasspath("/videos.dtd")).when().get("/videos");
    }

    @Test
    void validatesDtdStringInClasspathWhenPathDoesntStartsWithSlash() {
        expect().body(RestAssuredMatchers.matchesDtdInClasspath("videos.dtd")).when().get("/videos");
    }

    @Test
    void throwsExceptionWhenDtdValidationFails() throws Exception {
        try (InputStream dtd = getClass().getResourceAsStream("/videos.dtd")) {
            assertThatThrownBy(() ->
                expect().body(RestAssuredMatchers.matchesDtd(dtd)).when().get("/shopping")
            )
            .isInstanceOf(SAXParseException.class)
            .hasMessageContaining("Element type \"shopping\" must be declared.");
        }
    }

    @Test
    void throwsExceptionWhenUsingDtdForXsd() throws Exception {
        try (InputStream dtd = getClass().getResourceAsStream("/videos.dtd")) {
            assertThatThrownBy(() ->
                expect().body(RestAssuredMatchers.matchesXsd(dtd)).when().get("/carRecords")
            )
            .isInstanceOf(SAXParseException.class)
            .hasMessageContaining("The markup in the document preceding the root element must be well-formed.");
        }
    }

    @Test
    void throwsExceptionWhenUsingXsdForDtd() throws Exception {
        try (InputStream xsd = getClass().getResourceAsStream("/car-records.xsd")) {
            assertThatThrownBy(() ->
                expect().body(RestAssuredMatchers.matchesDtd(xsd)).when().get("/videos")
            )
            .isInstanceOf(SAXParseException.class)
            .hasMessageContaining("The markup declarations contained or pointed to by the document type declaration must be well-formed.");
        }
    }
}
