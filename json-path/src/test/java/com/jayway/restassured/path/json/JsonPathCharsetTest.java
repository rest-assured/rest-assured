package com.jayway.restassured.path.json;

import com.jayway.restassured.path.json.config.JsonPathConfig;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class JsonPathCharsetTest {

    private static final String GREETING_WITH_STRANGE_CHARS =  "{ \"greeting\" : { \n" +
          "                \"firstName\" : \"€%#åö\", \n" +
          "                \"lastName\" : \"`ü\" \n" +
          "               }\n" +
          "}";

    @Test public void
    json_path_supports_deserializing_input_stream_using_with_given_charset() throws UnsupportedEncodingException {
        // Given
        InputStream is = new ByteArrayInputStream(GREETING_WITH_STRANGE_CHARS.getBytes("UTF-8"));
        JsonPath jsonPath = new JsonPath(is).using(new JsonPathConfig("UTF-8"));

        // When
        final String firstName = jsonPath.getString("greeting.firstName");
        final String lastName = jsonPath.getString("greeting.lastName");

        // Then
        assertThat(firstName, equalTo("€%#åö"));
        assertThat(lastName, equalTo("`ü"));
    }

    @Test public void
    json_path_cannot_correctly_deserialize_input_stream_using_wrong_charset() throws IOException {
        // Given
        InputStream is = new ByteArrayInputStream(GREETING_WITH_STRANGE_CHARS.getBytes("US-ASCII"));
        JsonPath jsonPath = new JsonPath(is).using(new JsonPathConfig("ISO-8859-1"));

        // When
        final String firstName = jsonPath.getString("greeting.firstName");
        final String lastName = jsonPath.getString("greeting.lastName");

        // Then
        assertThat(firstName, containsString("?%#??"));
        assertThat(lastName, containsString("`?"));
    }

}
