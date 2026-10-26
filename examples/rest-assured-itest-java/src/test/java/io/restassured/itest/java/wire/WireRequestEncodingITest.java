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

package io.restassured.itest.java.wire;

import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.TestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * How REST Assured encodes the request target, the body and its Content-Type, and the explicit headers. URI and body
 * encoding move from HttpClient 4 into REST Assured's core later, so these tests check the exact bytes.
 */
public class WireRequestEncodingITest extends WireTest {

    private static final String A_UML = "ä";
    private static final String EURO = "€";

    // Paths

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("literalPaths")
    public void
    literal_path_is_percent_encoded(String path, String expectedTarget) {
        given().get(server.url(path));

        assertThat(server.onlyRequest().target()).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> literalPaths() {
        return Stream.of(
                arguments("/a b/c", "/a%20b/c"),
                arguments("/a%b", "/a%25b"),
                // Today's behavior: the % of an already encoded path is encoded again
                arguments("/a%20b", "/a%2520b"),
                arguments("/a+b", "/a%2Bb"),
                arguments("/" + A_UML + EURO, "/%C3%A4%E2%82%AC"),
                // The fragment isn't sent
                arguments("/a#b", "/a"),
                // Today's behavior: characters that RFC 3986 allows in a path segment are encoded anyway, except *
                arguments("/a;b=c/d:e@f/!$&'()*,=", "/a%3Bb%3Dc/d%3Ae%40f/%21%24%26%27%28%29*%2C%3D"),
                arguments("/a/", "/a/"),
                // Today's behavior: the empty segment turns into an encoded "//"
                arguments("/a//b", "/a%2F%2Fb"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pathParams")
    public void
    path_param_is_percent_encoded(String value, String expectedTarget) {
        given().pathParam("p", value).get(server.url("/x/{p}"));

        assertThat(server.onlyRequest().target()).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> pathParams() {
        return Stream.of(
                arguments("a b", "/x/a%20b"),
                arguments("a/b", "/x/a%2Fb"),
                arguments("a%b", "/x/a%25b"),
                arguments("a%20b", "/x/a%2520b"),
                arguments("a+b", "/x/a%2Bb"),
                arguments("a?b", "/x/a%3Fb"),
                arguments("a#b", "/x/a%23b"),
                arguments("a&b=c", "/x/a%26b%3Dc"),
                arguments(A_UML + EURO, "/x/%C3%A4%E2%82%AC"));
    }

    @Test public void
    path_param_uses_the_default_query_parameter_charset() {
        given().config(queryCharset("ISO-8859-1")).pathParam("p", A_UML).get(server.url("/x/{p}"));

        assertThat(server.onlyRequest().target()).isEqualTo("/x/%E4");
    }

    @Test public void
    path_param_inside_a_segment_is_percent_encoded() {
        given().pathParam("p", "a b").get(server.url("/x/pre{p}post"));

        assertThat(server.onlyRequest().target()).isEqualTo("/x/prea%20bpost");
    }

    @Test public void
    empty_path_param_removes_its_segment() {
        given().pathParam("p", "").get(server.url("/x/{p}/y"));

        assertThat(server.onlyRequest().target()).isEqualTo("/x/y");
    }

    @Test public void
    unnamed_path_params_are_percent_encoded() {
        given().get(server.url("/x/{a}/{b}"), "a b", "c/d");

        assertThat(server.onlyRequest().target()).isEqualTo("/x/a%20b/c%2Fd");
    }

    @Test public void
    named_and_unnamed_path_params_are_combined() {
        given().pathParam("a", "n a").get(server.url("/x/{a}/{b}"), "u b");

        assertThat(server.onlyRequest().target()).isEqualTo("/x/n%20a/u%20b");
    }

    @Test public void
    path_param_in_the_query_of_the_path_is_split_at_an_ampersand() {
        given().pathParam("p", "a b&c").get(server.url("/x?q={p}"));

        // Today's behavior: the & in the value splits it into a second query parameter
        assertThat(server.onlyRequest().target()).isEqualTo("/x?q=a%20b&c");
    }

    @Test public void
    url_encoding_disabled_sends_path_param_as_is() {
        given().urlEncodingEnabled(false).pathParam("p", "a%20b").get(server.url("/x/{p}"));

        assertThat(server.onlyRequest().target()).isEqualTo("/x/a%20b");
    }

    @Test public void
    url_encoding_disabled_sends_slash_in_path_param_as_a_segment_separator() {
        given().urlEncodingEnabled(false).pathParam("p", "a/b").get(server.url("/x/{p}"));

        assertThat(server.onlyRequest().target()).isEqualTo("/x/a/b");
    }

    @Test public void
    url_encoding_disabled_sends_already_encoded_literal_path_as_is() {
        given().urlEncodingEnabled(false).get(server.url("/a%20b/%C3%A4"));

        assertThat(server.onlyRequest().target()).isEqualTo("/a%20b/%C3%A4");
    }

    @Test public void
    url_encoding_disabled_with_space_in_literal_path_fails_before_sending() {
        assertThatThrownBy(() -> given().urlEncodingEnabled(false).get(server.url("/a b")))
                .isInstanceOf(URISyntaxException.class)
                .hasMessageContaining("Illegal character in path");

        assertThat(server.requests()).isEmpty();
    }

    // Query parameters

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @MethodSource("queryParams")
    public void
    query_param_is_percent_encoded(String name, String value, String expectedTarget) {
        given().queryParam(name, value).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> queryParams() {
        return Stream.of(
                // A space is %20, not +
                arguments("a", "b c", "/q?a=b%20c"),
                arguments("a b", "c", "/q?a%20b=c"),
                arguments("a", "b+c", "/q?a=b%2Bc"),
                arguments("a&b", "c=d&e", "/q?a%26b=c%3Dd%26e"),
                arguments(A_UML, EURO, "/q?%C3%A4=%E2%82%AC"),
                // Today's behavior: ~ is encoded although it is unreserved, * is not
                arguments("a", "/?#%:@!$'()*,;[]~", "/q?a=%2F%3F%23%25%3A%40%21%24%27%28%29*%2C%3B%5B%5D%7E"),
                arguments("a", "b%20c", "/q?a=b%2520c"),
                arguments("a", "", "/q?a="));
    }

    @Test public void
    query_param_without_value_is_sent_without_equals_sign() {
        given().queryParam("a").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a");
    }

    @Test public void
    null_query_param_value_is_sent_without_equals_sign() {
        given().queryParam("a", (Object) null).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a");
    }

    @Test public void
    non_string_query_param_values_are_sent_as_their_string_form() {
        given().queryParam("a", 1).queryParam("b", true).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=1&b=true");
    }

    @Test public void
    multiple_query_param_values_are_sent_as_repeated_params() {
        given().queryParam("a", "1", "2").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=1&a=2");
    }

    @Test public void
    list_query_param_values_are_sent_as_repeated_params() {
        given().queryParam("a", Arrays.asList("1", "2")).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=1&a=2");
    }

    @Test public void
    empty_value_among_multiple_query_param_values_is_sent_with_equals_sign() {
        given().queryParam("a", "", "x").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=&a=x");
    }

    @Test public void
    duplicate_query_param_names_are_all_sent() {
        given().queryParam("a", "1").queryParam("a", "2").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=1&a=2");
    }

    @Test public void
    query_params_are_sent_in_the_order_they_were_added() {
        given().queryParam("z", "1").queryParam("a", "2").queryParam("m", "3").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?z=1&a=2&m=3");
    }

    @Test public void
    query_params_in_the_url_are_sent_after_query_params_from_the_specification() {
        given().queryParam("b", "2").get(server.url("/q?a=1&c"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?b=2&a=1&c");
    }

    @Test public void
    query_param_in_the_url_with_the_same_name_is_sent_after_the_specification_value() {
        given().queryParam("a", "2").get(server.url("/q?a=1"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=2&a=1");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("urlQueries")
    public void
    query_in_the_url_is_decoded_into_params_and_encoded_again(String pathAndQuery, String expectedTarget) {
        given().get(server.url(pathAndQuery));

        assertThat(server.onlyRequest().target()).isEqualTo(expectedTarget);
    }

    static Stream<Arguments> urlQueries() {
        return Stream.of(
                // Today's behavior: an already encoded value in the url is encoded again
                arguments("/q?a=b c&d=%20&e=&f", "/q?a=b%20c&d=%2520&e=&f"),
                arguments("/q?a=b+c", "/q?a=b%2Bc"),
                arguments("/q?a=b=c", "/q?a=b%3Dc"),
                arguments("/q?a=" + A_UML, "/q?a=%C3%A4"),
                // Today's behavior: unlike a fragment after a path, it isn't dropped but sent encoded as part of the last value
                arguments("/q?a=b#frag", "/q?a=b%23frag"));
    }

    @Test public void
    empty_parameter_in_url_query_fails_before_sending() {
        assertThatThrownBy(() -> given().get(server.url("/q?a=1&&b=2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal parameters passed to REST Assured. Parameters was: [a=1, , b=2]");

        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    params_on_get_are_sent_as_query_params_before_query_params() {
        given().queryParam("q", "2").param("p", "1").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?p=1&q=2");
    }

    @Test public void
    params_on_get_are_percent_encoded_like_query_params() {
        given().param("a", "b c").param("m", "1", "2").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=b%20c&m=1&m=2");
    }

    @Test public void
    params_map_on_get_is_sent_in_map_order() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("x", "1");
        params.put("y", "2 3");

        given().params(params).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?x=1&y=2%203");
    }

    @Test public void
    query_params_on_post_are_sent_in_the_query() {
        given().queryParam("a", "b c").body("x").post(server.url("/q"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/q?a=b%20c");
        assertThat(request.bodyAsString()).isEqualTo("x");
    }

    @Test public void
    default_query_parameter_charset_is_used_for_query_params() {
        given().config(queryCharset("ISO-8859-1")).queryParam("a", A_UML).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=%E4");
    }

    @Test public void
    character_not_in_the_query_parameter_charset_is_sent_as_question_mark() {
        given().config(queryCharset("ISO-8859-1")).queryParam("a", EURO).get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=%3F");
    }

    @Test public void
    url_encoding_disabled_sends_query_params_as_is() {
        given().urlEncodingEnabled(false).queryParam("a", "b%20c").queryParam("d", "e+f").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=b%20c&d=e+f");
    }

    @Test public void
    url_encoding_disabled_sends_ampersand_in_query_param_value_as_a_separator() {
        given().urlEncodingEnabled(false).queryParam("a", "b&c").get(server.url("/q"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=b&c");
    }

    @Test public void
    url_encoding_disabled_sends_url_query_as_is() {
        given().urlEncodingEnabled(false).get(server.url("/q?a=b%20c&d=e+f"));

        assertThat(server.onlyRequest().target()).isEqualTo("/q?a=b%20c&d=e+f");
    }

    @Test public void
    url_encoding_disabled_with_space_in_query_param_fails_before_sending() {
        assertThatThrownBy(() -> given().urlEncodingEnabled(false).queryParam("a", "b c").get(server.url("/q")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal character in query");

        assertThat(server.requests()).isEmpty();
    }

    // Bodies

    @Test public void
    string_body_is_sent_as_text_plain_in_iso_8859_1_by_default() {
        given().body("h" + A_UML + EURO).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("3");
        // The euro sign isn't in ISO-8859-1 and becomes a ?
        assertThat(request.body()).containsExactly('h', 0xE4, '?');
    }

    @Test public void
    string_body_is_encoded_with_the_charset_of_the_content_type() {
        given().contentType("text/plain; charset=UTF-8").body("h" + A_UML + EURO).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
        assertThat(request.body()).containsExactly(("h" + A_UML + EURO).getBytes(UTF_8));
    }

    @Test public void
    string_body_with_utf_16_content_type_starts_with_a_byte_order_mark() {
        given().contentType("application/json; charset=UTF-16").body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json; charset=UTF-16");
        assertThat(request.body()).containsExactly(0xFE, 0xFF, 0x00, 0xE4);
    }

    @Test public void
    text_plain_content_type_without_charset_gets_iso_8859_1_appended() {
        given().contentType("text/plain").body("h" + A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly('h', 0xE4);
    }

    @Test public void
    string_body_with_json_content_type_is_sent_in_utf_8_without_charset() {
        given().contentType(ContentType.JSON).body("{\"a\":\"" + A_UML + "\"}").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\"}").getBytes(UTF_8));
    }

    @Test public void
    string_body_with_json_content_type_as_string_is_sent_in_utf_8_without_charset() {
        given().contentType("application/json").body("{\"a\":\"" + A_UML + "\"}").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\"}").getBytes(UTF_8));
    }

    @Test public void
    string_body_with_xml_content_type_is_sent_in_iso_8859_1() {
        given().contentType(ContentType.XML).body("<a>" + A_UML + "</a>").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/xml; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("<a>" + A_UML + "</a>").getBytes(ISO_8859_1));
    }

    @Test public void
    string_body_with_form_url_encoded_content_type_is_sent_as_is() {
        given().contentType(ContentType.URLENC).body("a=b c").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("a=b c");
    }

    @Test public void
    string_body_with_custom_json_content_type_is_sent_in_iso_8859_1() {
        given().contentType("application/vnd.foo+json").body("{\"a\":\"" + A_UML + "\"}").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        // Today's behavior: unlike application/json, a +json type gets the default content charset
        assertThat(request.header("Content-Type")).isEqualTo("application/vnd.foo+json; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    string_body_with_octet_stream_content_type_is_rejected() {
        assertThatThrownBy(() -> given().contentType("application/octet-stream").body("ab").post(server.url("/b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Don't know how to encode ab as a byte stream.");

        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    string_body_with_unknown_content_type_is_rejected() {
        assertThatThrownBy(() -> given().contentType("application/x-foo").body("ab").post(server.url("/b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Don't know how to encode ab as a byte stream.");

        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    byte_array_body_is_sent_as_octet_stream_with_charset() {
        given().body(new byte[]{0, 1, (byte) 0xFF, 'a'}).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        // Today's behavior: a charset is appended to a binary content type
        assertThat(request.header("Content-Type")).isEqualTo("application/octet-stream; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("4");
        assertThat(request.body()).containsExactly(0, 1, 0xFF, 'a');
    }

    @Test public void
    byte_array_body_is_sent_as_is_with_text_content_type() {
        given().contentType("text/plain").body(new byte[]{0, 1, (byte) 0xFF, 'a'}).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(0, 1, 0xFF, 'a');
    }

    @Test public void
    byte_array_body_is_sent_as_is_with_json_content_type() {
        given().contentType(ContentType.JSON).body("{\"a\":\"ä\"}".getBytes(UTF_8)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly("{\"a\":\"ä\"}".getBytes(UTF_8));
    }

    @Test public void
    file_body_is_sent_as_is_as_text_plain(@TempDir Path dir) throws IOException {
        File file = utf8File(dir);

        given().body(file).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        // Today's behavior: a file is sent as text/plain, not application/octet-stream like a byte array
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("6");
        assertThat(request.body()).containsExactly(("file" + A_UML).getBytes(UTF_8));
    }

    @Test public void
    file_body_is_sent_as_is_with_binary_content_type(@TempDir Path dir) throws IOException {
        File file = utf8File(dir);

        given().contentType(ContentType.BINARY).body(file).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/octet-stream; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("6");
        assertThat(request.body()).containsExactly(("file" + A_UML).getBytes(UTF_8));
    }

    @Test public void
    file_body_is_sent_as_is_with_json_content_type(@TempDir Path dir) throws IOException {
        File file = utf8File(dir);

        given().contentType(ContentType.JSON).body(file).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly(("file" + A_UML).getBytes(UTF_8));
    }

    @Test public void
    input_stream_body_is_sent_as_octet_stream_with_content_length() {
        given().body(new ByteArrayInputStream(new byte[]{0, 'x', (byte) 0xC3})).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/octet-stream; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("3");
        assertThat(request.body()).containsExactly(0, 'x', 0xC3);
    }

    @Test public void
    input_stream_body_with_text_content_type_is_streamed_chunked() {
        given().contentType("text/plain").body(new ByteArrayInputStream(new byte[]{0, 'x', (byte) 0xC3})).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        // Today's behavior: unlike an octet-stream input stream, the length isn't known up front
        assertThat(request.hasHeader("Content-Length")).isFalse();
        assertThat(request.header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(request.body()).containsExactly(0, 'x', 0xC3);
    }

    @Test public void
    map_body_without_content_type_is_serialized_as_json_but_sent_as_text_plain() {
        given().body(map("a", A_UML, "b", 1)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        // Today's behavior: the body is JSON but the Content-Type says text/plain and it's encoded in ISO-8859-1
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\",\"b\":1}").getBytes(ISO_8859_1));
    }

    @Test public void
    map_body_with_json_content_type_is_serialized_as_utf_8_json() {
        given().contentType(ContentType.JSON).body(map("a", A_UML, "b", 1)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.header("Content-Length")).isEqualTo("16");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\",\"b\":1}").getBytes(UTF_8));
    }

    @Test public void
    object_body_without_content_type_is_serialized_as_json_but_sent_as_text_plain() {
        given().body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"name\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    object_body_with_json_content_type_is_serialized_as_utf_8_json() {
        given().contentType(ContentType.JSON).body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly(("{\"name\":\"" + A_UML + "\"}").getBytes(UTF_8));
    }

    @Test public void
    object_body_with_json_content_type_and_charset_is_serialized_in_that_charset() {
        given().contentType("application/json; charset=ISO-8859-1").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"name\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    object_body_with_custom_json_content_type_is_serialized_as_iso_8859_1_json() {
        given().contentType("application/vnd.foo+json").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/vnd.foo+json; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"name\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    object_body_with_xml_content_type_is_serialized_with_jaxb_in_iso_8859_1() {
        given().contentType(ContentType.XML).body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/xml; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(xml("ISO-8859-1").getBytes(ISO_8859_1));
    }

    @Test public void
    object_body_with_xml_content_type_and_charset_is_serialized_in_that_charset() {
        given().contentType("application/xml; charset=UTF-8").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/xml; charset=UTF-8");
        assertThat(request.body()).containsExactly(xml("UTF-8").getBytes(UTF_8));
    }

    @Test public void
    object_body_with_custom_xml_content_type_is_serialized_with_jaxb() {
        given().contentType("application/vnd.foo+xml").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/vnd.foo+xml; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(xml("ISO-8859-1").getBytes(ISO_8859_1));
    }

    @Test public void
    object_body_with_unknown_content_type_is_rejected() {
        assertThatThrownBy(() -> given().contentType("application/x-foo").body(new Person("a")).post(server.url("/b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize because cannot determine how to serialize content-type application/x-foo");

        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    no_content_type_sends_the_body_without_content_type() {
        given().noContentType().body("x").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.hasHeader("Content-Type")).isFalse();
        assertThat(request.header("Content-Length")).isEqualTo("1");
        assertThat(request.bodyAsString()).isEqualTo("x");
    }

    // EncoderConfig

    @Test public void
    default_content_charset_is_used_for_string_body_and_content_type() {
        given().config(encoder(encoderConfig().defaultContentCharset("UTF-8"))).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    default_content_charset_utf_16_adds_a_byte_order_mark() {
        given().config(encoder(encoderConfig().defaultContentCharset("UTF-16"))).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-16");
        assertThat(request.body()).containsExactly(0xFE, 0xFF, 0x00, 0xE4);
    }

    @Test public void
    configuring_default_content_charset_appends_utf_8_to_json_content_type() {
        given().config(encoder(encoderConfig().defaultContentCharset("UTF-8"))).contentType(ContentType.JSON).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        // Today's behavior: a user configured EncoderConfig makes application/json get its (unchanged) UTF-8 charset
        assertThat(request.header("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    default_charset_for_content_type_is_used_for_explicit_content_type() {
        given().config(encoder(encoderConfig().defaultCharsetForContentType("UTF-8", "text/plain"))).contentType("text/plain").body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    default_charset_for_content_type_is_used_for_implicit_text_plain() {
        given().config(encoder(encoderConfig().defaultCharsetForContentType("UTF-8", "text/plain"))).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    default_charset_for_json_content_type_is_appended_and_used() {
        given().config(encoder(encoderConfig().defaultCharsetForContentType("ISO-8859-1", ContentType.JSON))).contentType(ContentType.JSON).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(0xE4);
    }

    @Test public void
    default_charset_utf_16_for_json_content_type_is_appended_and_used() {
        given().config(encoder(encoderConfig().defaultCharsetForContentType("UTF-16", "application/json"))).contentType("application/json").body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json; charset=UTF-16");
        assertThat(request.body()).containsExactly(0xFE, 0xFF, 0x00, 0xE4);
    }

    @Test public void
    not_appending_default_charset_leaves_explicit_content_type_as_is_but_still_encodes_with_it() {
        given().config(encoder(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).contentType("text/plain").body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain");
        assertThat(request.body()).containsExactly(0xE4);
    }

    @Test public void
    not_appending_default_charset_leaves_implicit_content_type_without_charset() {
        given().config(encoder(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain");
        assertThat(request.body()).containsExactly(0xE4);
    }

    @Test public void
    not_appending_default_charset_still_encodes_json_in_utf_8() {
        given().config(encoder(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).contentType(ContentType.JSON).body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    not_appending_default_charset_still_encodes_with_the_default_content_charset() {
        EncoderConfig config = encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false).defaultContentCharset("UTF-8");

        given().config(encoder(config)).contentType("text/plain").body(A_UML).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("text/plain");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    not_appending_default_charset_leaves_byte_array_content_type_without_charset() {
        given().config(encoder(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).body(new byte[]{1}).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/octet-stream");
        assertThat(request.body()).containsExactly(1);
    }

    @Test public void
    custom_content_type_encoded_as_json_sends_string_body_in_default_charset() {
        given().config(encoder(encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.JSON))).contentType("application/x-foo").body("{\"a\":\"" + A_UML + "\"}").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-foo; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"a\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    custom_content_type_encoded_as_json_serializes_object_as_json() {
        given().config(encoder(encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.JSON))).contentType("application/x-foo").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-foo; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(("{\"name\":\"" + A_UML + "\"}").getBytes(ISO_8859_1));
    }

    @Test public void
    custom_content_type_encoded_as_xml_serializes_object_as_xml() {
        given().config(encoder(encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.XML))).contentType("application/x-foo").body(new Person(A_UML)).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-foo; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(xml("ISO-8859-1").getBytes(ISO_8859_1));
    }

    @Test public void
    custom_content_type_encoded_as_binary_sends_byte_array_as_is() {
        given().config(encoder(encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.BINARY))).contentType("application/x-foo").body(new byte[]{1, 2}).post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-foo; charset=ISO-8859-1");
        assertThat(request.body()).containsExactly(1, 2);
    }

    @Test public void
    custom_content_type_encoded_as_form_url_encoded_sends_string_body_as_is() {
        given().config(encoder(encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.URLENC))).contentType("application/x-foo").body("a=b c").post(server.url("/b"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/x-foo; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("a=b c");
    }

    @Test public void
    custom_content_type_encoded_as_text_cannot_serialize_an_object() {
        EncoderConfig config = encoderConfig().encodeContentTypeAs("application/x-foo", ContentType.TEXT);

        assertThatThrownBy(() -> given().config(encoder(config)).contentType("application/x-foo").body(map("a", "b")).post(server.url("/b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize because cannot determine how to serialize content-type application/x-foo as TEXT (no serializer supports this format)");

        assertThat(server.requests()).isEmpty();
    }

    // Explicit headers

    @Test public void
    header_name_case_is_kept() {
        given().header("x-Mixed-Case", "1").header("X-UPPER", "2").get(server.url("/h"));

        assertThat(server.onlyRequest().headers()).contains(entry("x-Mixed-Case", "1"), entry("X-UPPER", "2"));
    }

    @Test public void
    header_with_multiple_values_is_sent_as_repeated_headers() {
        given().header("X-Multi", "1", "2").get(server.url("/h"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.headers("X-Multi")).containsExactly("1", "2");
        assertThat(request.headers()).contains(entry("X-Multi", "1"), entry("X-Multi", "2"));
    }

    @Test public void
    header_added_twice_is_sent_as_repeated_headers() {
        given().header("X-Multi", "1").header("X-Multi", "2").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("X-Multi")).containsExactly("1", "2");
    }

    @Test public void
    header_added_twice_with_names_differing_in_case_is_sent_as_repeated_headers_with_the_first_name() {
        given().header("X-Multi", "1").header("x-multi", "2").get(server.url("/h"));

        assertThat(server.onlyRequest().headers()).filteredOn(h -> h.getKey().equalsIgnoreCase("x-multi")).containsExactly(entry("X-Multi", "1"), entry("X-Multi", "2"));
    }

    @Test public void
    non_string_header_value_is_sent_as_its_string_form() {
        given().header("X-Int", 5).get(server.url("/h"));

        assertThat(server.onlyRequest().headers("X-Int")).containsExactly("5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Host", "Connection", "Upgrade"})
    public void
    header_that_the_jdk_client_restricts(String name) {
        Map<String, String> values = Map.of("Host", "example.test", "Connection", "close", "Upgrade", "example/1");

        if (TestClient.isJdk()) {
            // JDK differences table, restricted headers: the JDK client sends them only when the JVM allows it
            assertThatThrownBy(() -> given().header(name, values.get(name)).get(server.url("/h")))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("-Djdk.httpclient.allowRestrictedHeaders=" + name.toLowerCase(Locale.ROOT));
            assertThat(server.requests()).isEmpty();
        } else {
            given().header(name, values.get(name)).get(server.url("/h"));

            // Jetty answers a request with an Upgrade header itself, so the request isn't recorded
            if (!name.equals("Upgrade")) {
                assertThat(server.onlyRequest().headers(name)).containsExactly(values.get(name));
            }
        }
    }

    @Test public void
    accept_defaults_to_any() {
        given().get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("*/*");
    }

    @Test public void
    accept_set_with_header_is_sent_as_is() {
        given().header("Accept", "application/json").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("application/json");
    }

    @Test public void
    accept_set_with_content_type_enum_sends_all_its_media_types() {
        given().accept(ContentType.JSON).get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("application/json, application/javascript, text/javascript, text/json");
    }

    @Test public void
    accept_set_with_string_is_sent_as_is() {
        given().accept("application/xml").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("application/xml");
    }

    @Test public void
    accept_header_set_after_accept_replaces_it() {
        given().accept(ContentType.JSON).header("Accept", "text/plain").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("text/plain");
    }

    @Test public void
    accept_header_added_twice_sends_only_the_last() {
        given().header("Accept", "a/b").header("Accept", "c/d").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("c/d");
    }

    @Test public void
    lower_case_accept_header_replaces_the_default() {
        given().header("accept", "text/plain").get(server.url("/h"));

        assertThat(server.onlyRequest().headers("Accept")).containsExactly("text/plain");
    }

    @Test public void
    content_type_set_with_header_gets_the_default_charset_appended() {
        given().header("Content-Type", "text/plain").body("x").post(server.url("/h"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.headers("Content-Type")).containsExactly("text/plain; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("x");
    }

    @Test public void
    content_type_set_with_header_and_charset_is_used_to_encode_the_body() {
        given().header("Content-Type", "text/plain; charset=UTF-8").body(A_UML).post(server.url("/h"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.headers("Content-Type")).containsExactly("text/plain; charset=UTF-8");
        assertThat(request.body()).containsExactly(A_UML.getBytes(UTF_8));
    }

    @Test public void
    json_content_type_set_with_header_is_the_same_as_content_type() {
        given().header("Content-Type", "application/json").body("{}").post(server.url("/h"));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("application/json");
    }

    @Test public void
    lower_case_content_type_header_is_the_same_as_content_type() {
        given().header("content-type", "text/plain").body("x").post(server.url("/h"));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("text/plain; charset=ISO-8859-1");
    }

    @Test public void
    content_type_enum_text_gets_the_default_charset_appended() {
        given().contentType(ContentType.TEXT).body("x").post(server.url("/h"));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("text/plain; charset=ISO-8859-1");
    }

    @Test public void
    content_type_set_twice_sends_only_the_last() {
        given().contentType("text/plain").contentType("application/json").body("{}").post(server.url("/h"));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("application/json");
    }

    @Test public void
    content_type_header_added_twice_sends_only_the_last() {
        given().header("Content-Type", "text/plain").header("Content-Type", "application/json").body("{}").post(server.url("/h"));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("application/json");
    }

    @Test public void
    content_type_set_with_header_on_get_without_body_is_sent() {
        given().header("Content-Type", "application/json").get(server.url("/h"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.headers("Content-Type")).containsExactly("application/json");
        assertThat(request.hasHeader("Content-Length")).isFalse();
    }

    private static RestAssuredConfig encoder(EncoderConfig encoderConfig) {
        return RestAssuredConfig.config().encoderConfig(encoderConfig);
    }

    private static RestAssuredConfig queryCharset(String charset) {
        return encoder(encoderConfig().defaultQueryParameterCharset(charset));
    }

    private static Map<String, Object> map(Object... namesAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            map.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return map;
    }

    private static File utf8File(Path dir) throws IOException {
        return Files.write(dir.resolve("body.txt"), ("file" + A_UML).getBytes(UTF_8)).toFile();
    }

    private static String xml(String encoding) {
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\" standalone=\"yes\"?><person><name>" + A_UML + "</name></person>";
    }

    @javax.xml.bind.annotation.XmlRootElement
    @jakarta.xml.bind.annotation.XmlRootElement
    public static class Person {
        public String name;

        public Person() {
        }

        public Person(String name) {
            this.name = name;
        }
    }
}
