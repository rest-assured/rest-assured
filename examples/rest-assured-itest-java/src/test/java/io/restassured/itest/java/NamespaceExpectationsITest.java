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

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEmptyString.emptyString;

public class NamespaceExpectationsITest extends WithJetty {

    // see http://stackoverflow.com/questions/8669766/namespace-handling-in-groovys-xmlslurper
    @Test public void
    takes_namespaces_into_account_when_correct_namespace_is_declared() {
        given().
                config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().declareNamespace("ns", "http://localhost/"))).
        expect().
                body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                body(":foo.:bar.text()", equalTo("sudo ")).
                body("foo.ns:bar.text()", equalTo("make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    takes_namespaces_into_account_when_correct_namespace_is_declared_with_different_name() {
        given().
                config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().declareNamespace("test", "http://localhost/"))).
        expect().
                body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                body(":foo.:bar.text()", equalTo("sudo ")).
                body("foo.test:bar.text()", equalTo("make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_no_namespace_is_declared() {
        expect().
                body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                body(":foo.:bar.text()", equalTo("sudo ")).
                body("foo.ns:bar.text()", equalTo("")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_no_namespace_is_declared_but_namespace_aware_is_set_to_true() {
        given().
                config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().namespaceAware(true))).
        expect().
                body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                body(":foo.:bar.text()", equalTo("sudo ")).
                body("foo.ns:bar.text()", equalTo("")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_incorrect_namespace_is_declared() {
        given().
                config(RestAssuredConfig.newConfig().xmlConfig(xmlConfig().declareNamespace("ns", "http://something.com"))).
        expect().
                body("foo.bar.text()", equalTo("sudo make me a sandwich!")).
                body(":foo.:bar.text()", equalTo("sudo ")).
                body("foo.ns:bar.text()", emptyString()).
        when().
                get("/namespace-example");
    }
}
