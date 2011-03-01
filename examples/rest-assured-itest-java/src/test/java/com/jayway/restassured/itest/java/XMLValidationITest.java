/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesDtd;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;

public class XMLValidationITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void validatesXsdInputStream() throws Exception {
        final InputStream xsd = getClass().getResourceAsStream("/car-records.xsd");

        expect().body(matchesXsd(xsd)).when().get("/carRecords");
    }

    @Test
    public void throwsExceptionWhenXsdValidationFails() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("Cannot find the declaration of element 'shopping'.");

        final InputStream xsd = getClass().getResourceAsStream("/car-records.xsd");

        expect().body(matchesXsd(xsd)).when().get("/shopping");
    }

    @Test
    public void validatesDtdInputStream() throws Exception {
        final InputStream dtd = getClass().getResourceAsStream("/videos.dtd");

        expect().body(matchesDtd(dtd)).when().get("/videos");
    }

    @Test
    public void throwsExceptionWhenDtdValidationFails() throws Exception {
        exception.expect(SAXParseException.class);
        exception.expectMessage("Element type \"shopping\" must be declared.");

        final InputStream dtd = getClass().getResourceAsStream("/videos.dtd");

        expect().body(matchesDtd(dtd)).when().get("/shopping");
    }


}
