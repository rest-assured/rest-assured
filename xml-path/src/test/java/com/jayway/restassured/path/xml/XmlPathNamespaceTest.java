package com.jayway.restassured.path.xml;

import org.junit.Test;

import static com.jayway.restassured.path.xml.config.XmlPathConfig.xmlPathConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlPathNamespaceTest {

    @Test public void
    xml_path_supports_namespaces_when_declared_correctly() {
        // Given
        String xml = "<foo xmlns:ns=\"http://localhost/\">\n" +
                "      <bar>sudo </bar>\n" +
                "      <ns:bar>make me a sandwich!</ns:bar>\n" +
                "    </foo>";

        // When
        XmlPath xmlPath = new XmlPath(xml).using(xmlPathConfig().declaredNamespace("ns", "http://localhost/"));

        // Then
        assertThat(xmlPath.getString("bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString(":bar.text()"), equalTo("sudo "));
        assertThat(xmlPath.getString("ns:bar.text()"), equalTo("make me a sandwich!"));
    }

    @Test public void
    xml_path_doesnt_support_namespaces_when_not_declared() {
        // Given
        String xml = "<foo xmlns:ns=\"http://localhost/\">\n" +
                "      <bar>sudo </bar>\n" +
                "      <ns:bar>make me a sandwich!</ns:bar>\n" +
                "    </foo>";

        // When
        XmlPath xmlPath = new XmlPath(xml);

        // Then
        assertThat(xmlPath.getString("bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString(":bar.text()"), equalTo("sudo make me a sandwich!"));
        assertThat(xmlPath.getString("ns:bar.text()"), equalTo("sudo make me a sandwich!"));
    }
}
