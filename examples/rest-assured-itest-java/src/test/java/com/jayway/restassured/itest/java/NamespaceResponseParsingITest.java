package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.path.xml.XmlPath;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static com.jayway.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class NamespaceResponseParsingITest extends WithJetty {

    @Test public void
    takes_namespaces_into_account_when_correct_namespace_is_declared() {
        XmlPath xmlPath =
        given().
                config(newConfig().xmlConfig(xmlConfig().declaredNamespace("ns", "http://localhost/"))).
        when().
                get("/namespace-example").xmlPath();

        assertThat(xmlPath.getString("bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString(":bar.text()"), equalTo("sudo "));
        assertThat(xmlPath.getString("ns:bar.text()"), equalTo("make me a sandwich!"));
    }

    @Test public void
    doesnt_take_namespaces_into_account_when_no_namespace_is_declared() {
        XmlPath xmlPath = get("/namespace-example").xmlPath();

        assertThat(xmlPath.getString("bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString(":bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString("ns:bar.text()"), equalTo("sudo make me a sandwich!"));
    }
}
