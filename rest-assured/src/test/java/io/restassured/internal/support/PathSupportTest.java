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


import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
}