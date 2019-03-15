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

import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.WithJetty;
import org.hamcrest.Matcher;
import org.junit.Test;

import javax.xml.namespace.NamespaceContext;
import java.util.Arrays;
import java.util.Iterator;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.*;

public class XPathITest extends WithJetty {

    @Test public void
    has_xpath_works_with_single_check() {
        // When
        expect().
                body(hasXPath("//userFavorite[@application-id='1']")).
        when().
                get("/user-favorite-xml");
    }

    @Test public void
    has_xpath_works_when_wrapped_in_not_matcher() {
        exception.expect(AssertionError.class);
        exception.expectMessage("not an XML document with XPath //userFavorite[@application-id='1']");

        // When
        expect().
                body(not(hasXPath("//userFavorite[@application-id='1']"))).
        when().
                get("/user-favorite-xml");
    }

    @Test public void
    has_xpath_works_when_wrapped_in_all_of_matcher() {
        // When
        expect().
                body(allOf(hasXPath("//userFavorite[@application-id='1']"), hasXPath("//userFavorite[@userData='someData']"))).
        when().
                get("/user-favorite-xml");
    }

    @Test public void
    has_xpath_works_when_wrapped_in_any_of_matcher() {
        // When
        expect().
                body(anyOf(hasXPath("//userFavorite[@application-id='1']"), not(hasXPath("//userFavorite[@application-id='1']")))).
        when().
                get("/user-favorite-xml");
    }

    @Test(expected = AssertionError.class) public void
    cant_mix_has_xpath_and_equal_to_matchers() {
        // When
        expect().
                body(allOf((Matcher) hasXPath("//userFavorite[@application-id='1']"), equalTo("//userFavorite[@application-id='1']"))).
        when().
                get("/user-favorite-xml");
    }

    @Test public void
    xpath_works_with_namespaces_when_xml_config_is_configured_to_be_namespace_aware() {
        // Given
        NamespaceContext namespaceContext = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return "http://marklogic.com/manage/package/databases";
            }

            public String getPrefix(String namespaceURI) {
                return "db";
            }

            public Iterator getPrefixes(String namespaceURI) {
                return Arrays.asList("db").iterator();
            }
        };

        // When
        given().
                config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().with().namespaceAware(true))).
        expect().
                body(hasXPath("/db:package-database", namespaceContext)).
        when().
                get("/package-db-xml");
    }
}
