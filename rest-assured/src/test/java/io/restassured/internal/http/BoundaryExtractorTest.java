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

import static org.assertj.core.api.Assertions.assertThat;

class BoundaryExtractorTest {

    @Test
    void extractsBoundary() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("multipart/form-data; boundary=abc123")).isEqualTo("abc123");
    }

    @Test
    void extractsQuotedBoundary() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("multipart/mixed; charset=UTF-8; boundary=\"abc 123\"")).isEqualTo("abc 123");
    }

    @Test
    void extractsBoundaryIgnoringCaseOfParameterName() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("multipart/form-data; BOUNDARY=AbC")).isEqualTo("AbC");
    }

    @Test
    void extractsBoundaryFromPlusMultipartContentType() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("application/vnd.x+multipart+json;boundary=xyz")).isEqualTo("xyz");
    }

    @Test
    void returnsNullWhenBoundaryIsMissing() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("multipart/form-data; charset=UTF-8")).isNull();
    }

    @Test
    void returnsNullWhenContentTypeIsNull() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType(null)).isNull();
    }

    @Test
    void returnsNullWhenContentTypeIsEmpty() {
        assertThat(BoundaryExtractor.getBoundaryFromContentType("")).isNull();
    }

    @Test
    void returnsNullWhenBoundaryContainsEqualsSign() {
        // Quirk: boundaries containing "=" are valid per RFC 2046 but are not extracted
        assertThat(BoundaryExtractor.getBoundaryFromContentType("multipart/form-data; boundary=\"a=b\"")).isNull();
    }
}
