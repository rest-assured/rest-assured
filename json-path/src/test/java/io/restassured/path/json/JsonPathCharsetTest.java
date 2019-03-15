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

package io.restassured.path.json;

import io.restassured.path.json.config.JsonPathConfig;
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
