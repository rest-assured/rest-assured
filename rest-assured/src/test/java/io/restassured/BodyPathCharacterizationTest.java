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

package io.restassured;

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.JsonConfig;
import io.restassured.config.MatcherConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.filter.Filter;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static io.restassured.config.MatcherConfig.ErrorDescriptionType.HAMCREST;
import static io.restassured.matcher.RestAssuredMatchers.endsWithPath;
import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Pins how paths in {@code body(..)}, {@code path(..)} and {@code extract().path(..)} are built from root paths and
 * arguments and evaluated, and what their failure messages say, with GPath as the path engine. Each case runs on a
 * response returned by a filter, so no request reaches the network.
 */
class BodyPathCharacterizationTest {

    private static final String JSON = "{\"store\":{\"book\":[{\"title\":\"A\",\"price\":8.95},{\"title\":\"B\",\"price\":12.99}]," +
            "\"bicycle\":{\"color\":\"red\"}},\"a\":{\"b\":{\"c\":1}},\"$schema\":\"s\",\"self\":\"red\",\"number\":12345}";
    private static final String JSON_ARRAY = "[{\"name\":\"x\"},{\"name\":\"y\"}]";
    private static final String XML = "<shop><category name=\"c\"><item><name>n1</name><price>1</price></item>" +
            "<item><name>n2</name><price>2</price></item></category></shop>";
    private static final String NAMESPACED_XML = "<a:root xmlns:a=\"urn:a\"><a:child>x</a:child></a:root>";
    private static final String HTML = "<html><head><title>T</title></head><body><p>x</p></body></html>";

    /**
     * Returns a response with the config of the request, as a response that was sent has.
     */
    private static Filter respondWith(String contentType, String body) {
        return (requestSpec, responseSpec, ctx) -> {
            Response response = new ResponseBuilder().setStatusCode(200).setContentType(contentType).setBody(body).build();
            if (requestSpec.getConfig() != null) {
                ((RestAssuredResponseImpl) response).setConfig(requestSpec.getConfig());
            }
            return response;
        };
    }

    private static ValidatableResponse then(String contentType, String body) {
        return response(contentType, body).then();
    }

    private static Response response(String contentType, String body) {
        return given().filter(respondWith(contentType, body)).get("http://localhost:8080/");
    }

    private static ValidatableResponse json() {
        return then("application/json", JSON);
    }

    /**
     * Runs the expectations before the request, so the body is parsed as a stream, where {@link #json()} parses it
     * eagerly as a String.
     */
    private static ResponseSpecification expectJson() {
        return given().filter(respondWith("application/json", JSON)).expect();
    }

    // Root paths

    @Test
    void root_path_and_key_are_joined_with_a_dot() {
        json().rootPath("store.bicycle").body("color", equalTo("red"));
        expectJson().rootPath("store.bicycle").body("color", equalTo("red")).when().get("http://localhost:8080/");
    }

    @Test
    void root_path_ending_with_a_dot_and_key_starting_with_a_dot_are_joined_with_one_dot() {
        json().rootPath("store.bicycle.").body(".color", equalTo("red"));
    }

    @Test
    void root_path_ending_with_a_dot_is_joined_without_another_dot() {
        json().rootPath("store.bicycle.").body("color", equalTo("red"));
    }

    @Test
    void key_starting_with_a_dot_is_appended_as_is() {
        json().rootPath("store.bicycle").body(".color", equalTo("red"));
    }

    @Test
    void key_starting_with_a_bracket_is_appended_without_a_dot() {
        json().rootPath("store.book").body("[1].title", equalTo("B"));
    }

    @Test
    void key_starting_with_safe_navigation_is_appended_without_a_dot() {
        json().rootPath("store.bicycle").body("?.color", equalTo("red"));
        json().rootPath("store.missing").body("?.color", nullValue());
        json().rootPath("store.book").body("?[0].title", equalTo("A"));
    }

    @Test
    void key_starting_with_a_spread_is_appended_without_a_dot() {
        json().rootPath("store.book").body("*.title", contains("A", "B"));
    }

    @Test
    void root_path_spreads_over_a_list() {
        json().rootPath("store.book").body("title", contains("A", "B"));
    }

    @Test
    void empty_key_and_arguments_only_address_the_root_path() {
        json().rootPath("store.book[%d]").body(withArgs(1), hasKey("title"));
        json().rootPath("store.book[%d].title").body(withArgs(0), equalTo("A"), withArgs(1), equalTo("B"));
    }

