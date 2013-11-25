/*
 * Copyright 2013 the original author or authors.
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



package com.jayway.restassured.assertion

import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.internal.ResponseParserRegistrar
import com.jayway.restassured.response.Response
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilderFactory

import static org.apache.commons.lang3.StringUtils.*

class BodyMatcher {
    private static final String XPATH = "XPath"
    def key
    def Matcher matcher
    def ResponseParserRegistrar rpr

    def validate(Response response, content, RestAssuredConfig config) {
        def success = true
        def errorMessage = "";

        content = fallbackToResponseBodyIfContentHasAlreadyBeenRead(response, content)
        if (key == null) {
            if (isXPathMatcher()) {
                def xmlConfig = config.getXmlConfig();
                boolean namespaceAware = xmlConfig.isNamespaceAware()
                def Map<String, Boolean> features = xmlConfig.features()

                def factory = DocumentBuilderFactory.newInstance()
                factory.setNamespaceAware(namespaceAware)
                if (!features.isEmpty()) {
                    features.each { featureName, isEnabled ->
                        factory.setFeature(featureName, isEnabled)
                    }
                }

                Element node = factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.asByteArray())).getDocumentElement();
                if (!matcher.matches(node)) {
                    success = false
                    errorMessage = String.format("Expected: %s\n  Actual: %s\n", trim(matcher.toString()), content)
                }
            } else if (!matcher.matches(response.asString())) {
                success = false
                errorMessage = "Response body doesn't match expectation.\nExpected: $matcher\n  Actual: $content\n"
            }
        } else {
            def assertion = StreamVerifier.newAssertion(response, key, rpr)
            def result = null
            if (content != null) {
                result = assertion.getResult(content, config)
            }

            if (!matcher.matches(result)) {
                success = false
                if (result instanceof Object[]) {
                    result = result.join(",")
                }
                errorMessage = String.format("%s %s doesn't match.\nExpected: %s\n  Actual: %s\n", assertion.description(), key, removeQuotesIfString(matcher.toString()), result)
            }
        }
        return [success: success, errorMessage: errorMessage];
    }

    private String removeQuotesIfString(String string) {
        if (startsWith(string, "\"") && endsWith(string, "\"")) {
            def start = removeStart(string, "\"")
            string = removeEnd(start, "\"")
        }
        string
    }

    def fallbackToResponseBodyIfContentHasAlreadyBeenRead(Response response, content) {
        if (content instanceof Reader || content instanceof InputStream) {
            return response.asString()
        }
        return content
    }

    private boolean isXPathMatcher() {
        def isNestedMatcherContainingXPathMatcher = {
            def description = new StringDescription()
            matcher.describeTo(description)
            description.toString().contains(XPATH)
        }

        matcher instanceof HasXPath || isNestedMatcherContainingXPathMatcher()
    }

    def boolean requiresTextParsing() {
        isXPathMatcher() || key == null
    }

    def String getDescription() {
        String description
        if (key) {
            description = "Body containing expression \"$key\" must match $matcher"
        } else {
            description = "Body must match $matcher"
        }
        return description
    }
}
