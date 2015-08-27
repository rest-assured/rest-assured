/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.http;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentTypeTest {

    @Test public void
    content_type_with_charset_returns_the_content_type_with_the_given_charset() {
        // When
        final String contentType = ContentType.JSON.withCharset("UTF-8");

        // Then
        assertThat(contentType, equalTo("application/json; charset=UTF-8"));
    }

    @Test public void
    content_type_with_java_charset_returns_the_content_type_with_the_given_charset() {
        // When
        final String contentType = ContentType.JSON.withCharset(Charset.forName("ISO-8859-1"));

        // Then
        assertThat(contentType, equalTo("application/json; charset=ISO-8859-1"));
    }

    @Test public void
    content_type_matches_expected_content_type_using_ignore_case() {
        // Given
        final String expected = "appliCatIon/JSON";

        // When
        boolean matches = ContentType.JSON.matches(expected);

        // Then
        assertThat(matches, is(true));
    }

    @Test public void
    content_type_doesnt_match_when_expected_content_type_is_not_equal_to_actual() {
        // Given
        final String expected = "application/json2";

        // When
        boolean matches = ContentType.JSON.matches(expected);

        // Then
        assertThat(matches, is(false));
    }

    @Test public void
    content_type_doesnt_match_when_expected_content_type_is_null() {
        // Given
        final String expected = null;

        // When
        boolean matches = ContentType.JSON.matches(expected);

        // Then
        assertThat(matches, is(false));
    }
}
