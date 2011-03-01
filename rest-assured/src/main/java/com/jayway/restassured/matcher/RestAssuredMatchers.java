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

public class RestAssuredMatchers {

    public static Matcher<Boolean> matchesXsd(String xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    public static Matcher<Boolean> matchesXsd(InputStream xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    public static Matcher<Boolean> matchesXsd(Reader xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    public static Matcher<Boolean> matchesXsd(File xsd) {
        return XmlXsdMatcher.matchesXsd(xsd);
    }

    public static Matcher<Boolean> matchesDtd(String dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    public static Matcher<Boolean> matchesDtd(InputStream dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    public static Matcher<Boolean> matchesDtd(File dtd) {
        return XmlDtdMatcher.matchesDtd(dtd);
    }

    public static Matcher<Boolean> matchesDtd(URL url) {
        return XmlDtdMatcher.matchesDtd(url);
    }
}