package com.jayway.restassured.path.xml;

import com.jayway.restassured.path.xml.config.XmlPathConfig;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlPathCharsetTest {

    private static final String GREETING_WITH_STRANGE_CHARS = "<greeting><firstName>€%#åö</firstName>\n" +
            "      <lastName>`ü</lastName>\n" +
            "    </greeting>";


    @Test public void
    xml_path_supports_deserializing_input_stream_using_with_given_charset() throws UnsupportedEncodingException {
        // Given
        InputStream is = new ByteArrayInputStream(GREETING_WITH_STRANGE_CHARS.getBytes("UTF-16"));
        XmlPath xmlPath = new XmlPath(is).using(new XmlPathConfig("UTF-16"));

        // When
        final String firstName = xmlPath.getString("greeting.firstName");
        final String lastName = xmlPath.getString("greeting.lastName");

        // Then
        assertThat(firstName, equalTo("€%#åö"));
        assertThat(lastName, equalTo("`ü"));
    }

    @Test public void
    xml_path_cannot_correctly_deserialize_input_stream_using_wrong_charset() throws UnsupportedEncodingException {
        // Given
        InputStream is = new ByteArrayInputStream(GREETING_WITH_STRANGE_CHARS.getBytes("US-ASCII"));
        XmlPath xmlPath = new XmlPath(is).using(new XmlPathConfig("ISO-8859-1"));

        // When
        final String firstName = xmlPath.getString("greeting.firstName");
        final String lastName = xmlPath.getString("greeting.lastName");

        // Then
        assertThat(firstName, equalTo("?%#??"));
        assertThat(lastName, equalTo("`?"));
    }

}
