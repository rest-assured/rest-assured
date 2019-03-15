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

package io.restassured.config;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HeaderConfigTest {

    @Test public void
    shouldOverwriteHeaderWithName_returns_true_when_applicable() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.overwriteHeadersWithName("header");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("header"), is(true));
    }

    @Test public void
    shouldOverwriteHeaderWithName_returns_false_when_applicable() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.overwriteHeadersWithName("header2");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("header"), is(false));
    }

    @Test public void
    shouldOverwriteHeaderWithName_is_case_insensitive() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.overwriteHeadersWithName("HeadEr");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("header"), is(true));
    }

    @Test public void
    shouldOverwriteHeaderWithName_returns_true_when_defining_multiple_headers_at_once() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.overwriteHeadersWithName("Header2", "header2", "heaDer1");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("header2"), is(true));
        assertThat(config.shouldOverwriteHeaderWithName("Header1"), is(true));
    }

    @Test public void
    content_type_header_is_overwritable_by_default() {
        HeaderConfig headerConfig = new HeaderConfig();

        assertThat(headerConfig.shouldOverwriteHeaderWithName("content-type"), is(true));
    }

    @Test public void
    accept_header_is_overwritable_by_default() {
        HeaderConfig headerConfig = new HeaderConfig();

        assertThat(headerConfig.shouldOverwriteHeaderWithName("content-type"), is(true));
    }

    @Test public void
    accept_header_is_mergeable_if_configured_accordingly() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.mergeHeadersWithName("Accept");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("accept"), is(false));
    }

    @Test public void
    content_type_header_is_mergeable_if_configured_accordingly() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.mergeHeadersWithName("Content-type");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("content-type"), is(false));
    }

    @Test public void
    mergeHeadersWithName_works_as_expected() {
        // Given
        HeaderConfig headerConfig = new HeaderConfig();

        // When
        HeaderConfig config = headerConfig.mergeHeadersWithName("Content-type", "accept");

        // Then
        assertThat(config.shouldOverwriteHeaderWithName("content-type"), is(false));
        assertThat(config.shouldOverwriteHeaderWithName("Accept"), is(false));
    }
}