    @Test
    void arguments_without_a_root_path_fail() {
        assertThatThrownBy(() -> json().body(withArgs(1), equalTo("A")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot specify arguments when root path is empty");
    }

    @Test
    void no_root_path_uses_the_key_as_is() {
        json().rootPath("store").noRootPath().body("a.b.c", equalTo(1));
    }

    @Test
    void append_and_detach_change_the_root_path_for_later_expectations_only() {
        json().rootPath("store").appendRootPath("bicycle").body("color", equalTo("red"))
                .detachRootPath("bicycle").body("book[0].title", equalTo("A"));
        expectJson().rootPath("store").body("bicycle.color", equalTo("red"))
                .appendRootPath("book[0]").body("title", equalTo("A"))
                .rootPath("a").body("b.c", equalTo(1))
                .when().get("http://localhost:8080/");
    }

    @Test
    void root_path_of_a_merged_specification_applies_to_its_own_expectations() {
        ResponseSpecification spec = new io.restassured.builder.ResponseSpecBuilder().rootPath("store.bicycle")
                .expectBody("color", equalTo("red")).build();

        given().filter(respondWith("application/json", JSON)).expect().rootPath("a").body("b.c", equalTo(1)).spec(spec)
                .when().get("http://localhost:8080/");
    }

    // Arguments

    @Test
    void arguments_are_applied_after_root_path_and_key_are_joined() {
        // The root path's placeholder takes the first argument, the key's the second
        json().rootPath("store.%s").body("[%d].title", withArgs("book", 1), equalTo("B"));
        // An argument may contain a dot
        json().rootPath("a").body("%s", withArgs("b.c"), equalTo(1));
        // A placeholder left by rootPath(.., args) is filled by the arguments of body(..)
        json().rootPath("%s.%s", withArgs("store")).body("color", withArgs("bicycle"), equalTo("red"));
    }

    @Test
    void explicit_argument_indexes_count_from_the_joined_path() {
        json().rootPath("store.%2$s").body("%1$s", withArgs("color", "bicycle"), equalTo("red"));
        json().rootPath("a.%s").body("%<s", withArgs("b"), is(nullValue()));
    }

    @Test
    void the_same_key_with_different_arguments_gets_each_set_of_arguments() {
        json().rootPath("store.book").body("[%d].title", withArgs(0), equalTo("A"),
                "[%d].title", withArgs(0), equalTo("A"), "[%d].title", withArgs(1), equalTo("B"));
    }

    @Test
    void path_takes_string_arguments() {
        assertThat((String) response("application/json", JSON).path("store.book[%s].title", "1")).isEqualTo("B");
        assertThat((String) response("application/json", JSON).then().extract().path("store.%s.color", "bicycle")).isEqualTo("red");
    }

    @Test
    void a_bad_argument_fails_when_the_expectation_is_defined() {
        ResponseSpecification spec = given().filter(respondWith("application/json", JSON)).expect().rootPath("store.book[%d]");

        assertThatThrownBy(() -> spec.body("title", withArgs("x"), equalTo("A")))
                .isInstanceOf(java.util.IllegalFormatConversionException.class);
    }

    // ResponseAwareMatcher

    @Test
    void response_aware_matchers_evaluate_their_path_on_the_whole_response_without_the_root_path() {
        json().rootPath("store.bicycle").body("color", equalToPath("self"));
        json().body("store.bicycle.color", equalToPath("self")).body("store.book[0].title", endsWithPath("store.book[0].title"));
        json().rootPath("store").body("bicycle.color", response -> equalTo(response.path("store.bicycle.color")));
    }

    // Failure messages

    @Test
    void failure_message_shows_the_joined_path() {
        assertThatThrownBy(() -> json().rootPath("store.book").body("[%d].title", withArgs(0), equalTo("B")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nJSON path store.book[0].title doesn't match.\nExpected: B\n  Actual: A\n");
    }

    @Test
    void failure_message_with_hamcrest_descriptions() {
        RestAssuredConfig config = RestAssuredConfig.config().matcherConfig(new MatcherConfig(HAMCREST));

        assertThatThrownBy(() -> given().config(config).filter(respondWith("application/json", JSON))
                .expect().rootPath("store.bicycle").body("color", equalTo("blue")).when().get("http://localhost:8080/"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nJSON path store.bicycle.color doesn't match.\n\nExpected: \"blue\"\n  Actual: was \"red\"");
    }

    @Test
    void failure_message_for_xml() {
        assertThatThrownBy(() -> then("application/xml", XML).rootPath("shop.category").body("item[0].name", equalTo("n2")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nXML path shop.category.item[0].name doesn't match.\nExpected: n2\n  Actual: n1\n");
    }

    @Test
    void invalid_path_fails_with_an_illegal_argument_exception() {
        assertThatThrownBy(() -> json().body("store.book.findAll {", equalTo("A")))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid JSON expression:");
    }

    // A leading $

    @Test
    void a_lone_dollar_is_the_root() {
        json().body("$", hasKey("store")).body("", hasKey("store"));
        assertThat((Object) response("application/json", JSON).path("$")).isInstanceOf(java.util.Map.class);
    }

    @Test
    void a_leading_dollar_dot_is_not_the_root_on_an_object() {
        json().body("$.store", nullValue()).body("$['store']", nullValue()).body("$[0]", nullValue());
        assertThat((Object) response("application/json", JSON).path("$.store")).isNull();
    }

    @Test
    void a_key_starting_with_a_dollar_is_a_key() {
        json().body("$schema", equalTo("s"));
    }

    @Test
    void a_leading_dollar_dot_spreads_over_an_array() {
        then("application/json", JSON_ARRAY).body("$", org.hamcrest.Matchers.hasSize(2)).body("$.name", equalTo(Collections.emptyList()))
                .body("[0].name", equalTo("x")).body("name", equalTo(Arrays.asList("x", "y")));
    }

    // XML and HTML

    @Test
    void xml_paths_with_root_paths_and_arguments() {
        then("application/xml", XML).rootPath("shop.category").body("item[%d].name", withArgs(1), equalTo("n2"))
                .body("@name", equalTo("c")).body("item.price", contains("1", "2"));
        assertThat((String) response("application/xml", XML).path("shop.category.item[%s].name", "0")).isEqualTo("n1");
    }

    @Test
    void html_paths() {
        then("text/html", HTML).body("html.head.title", equalTo("T"));
        assertThat((String) response("text/html", HTML).path("html.body.p")).isEqualTo("x");
    }

    // Settings that body(..) and path(..) use differently

    @Test
    void path_ignores_the_number_length_limit_that_body_honors() {
        RestAssuredConfig config = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().numberLengthLimit(3));
        Response response = given().config(config).filter(respondWith("application/json", JSON)).get("http://localhost:8080/");

        assertThat((Object) response.path("number")).isEqualTo(12345);
        assertThatThrownBy(() -> response.then().body("number", equalTo(12345))).hasMessageContaining("exceeds the maximum allowed length of 3");
    }

    @Test
    void path_ignores_namespace_awareness_that_body_honors() {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().namespaceAware(false));
        Response response = given().config(config).filter(respondWith("application/xml", NAMESPACED_XML)).get("http://localhost:8080/");

        // Not namespace aware, the prefix is part of the element name
        response.then().body("'a:root'.'a:child'", equalTo("x"));
        // path(..) is always namespace aware, so the prefix isn't
        assertThat((String) response.path("root.child")).isEqualTo("x");
    }

    @Test
    void xml_properties_are_set_before_features_in_body_and_in_path() {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig()
                .feature("urn:unknown-feature", true).property("urn:unknown-property", "x"));
        Response response = given().config(config).filter(respondWith("application/xml", XML)).get("http://localhost:8080/");

        assertThatThrownBy(() -> response.then().body("shop.category.@name", equalTo("c")))
                .isExactlyInstanceOf(org.xml.sax.SAXNotRecognizedException.class).hasMessageContaining("urn:unknown-property");
        assertThatThrownBy(() -> response.path("shop.category.@name"))
                .isExactlyInstanceOf(io.restassured.path.xml.exception.XmlPathException.class).hasMessage("Failed to parse the XML document")
                .cause().isExactlyInstanceOf(org.xml.sax.SAXNotRecognizedException.class).hasMessageContaining("urn:unknown-property");
    }

    @Test
    void path_fails_with_the_exception_of_json_path_and_xml_path_when_the_body_cant_be_parsed() {
        assertThatThrownBy(() -> response("application/json", "{").path("a"))
                .isExactlyInstanceOf(io.restassured.path.json.exception.JsonPathException.class).hasMessage("Failed to parse the JSON document");
        assertThatThrownBy(() -> response("application/xml", "<a>").path("a"))
                .isExactlyInstanceOf(io.restassured.path.xml.exception.XmlPathException.class).hasMessage("Failed to parse the XML document");
    }
}
