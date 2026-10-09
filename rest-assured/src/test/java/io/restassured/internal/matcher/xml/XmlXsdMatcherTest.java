/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.matcher.xml;

import org.codehaus.groovy.runtime.GStringImpl;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlXsdMatcherTest {

    private static final String VALID_XML = "<greeting><name>John</name></greeting>";
    private static final String INVALID_XML = "<greeting><other/></greeting>";

    private static final String MAIN_XSD_WITH_INCLUDE = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <xs:include schemaLocation=\"greeting-type.xsd\"/>\n" +
            "  <xs:element name=\"greeting\" type=\"greetingType\"/>\n" +
            "</xs:schema>";

    private static final String INCLUDED_XSD = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "  <xs:complexType name=\"greetingType\">\n" +
            "    <xs:sequence><xs:element name=\"name\" type=\"xs:string\"/></xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>";

    @TempDir
    Path tempDir;

    @Test public void
    matches_xml_against_xsd_given_as_string() throws Exception {
        assertThat(XmlXsdMatcher.matchesXsd(xsd()).matches(VALID_XML)).isTrue();
    }

    @Test public void
    trims_xsd_given_as_string() throws Exception {
        // An XML declaration that isn't at the very start of the document is a fatal error, so this only works if the xsd is trimmed
        assertThat(XmlXsdMatcher.matchesXsd("\n   " + xsd() + "\n  ").matches(VALID_XML)).isTrue();
    }

    @Test public void
    throws_sax_parse_exception_when_xml_does_not_match_xsd() throws Exception {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(xsd());

        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    throws_sax_parse_exception_when_xsd_is_invalid() {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element/></xs:schema>");

        assertThatThrownBy(() -> matcher.matches(VALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    matches_xml_against_xsd_given_as_input_stream() throws Exception {
        InputStream xsd = new ByteArrayInputStream(xsd().getBytes(StandardCharsets.UTF_8));

        assertThat(XmlXsdMatcher.matchesXsd(xsd).matches(VALID_XML)).isTrue();
    }

    @Test public void
    matches_xml_against_xsd_given_as_reader() throws Exception {
        assertThat(XmlXsdMatcher.matchesXsd(new StringReader(xsd())).matches(VALID_XML)).isTrue();
    }

    @Test public void
    matches_xml_against_xsd_given_as_file_more_than_once() throws Exception {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(xsdFile());

        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    matches_xml_against_xsd_given_as_url_more_than_once() throws Exception {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(xsdFile().toURI().toURL());

        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    matches_xml_against_xsd_given_as_string_more_than_once() throws Exception {
        assertMatchesMoreThanOnce(XmlXsdMatcher.matchesXsd(xsd()));
    }

    @Test public void
    matches_xml_against_xsd_given_as_input_stream_more_than_once() throws Exception {
        assertMatchesMoreThanOnce(XmlXsdMatcher.matchesXsd(new ByteArrayInputStream(xsd().getBytes(StandardCharsets.UTF_8))));
    }

    @Test public void
    matches_xml_against_xsd_given_as_reader_more_than_once() throws Exception {
        assertMatchesMoreThanOnce(XmlXsdMatcher.matchesXsd(new StringReader(xsd())));
    }

    @Test public void
    matches_xml_against_xsd_in_classpath_more_than_once() throws Exception {
        assertMatchesMoreThanOnce(XmlXsdMatcher.matchesXsdInClasspath("xml-matchers/greeting.xsd"));
    }

    @Test public void
    matches_xml_against_xsd_with_resource_resolver_more_than_once() throws Exception {
        LSResourceResolver resolver = (type, namespaceURI, publicId, systemId, baseURI) -> lsInput(INCLUDED_XSD);

        assertMatchesMoreThanOnce(XmlXsdMatcher.matchesXsd(MAIN_XSD_WITH_INCLUDE).using(resolver));
    }

    @Test public void
    matches_xml_against_xsd_in_classpath_with_and_without_leading_slash() throws Exception {
        assertThat(XmlXsdMatcher.matchesXsdInClasspath("xml-matchers/greeting.xsd").matches(VALID_XML)).isTrue();
        assertThat(XmlXsdMatcher.matchesXsdInClasspath("/xml-matchers/greeting.xsd").matches(VALID_XML)).isTrue();
    }

    @Test public void
    using_resource_resolver_resolves_included_schemas_and_returns_a_new_matcher() throws Exception {
        List<String> resolvedSystemIds = new ArrayList<>();
        LSResourceResolver resolver = (type, namespaceURI, publicId, systemId, baseURI) -> {
            resolvedSystemIds.add(systemId);
            return lsInput(INCLUDED_XSD);
        };
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(MAIN_XSD_WITH_INCLUDE);

        XmlXsdMatcher matcherWithResolver = matcher.using(resolver);

        assertThat(matcherWithResolver).isNotSameAs(matcher);
        assertThat(matcherWithResolver.getResourceResolver()).isSameAs(resolver);
        assertThat(matcherWithResolver.getXsd()).isSameAs(matcher.getXsd());
        assertThat(matcher.getResourceResolver()).isNull();
        assertThat(matcherWithResolver.matches(VALID_XML)).isTrue();
        assertThat(resolvedSystemIds).containsExactly("greeting-type.xsd");
    }

    @Test public void
    with_resource_resolver_is_an_alias_for_using() throws Exception {
        LSResourceResolver resolver = (type, namespaceURI, publicId, systemId, baseURI) -> lsInput(INCLUDED_XSD);

        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(MAIN_XSD_WITH_INCLUDE).with(resolver);

        assertThat(matcher.getResourceResolver()).isSameAs(resolver);
        assertThat(matcher.matches(VALID_XML)).isTrue();
    }

    @Test public void
    included_schema_is_not_resolved_without_resource_resolver() {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(MAIN_XSD_WITH_INCLUDE);

        assertThatThrownBy(() -> matcher.matches(VALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    matches_gstring() throws Exception {
        Object gString = new GStringImpl(new Object[0], new String[]{VALID_XML});

        assertThat(XmlXsdMatcher.matchesXsd(xsd()).matches(gString)).isTrue();
    }

    @Test public void
    throws_npe_when_matching_null() throws Exception {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(xsd());

        assertThatThrownBy(() -> matcher.matches(null)).isInstanceOf(NullPointerException.class);
    }

    @Test public void
    throws_illegal_argument_exception_when_matching_something_that_is_not_a_string() throws Exception {
        XmlXsdMatcher matcher = XmlXsdMatcher.matchesXsd(xsd());

        assertThatThrownBy(() -> matcher.matches(42))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The XSD matcher can only match a String but was java.lang.Integer.");
    }

    @Test public void
    describes_itself_as_the_supplied_xsd() throws Exception {
        assertThat(StringDescription.toString(XmlXsdMatcher.matchesXsd(xsd()))).isEqualTo("the supplied XSD");
    }

    @Test public void
    rejects_null_arguments() throws Exception {
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd((String) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("xsd cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd((InputStream) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("xsd cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd((java.io.Reader) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("xsd cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd((File) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("xsd cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd((java.net.URL) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("url cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsdInClasspath(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Path that points to the XSD in classpath cannot be null");
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsd(xsd()).using(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("LSResourceResolver cannot be null");
    }

    @Test public void
    rejects_xsd_that_is_not_in_classpath() {
        assertThatThrownBy(() -> XmlXsdMatcher.matchesXsdInClasspath("xml-matchers/missing.xsd")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Couldn't find the XSD \"xml-matchers/missing.xsd\" in classpath.");
    }

    private static void assertMatchesMoreThanOnce(XmlXsdMatcher matcher) {
        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    private static String xsd() throws Exception {
        try (InputStream stream = XmlXsdMatcherTest.class.getResourceAsStream("/xml-matchers/greeting.xsd")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private File xsdFile() throws Exception {
        Path file = tempDir.resolve("greeting.xsd");
        Files.write(file, xsd().getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    private static LSInput lsInput(String content) {
        try {
            DOMImplementationLS domImplementation = (DOMImplementationLS) DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            LSInput input = domImplementation.createLSInput();
            input.setStringData(content);
            return input;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
