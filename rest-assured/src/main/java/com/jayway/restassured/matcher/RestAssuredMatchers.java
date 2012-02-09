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

package com.jayway.restassured.matcher;

import com.jayway.restassured.internal.matcher.xml.XmlDtdMatcher;
import com.jayway.restassured.internal.matcher.xml.XmlXsdMatcher;
import org.hamcrest.Matcher;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * Providers Hamcrest matchers that may be useful when validating a response.
 */
public class RestAssuredMatchers {

    /**
     * Evaluates to true if an XML string matches the supplied XSD (Xml Schema).
     *
     * @param xsd The XSD to match
     * @return The XSD matcher
     */
    public static Matcher<String> matchesXsd(String xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied XSD (Xml Schema).
     *
     * @param xsd The XSD to match
     * @return The XSD matcher
     */
    public static Matcher<String> matchesXsd(InputStream xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied XSD (Xml Schema).
     *
     * @param xsd The XSD to match
     * @return The XSD matcher
     */
    public static Matcher<String> matchesXsd(Reader xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied XSD (Xml Schema).
     *
     * @param xsd The XSD to match
     * @return The XSD matcher
     */
    public static Matcher<String> matchesXsd(File xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied DTD.
     *
     * @param dtd The DTD to match
     * @return The DTD matcher
     */
    public static Matcher<String> matchesDtd(String dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied DTD.
     *
     * @param dtd The DTD to match
     * @return The DTD matcher
     */
    public static Matcher<String> matchesDtd(InputStream dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied DTD.
     *
     * @param dtd The DTD to match
     * @return The DTD matcher
     */
    public static Matcher<String> matchesDtd(File dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    /**
     * Evaluates to true if an XML string matches the supplied DTD.
     *
     * @param url The DTD to match
     * @return The DTD matcher
     */
    public static Matcher<String> matchesDtd(URL url) {
        return XmlDtdMatcher.matchesDtd(url);
    }
}