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

package io.restassured.internal.support;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PathSupportTest {

    @Test public void
    returns_slash_string_when_fully_qualified_uri_doesnt_have_path_but_have_port() {
        // Given
        String targetUri = "http://localhost:8080";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/"));
    }

    @Test public void
    returns_slash_string_when_fully_qualified_uri_doesnt_have_path_and_no_port() {
        // Given
        String targetUri = "http://localhost";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/"));
    }

    @Test public void
    returns_uri_as_is_when_uri_is_a_path_starting_with_slash() {
        // Given
        String targetUri = "/path";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    adds_slash_to_path_when_uri_is_a_path_not_starting_with_slash() {
        // Given
        String targetUri = "path";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    removes_query_parameters_from_uri_when_uri_is_not_fully_qualified() {
        // Given
        String targetUri = "path?q=r&u=2";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    removes_query_params_form_fully_qualified_uri_with_path() {
        // Given
        String targetUri = "http://localhost:808/path?u=4";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    removes_query_params_form_uri_without_port_but_with_path() {
        // Given
        String targetUri = "http://localhost/path?u=4";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    returns_slash_when_path_is_undefined_for_fully_qualified_uri() {
        // Given
        String targetUri = "http://localhost?u=4";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/"));
    }
    @Test public void
    returns_path_when_param_has_scheme() {
        // Given
        String targetUri = "/path?uri=http://localhost";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }
    @Test public void
    returns_path_when_path_is_fully_qualified_uri_and_param_has_scheme() {
        // Given
        String targetUri = "http://localhost/path?uri=http://localhost";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/path"));
    }

    @Test public void
    returns_slash_when_fully_qualified_uri_has_no_path_and_query_param_has_url() {
        // Given
        String targetUri = "https://example.com?redirect=https://example.com/callback";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/"));
    }

    @Test public void
    correctly_identifies_fully_qualified_uri_when_no_path_and_query_param_has_url() {
        assertThat(PathSupport.isFullyQualified("https://example.com?redirect=https://example.com/callback"), is(true));
    }

    @Test public void
    correctly_identifies_non_fully_qualified_uri_when_query_param_has_url() {
        assertThat(PathSupport.isFullyQualified("example.com?redirect=https://example.com/callback"), is(false));
    }

    @Test public void
    returns_real_path_when_fully_qualified_uri_has_path_and_query_param_has_url() {
        // Given
        String targetUri = "https://example.com/real/path?redirect=https://other.com/cb";

        // When
        String path = PathSupport.getPath(targetUri);

        // Then
        assertThat(path, is("/real/path"));
    }

    @Test public void
    correctly_identifies_fully_qualified_uri_when_path_and_query_param_has_url() {
        assertThat(PathSupport.isFullyQualified("https://example.com/real/path?redirect=https://other.com/cb"), is(true));
    }

    @Test public void
    is_fully_qualified_only_when_the_scheme_comes_before_the_first_slash() {
        assertThat(PathSupport.isFullyQualified(null), is(false));
        assertThat(PathSupport.isFullyQualified(" "), is(false));
        assertThat(PathSupport.isFullyQualified("/path/http://example.com"), is(false));
        assertThat(PathSupport.isFullyQualified("http://example.com"), is(true));
    }

    @Test public void
    get_path_returns_blank_uris_as_they_are() {
        assertThat(PathSupport.getPath(null), is((String) null));
        assertThat(PathSupport.getPath(" "), is(" "));
        assertThat(PathSupport.getPath("path?x=y"), is("/path"));
    }

    @Test public void
    merge_and_remove_double_slash_joins_two_parts_with_exactly_one_slash() {
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a/", "/b"), is("a/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a/", "//b"), is("a//b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a/", "b"), is("a/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a", "/b"), is("a/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a", "b"), is("a/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash(" a ", " b "), is("a/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("http://localhost:8080/", "/b?c=d"), is("http://localhost:8080/b?c=d"));
    }

    @Test public void
    merge_and_remove_double_slash_with_empty_parts() {
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a", ""), is("a"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("", "b"), is("b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash(" ", " "), is("/"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a/", ""), is("a/"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("", "/b"), is("/b"));
    }

    @Test public void
    merge_and_remove_double_slash_drops_the_first_part_when_it_ends_with_slash_and_the_second_is_fully_qualified() {
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a/", "http://example.com/b"), is("http://example.com/b"));
        assertThat(PathSupport.mergeAndRemoveDoubleSlash("a", "http://example.com/b"), is("a/http://example.com/b"));
    }

    @Test public void
    merge_and_remove_double_slash_does_not_accept_null() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> PathSupport.mergeAndRemoveDoubleSlash(null, "b"));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> PathSupport.mergeAndRemoveDoubleSlash("a", null));
    }
}
