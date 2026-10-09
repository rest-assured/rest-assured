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

import static io.restassured.internal.http.ContentTypeSubTypeExtractor.getSubTypeValueFromContentType;
import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeSubTypeExtractorTest {

    @Test
    void returnsNullWhenContentTypeIsNull() {
        assertThat(getSubTypeValueFromContentType(null, "charset")).isNull();
    }

    @Test
    void returnsNullWhenContentTypeIsEmpty() {
        assertThat(getSubTypeValueFromContentType("", "charset")).isNull();
    }

    @Test
    void returnsNullWhenContentTypeIsBlank() {
        assertThat(getSubTypeValueFromContentType("   ", "charset")).isNull();
    }

    @Test
    void returnsNullWhenSubTypeIsNull() {
        assertThat(getSubTypeValueFromContentType("application/json; charset=UTF-8", null)).isNull();
    }

    @Test
    void returnsNullWhenContentTypeHasNoParameters() {
        assertThat(getSubTypeValueFromContentType("application/json", "charset")).isNull();
    }

    @Test
    void returnsNullWhenSubTypeOnlyOccursOutsideOfAParameter() {
        assertThat(getSubTypeValueFromContentType("application/charset", "charset")).isNull();
    }

    @Test
    void returnsNullWhenSubTypeIsOnlyASuffixOfAnotherParameterName() {
        assertThat(getSubTypeValueFromContentType("application/json; xcharset=UTF-8", "charset")).isNull();
    }

    @Test
    void extractsValue() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=UTF-8", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void extractsValueWithoutSpaceAfterSemicolon() {
        assertThat(getSubTypeValueFromContentType("text/plain;charset=UTF-8", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void trimsWhitespaceAroundNameAndValue() {
        assertThat(getSubTypeValueFromContentType("text/plain ;  charset  =   UTF-8   ", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void matchesSubTypeNameIgnoringCaseAndKeepsCaseOfValue() {
        assertThat(getSubTypeValueFromContentType("text/plain; CHARSET=utf-8", "ChArSeT")).isEqualTo("utf-8");
    }

    @Test
    void removesSurroundingDoubleQuotes() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=\"UTF-8\"", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void removesUnbalancedLeadingDoubleQuote() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=\"UTF-8", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void removesUnbalancedTrailingDoubleQuote() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=UTF-8\"", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void keepsWhitespaceInsideDoubleQuotes() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=\" UTF-8 \"", "charset")).isEqualTo(" UTF-8 ");
    }

    @Test
    void keepsSingleQuotes() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset='UTF-8'", "charset")).isEqualTo("'UTF-8'");
    }

    @Test
    void lastMatchingParameterWins() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=UTF-8; charset=ISO-8859-1", "charset")).isEqualTo("ISO-8859-1");
    }

    @Test
    void picksTheRequestedParameterAmongSeveral() {
        assertThat(getSubTypeValueFromContentType("multipart/form-data; charset=UTF-8; boundary=abc; qs=0.5", "boundary")).isEqualTo("abc");
    }

    @Test
    void ignoresTrailingSemicolon() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=UTF-8;", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void extractsValueWhenContentTypeStartsWithTheParameter() {
        assertThat(getSubTypeValueFromContentType(";charset=UTF-8", "charset")).isEqualTo("UTF-8");
    }

    @Test
    void returnsNullWhenValueIsMissing() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset=", "charset")).isNull();
    }

    @Test
    void returnsEmptyStringWhenValueIsOnlyWhitespace() {
        // Quirk: "charset= " splits into two parts, so the trimmed (empty) value is returned
        assertThat(getSubTypeValueFromContentType("text/plain; charset= ;", "charset")).isEmpty();
    }

    @Test
    void returnsNullWhenParameterHasNoEqualsSign() {
        assertThat(getSubTypeValueFromContentType("text/plain; charset", "charset")).isNull();
    }

    @Test
    void extractsValueContainingEqualsSign() {
        // The parameter is split on the first "=" only (issue #1948)
        assertThat(getSubTypeValueFromContentType("multipart/form-data; boundary=abc=def", "boundary")).isEqualTo("abc=def");
        assertThat(getSubTypeValueFromContentType("multipart/form-data; boundary=\"abc=def\"", "boundary")).isEqualTo("abc=def");
        assertThat(getSubTypeValueFromContentType("multipart/form-data; boundary==abc=", "boundary")).isEqualTo("=abc=");
        assertThat(getSubTypeValueFromContentType("multipart/form-data; boundary=\"=\"", "boundary")).isEqualTo("=");
    }

    @Test
    void extractsValueFromStructuredSyntaxSuffixJsonContentType() {
        assertThat(getSubTypeValueFromContentType("application/vnd.api+json; charset=UTF-16", "charset")).isEqualTo("UTF-16");
    }

    @Test
    void extractsValueFromStructuredSyntaxSuffixXmlContentType() {
        assertThat(getSubTypeValueFromContentType("application/problem+xml;charset=ISO-8859-1", "charset")).isEqualTo("ISO-8859-1");
    }

    @Test
    void emptySubTypeMatchesParameterWithEmptyName() {
        assertThat(getSubTypeValueFromContentType("text/plain; =value", "")).isEqualTo("value");
        assertThat(getSubTypeValueFromContentType("text/plain; charset=UTF-8", "")).isNull();
    }
}
