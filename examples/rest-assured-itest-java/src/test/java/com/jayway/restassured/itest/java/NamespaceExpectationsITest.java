package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static com.jayway.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class NamespaceExpectationsITest extends WithJetty {

    // see http://stackoverflow.com/questions/8669766/namespace-handling-in-groovys-xmlslurper
    @Test public void
    takes_namespaces_into_account_when_correct_namespace_is_declared() {
        given().
                config(newConfig().xmlConfig(xmlConfig().declaredNamespace("ns", "http://localhost/"))).
        expect().
                body("bar.text()", equalTo("sudo make me a sandwich!")).
                body(":bar.text()", equalTo("sudo ")).
                body("ns:bar.text()", equalTo("make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    takes_namespaces_into_account_when_correct_namespace_is_declared_with_different_name() {
        given().
                config(newConfig().xmlConfig(xmlConfig().declaredNamespace("test", "http://localhost/"))).
        expect().
                body("bar.text()", equalTo("sudo make me a sandwich!")).
                body(":bar.text()", equalTo("sudo ")).
                body("test:bar.text()", equalTo("make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_no_namespace_is_declared() {
        expect().
                body("bar.text()", equalTo("sudo make me a sandwich!")).
                body(":bar.text()", equalTo("sudo make me a sandwich!")).
                body("ns:bar.text()", equalTo("sudo make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_no_namespace_is_declared_but_namespace_aware_is_set_to_true() {
        given().
                config(newConfig().xmlConfig(xmlConfig().namespaceAware(true))).
        expect().
                body("bar.text()", equalTo("sudo make me a sandwich!")).
                body(":bar.text()", equalTo("sudo make me a sandwich!")).
                body("ns:bar.text()", equalTo("sudo make me a sandwich!")).
        when().
                get("/namespace-example");
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_incorrect_namespace_is_declared() {
        given().
                config(newConfig().xmlConfig(xmlConfig().declaredNamespace("ns", "http://something.com"))).
        expect().
                body("bar.text()", equalTo("sudo make me a sandwich!")).
                body(":bar.text()", equalTo("sudo ")).
                body("ns:bar.text()", isEmptyString()).
        when().
                get("/namespace-example");
    }
}
