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
import io.restassured.filter.Filter;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;

/**
 * Runs each named-argument construction of {@link HeaderMatcher}, {@link DetailedCookieAssertion},
 * {@link ResponseTimeMatcher} and {@code CookieMatcher} in {@code ResponseSpecificationImpl}.
 */
public class AssertionMatcherNamedArgumentSitesTest {

    private static final String HEADERS = "X-Name=value\nX-Number=12\nSet-Cookie=name=value; Path=/p";

    private static final Filter RESPONSE = (requestSpec, responseSpec, ctx) -> new ResponseBuilder()
            .setStatusCode(200)
            .setHeader("X-Name", "value")
            .setHeader("X-Number", "12")
            .setHeader("Set-Cookie", "name=value; Path=/p")
            .build();

    private static ValidatableResponse then() {
        return given().filter(RESPONSE).get("http://localhost:8080/").then();
    }

    @Test
    public void headers_map_with_single_values() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("X-Name", "value");
        expected.put("X-Number", startsWith("1"));
        then().headers(expected);

        assertThatThrownBy(() -> then().headers(Collections.singletonMap("X-Name", "other")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void headers_map_with_list_values() {
        then().headers(Collections.singletonMap("X-Name", Arrays.asList("value", startsWith("v"))));

        assertThatThrownBy(() -> then().headers(Collections.singletonMap("X-Name", Arrays.asList("value", startsWith("x")))))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected header \"X-Name\" was not a string starting with \"x\", was \"value\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void headers_name_value_pairs() {
        then().headers("X-Name", "value", "X-Number", "12");
    }

    @Test
    public void header_with_mapping_function() {
        then().header("X-Number", Integer::parseInt, lessThan(13));

        assertThatThrownBy(() -> then().header("X-Number", Integer::parseInt, lessThan(10)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected header \"X-Number\" was not a value less than <10>, was \"12\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void header_with_matcher_and_with_string() {
        then().header("X-Name", startsWith("va")).header("X-Name", "value");

        assertThatThrownBy(() -> given().filter(RESPONSE).expect().header("X-Name", "other").header("X-Number", startsWith("2")).when().get("http://localhost:8080/"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("2 expectations failed.\nExpected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n\n" +
                        "Expected header \"X-Number\" was not a string starting with \"2\", was \"12\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void header_with_response_aware_matcher() {
        then().header("X-Name", response -> equalTo("value"));

        assertThatThrownBy(() -> then().header("X-Name", response -> equalTo(response.header("X-Number"))))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected header \"X-Name\" was not \"12\", was \"value\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    public void cookies_map_and_cookie_with_matcher() {
        then().cookies(Collections.singletonMap("name", Arrays.asList("value", startsWith("v"))))
                .cookies(Collections.singletonMap("name", "value"))
                .cookie("name", startsWith("v"));

        assertThatThrownBy(() -> then().cookie("name", startsWith("x")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected cookie \"name\" was not a string starting with \"x\", was \"value\".\n");
    }

    @Test
    public void cookie_with_detailed_cookie_matcher() {
        then().cookie("name", detailedCookie().value("value").path("/p"));

        assertThatThrownBy(() -> then().cookie("name", detailedCookie().path("/other")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected cookie \"name\" was not (not null and hasProperty(\"path\", \"/other\")), hasProperty(\"path\", \"/other\")  property 'path' was \"/p\".\n");
    }

    @Test
    public void time_with_and_without_time_unit() {
        // A response built by a filter has no recorded time
        assertThatThrownBy(() -> then().time(lessThan(60000L)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nNo time was recorded, cannot perform response time validation.");
        assertThatThrownBy(() -> then().time(lessThan(60L), TimeUnit.SECONDS))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nNo time was recorded, cannot perform response time validation.");
    }
}
