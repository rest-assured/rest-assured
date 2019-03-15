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
import org.junit.Test;

import javax.xml.namespace.NamespaceContext;
import java.util.Arrays;
import java.util.Iterator;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.*;

public class GivenWhenThenNamespaceITest extends WithJetty {

    @Test public void
    can_use_gpath_with_namespace_using_given_when_then_api() {
        given().
                  config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().declareNamespace("ns", "http://localhost/"))).
        when().
                  get("/namespace-example").
        then().
                  body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                  body(":foo.:bar.text()", equalTo("sudo ")).
                  body(":foo.ns:bar.text()", equalTo("make me a sandwich!"));
    }

    @Test public void
    can_use_hamcrest_xpath_matcher_with_namespace_using_given_when_then_api() {
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
        when().
                get("/package-db-xml").
        then().
                body(hasXPath("/db:package-database", namespaceContext));
    }

    @Test public void
    can_use_not_in_hamcrest_xpath_matcher_with_namespace_using_given_when_then_api() {
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
        when().
                get("/package-db-xml").
        then().
                body(not(hasXPath("/db:package-database21", namespaceContext)));
    }

    @Test public void
    can_validate_namespace_attributes_when_namespace_aware_is_set_to_false() {
        given().
                config(config().xmlConfig(xmlConfig().namespaceAware(false))).
        when().
                get("/namespace-example2").
        then().
                body("soapenv:Envelope.soapenv:Body.ns1:getBankResponse.@xmlns:ns1", equalTo("http://thomas-bayer.com/blz/"));
    }
}
