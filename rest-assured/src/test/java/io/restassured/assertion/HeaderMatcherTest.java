/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.assertion;

import io.restassured.builder.ResponseBuilder;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

@SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
public class HeaderMatcherTest {

    private static final String HEADERS = "X-Name=value\nX-Number=12";

    private static Response response() {
        return new ResponseBuilder()
                .setStatusCode(200)
                .setHeader("X-Name", "value")
                .setHeader("X-Number", "12")
                .build();
    }

    private static Map<String, Object> validate(HeaderMatcher headerMatcher) {
        return (Map<String, Object>) headerMatcher.validateHeader(response());
    }

    private static HeaderMatcher headerMatcher(Object headerName, org.hamcrest.Matcher<?> matcher) {
        HeaderMatcher headerMatcher = new HeaderMatcher();
        headerMatcher.setHeaderName(headerName);
        headerMatcher.setMatcher(matcher);
        return headerMatcher;
    }

    @Test
    public void returns_success_and_empty_error_message_when_header_matches() {
        Map<String, Object> result = validate(headerMatcher("X-Name", equalTo("value")));

        assertThat(result).containsOnlyKeys("success", "errorMessage");
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(String.valueOf(result.get("errorMessage"))).isEmpty();
    }

    @Test
    public void reports_expected_matcher_actual_value_and_all_headers_when_header_does_not_match() {
        Map<String, Object> result = validate(headerMatcher("X-Name", equalTo("other")));

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void reports_null_as_actual_value_when_header_is_missing() {
        Map<String, Object> result = validate(headerMatcher("X-Missing", equalTo("other")));

        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected header \"X-Missing\" was not \"other\", was \"null\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void applies_mapping_function_before_matching() {
        HeaderMatcher headerMatcher = headerMatcher("X-Number", lessThan(10));
        headerMatcher.setMappingFunction((Function<String, Integer>) Integer::parseInt);

        Map<String, Object> result = validate(headerMatcher);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected header \"X-Number\" was not a value less than <10>, was \"12\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void mapping_function_result_that_matches_is_success() {
        HeaderMatcher headerMatcher = headerMatcher("X-Number", equalTo(12));
        headerMatcher.setMappingFunction((Function<String, Integer>) Integer::parseInt);

        assertThat(validate(headerMatcher).get("success")).isEqualTo(true);
    }

    @Test
    public void renders_list_returned_by_mapping_function_the_groovy_way() {
        HeaderMatcher headerMatcher = headerMatcher("X-Name", equalTo("other"));
        headerMatcher.setMappingFunction((Function<String, Object>) value -> Arrays.asList(value, 1));

        assertThat(String.valueOf(validate(headerMatcher).get("errorMessage")))
                .startsWith("Expected header \"X-Name\" was not \"other\", was \"[value, 1]\". Headers are:\n");
    }

    @Test
    public void renders_array_returned_by_mapping_function_the_groovy_way() {
        HeaderMatcher headerMatcher = headerMatcher("X-Name", equalTo("other"));
        headerMatcher.setMappingFunction((Function<String, Object>) value -> new int[]{1, 2});

        assertThat(String.valueOf(validate(headerMatcher).get("errorMessage")))
                .startsWith("Expected header \"X-Name\" was not \"other\", was \"[1, 2]\". Headers are:\n");
    }

    @Test
    public void renders_map_returned_by_mapping_function_the_groovy_way() {
        HeaderMatcher headerMatcher = headerMatcher("X-Name", equalTo("other"));
        headerMatcher.setMappingFunction((Function<String, Object>) value -> Collections.singletonMap("a", value));

        assertThat(String.valueOf(validate(headerMatcher).get("errorMessage")))
                .startsWith("Expected header \"X-Name\" was not \"other\", was \"[a:value]\". Headers are:\n");
    }

    @Test
    public void uses_matcher_from_response_aware_matcher() {
        HeaderMatcher headerMatcher = new HeaderMatcher();
        headerMatcher.setHeaderName("X-Name");
        headerMatcher.setResponseAwareMatcher((ResponseAwareMatcher<Response>) response -> equalTo(response.header("X-Number")));

        Map<String, Object> result = validate(headerMatcher);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected header \"X-Name\" was not \"12\", was \"value\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void response_aware_matcher_takes_precedence_over_matcher() {
        HeaderMatcher headerMatcher = headerMatcher("X-Name", equalTo("other"));
        headerMatcher.setResponseAwareMatcher((ResponseAwareMatcher<Response>) response -> equalTo("value"));

        assertThat(validate(headerMatcher).get("success")).isEqualTo(true);
    }

    @Test
    public void throws_illegal_argument_exception_when_response_aware_matcher_returns_null() {
        HeaderMatcher headerMatcher = new HeaderMatcher();
        headerMatcher.setHeaderName("X-Name");
        headerMatcher.setResponseAwareMatcher((ResponseAwareMatcher<Response>) response -> null);

        assertThatThrownBy(() -> validate(headerMatcher))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The ResponseAwareMatcher for header \"X-Name\" returned null instead of a Hamcrest matcher");
    }

    @Test
    public void rethrows_checked_exception_from_response_aware_matcher_unwrapped() {
        IOException exception = new IOException("boom");
        HeaderMatcher headerMatcher = new HeaderMatcher();
        headerMatcher.setHeaderName("X-Name");
        headerMatcher.setResponseAwareMatcher((ResponseAwareMatcher<Response>) response -> {
            throw exception;
        });

        assertThatThrownBy(() -> validate(headerMatcher)).isSameAs(exception);
    }

    @Test
    public void keeps_groovy_accessors() {
        Function<String, Object> mappingFunction = value -> value;
        ResponseAwareMatcher<Response> responseAwareMatcher = response -> equalTo("value");
        HeaderMatcher headerMatcher = headerMatcher("X-Name", equalTo("value"));
        headerMatcher.setMappingFunction(mappingFunction);
        headerMatcher.setResponseAwareMatcher(responseAwareMatcher);

        assertThat(headerMatcher.getHeaderName()).isEqualTo("X-Name");
        assertThat(headerMatcher.getMappingFunction()).isSameAs(mappingFunction);
        assertThat(headerMatcher.getResponseAwareMatcher()).isSameAs(responseAwareMatcher);
        assertThat(headerMatcher.getMatcher().toString()).isEqualTo("\"value\"");
    }
}
