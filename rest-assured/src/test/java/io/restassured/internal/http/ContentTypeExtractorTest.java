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

package io.restassured.internal.http;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ContentTypeExtractorTest {

    @Test public void
    returns_content_type_as_is_when_charset_is_undefined() {
        // Given
        String contentType = "application/json";

        // When
        String withoutCharset = ContentTypeExtractor.getContentTypeWithoutCharset(contentType);

        // Then
        assertThat(withoutCharset, equalTo("application/json"));
    }

    @Test public void
    removes_parameters_and_surrounding_whitespace() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset("  application/vnd.api+json ; charset=UTF-8; qs=0.5"), equalTo("application/vnd.api+json"));
    }

    @Test public void
    removes_trailing_semicolon() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset("application/xml;"), equalTo("application/xml"));
    }

    @Test public void
    keeps_case_of_content_type() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset("Application/JSON; Charset=UTF-8"), equalTo("Application/JSON"));
    }

    @Test public void
    returns_null_when_content_type_is_null() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset(null), nullValue());
    }

    @Test public void
    returns_empty_string_when_content_type_is_empty_or_blank() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset(""), equalTo(""));
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset("   "), equalTo(""));
    }

    @Test public void
    returns_empty_string_when_content_type_starts_with_semicolon() {
        assertThat(ContentTypeExtractor.getContentTypeWithoutCharset("; charset=UTF-8"), equalTo(""));
    }
}