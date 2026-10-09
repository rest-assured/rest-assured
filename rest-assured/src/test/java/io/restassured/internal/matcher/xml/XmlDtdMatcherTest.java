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
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlDtdMatcherTest {

    private static final String VALID_XML = "<greeting><name>John</name></greeting>";
    private static final String INVALID_XML = "<greeting><other/></greeting>";

    @TempDir
    Path tempDir;

    @Test public void
    matches_xml_against_dtd_given_as_string() throws Exception {
        assertThat(XmlDtdMatcher.matchesDtd(dtd()).matches(VALID_XML)).isTrue();
    }

    @Test public void
    throws_sax_parse_exception_when_xml_does_not_match_dtd() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtd());

        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    throws_sax_parse_exception_when_xml_is_malformed() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtd());

        assertThatThrownBy(() -> matcher.matches("<greeting>")).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    matches_xml_against_dtd_given_as_input_stream() throws Exception {
        InputStream dtd = new ByteArrayInputStream(dtd().getBytes(StandardCharsets.UTF_8));

        assertThat(XmlDtdMatcher.matchesDtd(dtd).matches(VALID_XML)).isTrue();
    }

    @Test public void
    matches_xml_against_dtd_given_as_file() throws Exception {
        assertThat(XmlDtdMatcher.matchesDtd(dtdFile()).matches(VALID_XML)).isTrue();
    }

    @Test public void
    matches_xml_against_dtd_given_as_url() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtdFile().toURI().toURL());

        assertThat(matcher.matches(VALID_XML)).isTrue();
    }

    @Test public void
    dtd_given_as_url_is_read_for_every_match() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtdFile().toURI().toURL());

        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(INVALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    dtd_given_as_string_is_read_by_the_first_match_only() throws Exception {
        // Known quirk: the dtd is kept as an InputStream, which the first match consumes, so later matches validate against an empty DTD
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtd());

        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(VALID_XML)).isExactlyInstanceOf(SAXParseException.class);
    }

    @Test public void
    dtd_given_as_file_is_read_by_the_first_match_only() throws Exception {
        // Known quirk: the file is opened when the matcher is created and closed by the first match
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtdFile());

        assertThat(matcher.matches(VALID_XML)).isTrue();
        assertThatThrownBy(() -> matcher.matches(VALID_XML)).isExactlyInstanceOf(IOException.class).hasMessage("Stream Closed");
    }

    @Test public void
    matches_xml_against_dtd_in_classpath_with_and_without_leading_slash() throws Exception {
        assertThat(XmlDtdMatcher.matchesDtdInClasspath("xml-matchers/greeting.dtd").matches(VALID_XML)).isTrue();
        assertThat(XmlDtdMatcher.matchesDtdInClasspath("/xml-matchers/greeting.dtd").matches(VALID_XML)).isTrue();
    }

    @Test public void
    throws_file_not_found_exception_when_dtd_file_does_not_exist() {
        File missing = tempDir.resolve("missing.dtd").toFile();

        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtd(missing)).isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test public void
    matches_gstring() throws Exception {
        Object gString = new GStringImpl(new Object[0], new String[]{VALID_XML});

        assertThat(XmlDtdMatcher.matchesDtd(dtd()).matches(gString)).isTrue();
    }

    @Test public void
    throws_npe_when_matching_null() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtd());

        assertThatThrownBy(() -> matcher.matches(null)).isInstanceOf(NullPointerException.class);
    }

    @Test public void
    throws_illegal_argument_exception_when_matching_something_that_is_not_a_string() throws Exception {
        Matcher<String> matcher = XmlDtdMatcher.matchesDtd(dtd());

        assertThatThrownBy(() -> matcher.matches(42))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The DTD matcher can only match a String but was java.lang.Integer.");
    }

    @Test public void
    describes_itself_as_the_supplied_dtd() throws Exception {
        assertThat(StringDescription.toString(XmlDtdMatcher.matchesDtd(dtd()))).isEqualTo("the supplied DTD");
    }

    @Test public void
    rejects_null_arguments() {
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtd((String) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("dtd cannot be null");
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtd((InputStream) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("dtd cannot be null");
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtd((File) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("file cannot be null");
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtd((URL) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("url cannot be null");
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtdInClasspath(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Path that points to the DTD in classpath cannot be null");
    }

    @Test public void
    rejects_dtd_that_is_not_in_classpath() {
        assertThatThrownBy(() -> XmlDtdMatcher.matchesDtdInClasspath("xml-matchers/missing.dtd")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Couldn't find the DTD \"xml-matchers/missing.dtd\" in classpath.");
    }

    private static String dtd() throws Exception {
        try (InputStream stream = XmlDtdMatcherTest.class.getResourceAsStream("/xml-matchers/greeting.dtd")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private File dtdFile() throws Exception {
        Path file = tempDir.resolve("greeting.dtd");
        Files.write(file, dtd().getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }
}
