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

package io.restassured.internal;

import io.restassured.builder.ResponseBuilder;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.DecoderConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.http.HttpContextDecorator;
import io.restassured.internal.http.HttpResponseDecorator;
import io.restassured.internal.support.CloseHTTPClientConnectionInputStreamWrapper;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.XmlPath.CompatibilityMode;
import io.restassured.path.xml.config.XmlPathConfig;
import io.restassured.response.Response;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Characterizes {@link RestAssuredResponseOptionsImpl}: parsing of the HTTP response, the body accessors and how often the
 * body can be read, path evaluation per content type, object mapping, charset resolution, header/cookie/status accessors,
 * timing and printing.
 */
class RestAssuredResponseOptionsImplTest {

    private static final String NL = System.lineSeparator();
    private static final String ISO = "ISO-8859-1";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureSystemOut() {
        originalOut = System.out;
        System.setOut(new PrintStream(out, true));
    }

    @AfterEach
    void restoreSystemOut() {
        System.setOut(originalOut);
    }

    // Status, headers and cookies

    @Test
    void status_code_is_minus_one_when_not_set_and_also_when_zero() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        assertThat(response.statusCode()).isEqualTo(-1);
        assertThat(response.getStatusCode()).isEqualTo(-1);

        response.setStatusCode(0);
        assertThat(response.statusCode()).isEqualTo(-1);

        response.setStatusCode(201);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.getStatusCode()).isEqualTo(201);
    }

    @Test
    void status_line_that_is_not_a_string_is_converted_to_a_string() {
        // ResponseBuilder.build() stores the Integer status code as status line when none is set
        Response response = new ResponseBuilder().setStatusCode(200).build();

        assertThat(response.statusLine()).isEqualTo("200");
        assertThat(response.getStatusLine()).isEqualTo("200");
        assertThat(new ResponseBuilder().build().statusLine()).isEqualTo("-1");
    }

    @Test
    void status_line_and_content_type_are_null_when_not_set() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        assertThat(response.statusLine()).isNull();
        assertThat(response.getStatusLine()).isNull();
        assertThat(response.contentType()).isNull();
        assertThat(response.getContentType()).isNull();
    }

    @Test
    void headers_are_empty_when_not_set() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        assertThat(response.headers().exist()).isFalse();
        assertThat(response.getHeaders().exist()).isFalse();
        assertThat(response.getResponseHeaders()).isNull();
    }

    @Test
    void headers_returns_the_headers_that_were_set_even_when_empty() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        Headers empty = new Headers();
        response.setResponseHeaders(empty);

        assertThat(response.headers()).isSameAs(empty);
    }

    @Test
    void header_returns_last_value_and_null_for_missing_header() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        Headers headers = new Headers(new Header("a", "1"), new Header("a", "2"), new Header("b", "3"));
        response.setResponseHeaders(headers);

        assertThat(response.headers()).isSameAs(headers);
        assertThat(response.header("a")).isEqualTo("2");
        assertThat(response.getHeader("b")).isEqualTo("3");
        assertThat(response.header("c")).isNull();
        assertThat(response.getResponseHeaders()).isSameAs(headers);
    }

    @Test
    void header_with_null_name_throws() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        response.setResponseHeaders(new Headers());

        assertThatThrownBy(() -> response.header(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("name cannot be null");
    }

    @Test
    void header_throws_null_pointer_exception_when_no_headers_are_set() {
        assertThatThrownBy(() -> new RestAssuredResponseImpl().header("a")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void cookies_are_empty_when_not_set() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        assertThat(response.cookies()).isEmpty();
        assertThat(response.getCookies()).isEmpty();
        assertThat(response.cookie("a")).isNull();
        assertThat(response.getCookie("a")).isNull();
        assertThat(response.detailedCookies().exist()).isFalse();
        assertThat(response.getDetailedCookies().exist()).isFalse();
        assertThat(response.detailedCookie("a")).isNull();
        assertThatThrownBy(() -> response.cookies().put("a", "b")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cookies_are_returned_in_order_with_the_last_value_for_a_repeated_name() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        Cookie last = new Cookie.Builder("z", "3").build();
        Cookies cookies = new Cookies(new Cookie.Builder("z", "1").build(), new Cookie.Builder("a", "2").build(), last);
        response.setCookies(cookies);

        assertThat(response.cookies()).containsExactly(entry("z", "3"), entry("a", "2"));
        assertThat(response.getCookies()).containsExactly(entry("z", "3"), entry("a", "2"));
        assertThatThrownBy(() -> response.cookies().put("a", "b")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(response.cookie("z")).isEqualTo("3");
        assertThat(response.getCookie("a")).isEqualTo("2");
        assertThat(response.detailedCookies()).isSameAs(cookies);
        assertThat(response.getDetailedCookies()).isSameAs(cookies);
        assertThat(response.detailedCookie("z")).isSameAs(last);
        assertThat(response.getDetailedCookie("z")).isSameAs(last);
    }

    @Test
    void cookie_with_null_name_throws() {
        assertThatThrownBy(() -> new RestAssuredResponseImpl().cookie(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("name cannot be null");
    }

    @Test
    void session_id_is_the_cookie_named_by_the_session_id_name() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        response.setCookies(new Cookies(new Cookie.Builder("JSESSIONID", "abc").build(), new Cookie.Builder("other", "x").build()));
        response.setSessionIdName("JSESSIONID");

        assertThat(response.sessionId()).isEqualTo("abc");
        assertThat(response.getSessionId()).isEqualTo("abc");
        assertThat(response.getSessionIdName()).isEqualTo("JSESSIONID");

        response.setSessionIdName("missing");
        assertThat(response.sessionId()).isNull();
    }

    @Test
    void session_id_throws_when_no_session_id_name_is_set() {
        assertThatThrownBy(() -> new RestAssuredResponseImpl().sessionId()).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("name cannot be null");
    }

    @Test
    void getters_return_what_was_set() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        RestAssuredConfig config = RestAssuredConfig.config();
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        DecoderConfig decoderConfig = DecoderConfig.decoderConfig();
        Map<String, Object> properties = new HashMap<>();
        BasicHttpContext context = new BasicHttpContext();
        Object connectionManager = new Object();

        response.setConfig(config);
        response.setRpr(rpr);
        response.setDecoderConfig(decoderConfig);
        response.setFilterContextProperties(properties);
        response.setApacheHttpContext(context);
        response.setConnectionManager(connectionManager);
        response.setDefaultContentType("application/json");
        response.setHasExpectations(true);
        response.setContent("body");

        assertThat(response.getConfig()).isSameAs(config);
        assertThat(response.getRpr()).isSameAs(rpr);
        assertThat(response.getDecoderConfig()).isSameAs(decoderConfig);
        assertThat(response.getFilterContextProperties()).isSameAs(properties);
        assertThat(response.getApacheHttpContext()).isSameAs(context);
        assertThat(response.getConnectionManager()).isSameAs(connectionManager);
        assertThat(response.getDefaultContentType()).isEqualTo("application/json");
        assertThat(response.getHasExpectations()).isTrue();
        assertThat(response.getContent()).isEqualTo("body");
        assertThat(response.body()).isSameAs(response);
        assertThat(response.getBody()).isSameAs(response);
        assertThat(response.response()).isSameAs(response);
        assertThat(response.andReturn()).isSameAs(response);
        assertThat(response.thenReturn()).isSameAs(response);
    }

    // parseResponse

    @Test
    void parse_response_reads_status_headers_cookies_content_type_and_context() {
        BasicHttpResponse base = httpResponse("application/json; charset=UTF-8");
        base.addHeader("Set-Cookie", "a=1");
        base.addHeader("Set-Cookie", "JSESSIONID=abc; Path=/");
        HttpContextDecorator context = new HttpContextDecorator();
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        response.setSessionIdName("JSESSIONID");

        response.parseResponse(new HttpResponseDecorator(base, context, null), null, false, rpr);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.statusLine()).isEqualTo("HTTP/1.1 201 Created");
        assertThat(response.contentType()).isEqualTo("application/json; charset=UTF-8");
        assertThat(response.headers().asList()).extracting(Header::getName)
                .containsExactly("Content-Type", "Set-Cookie", "Set-Cookie");
        assertThat(response.headers().getValues("Set-Cookie")).containsExactly("a=1", "JSESSIONID=abc; Path=/");
        assertThat(response.cookies()).containsExactly(entry("a", "1"), entry("JSESSIONID", "abc"));
        assertThat(response.detailedCookie("JSESSIONID").getPath()).isEqualTo("/");
        assertThat(response.sessionId()).isEqualTo("abc");
        assertThat(response.getRpr()).isSameAs(rpr);
        assertThat(response.getDefaultContentType()).isNull();
        assertThat(response.getHasExpectations()).isFalse();
        assertThat(response.getApacheHttpContext()).isSameAs(context.getDelegate());
        assertThat(response.getContent()).isNull();
    }

    @Test
    void parse_response_without_content_type_header_sets_empty_content_type_and_no_cookies() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        response.parseResponse(new HttpResponseDecorator(httpResponse(null), new HttpContextDecorator(), null), null, false, new ResponseParserRegistrar());

        assertThat(response.contentType()).isEmpty();
        assertThat(response.detailedCookies().exist()).isFalse();
        assertThat(response.headers().exist()).isFalse();
    }

    @Test
    void parse_response_takes_default_content_type_from_default_parser() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.XML);
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        response.parseResponse(new HttpResponseDecorator(httpResponse(null), new HttpContextDecorator(), null), null, false, rpr);

        assertThat(response.getDefaultContentType()).isEqualTo("application/xml");
    }

    @Test
    void parse_response_without_body_assertions_keeps_the_stream() {
        InputStream stream = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        response.parseResponse(new HttpResponseDecorator(httpResponse("text/plain"), new HttpContextDecorator(), null), stream, false, new ResponseParserRegistrar());

        assertThat(response.getContent()).isSameAs(stream);
        assertThat(response.isInputStream()).isTrue();
    }

    @Test
    void parse_response_with_body_assertions_reads_and_closes_the_stream() {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream stream = new ByteArrayInputStream("åäö".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        response.parseResponse(new HttpResponseDecorator(httpResponse("text/plain; charset=UTF-8"), new HttpContextDecorator(), null), stream, true,
                new ResponseParserRegistrar());

        assertThat(response.getContent()).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
        assertThat(closed).isTrue();
        assertThat(response.isInputStream()).isFalse();
        assertThat(response.getHasExpectations()).isTrue();
        assertThat(response.asString()).isEqualTo("åäö");
    }

    @Test
    void parse_response_with_body_assertions_keeps_string_content_and_converts_null_to_empty_string() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        response.parseResponse(new HttpResponseDecorator(httpResponse("text/plain"), new HttpContextDecorator(), null), "", true, new ResponseParserRegistrar());
        assertThat(response.getContent()).isEqualTo("");

        RestAssuredResponseImpl nullContent = new RestAssuredResponseImpl();
        nullContent.parseResponse(new HttpResponseDecorator(httpResponse("text/plain"), new HttpContextDecorator(), null), null, true, new ResponseParserRegistrar());
        assertThat(nullContent.getContent()).isEqualTo("");
    }

    @Test
    void parse_response_with_body_assertions_reads_a_reader() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        response.parseResponse(new HttpResponseDecorator(httpResponse("text/plain"), new HttpContextDecorator(), null), new StringReader("text"), true,
                new ResponseParserRegistrar());

        assertThat(response.getContent()).isEqualTo("text");
    }

    @Test
    void parse_response_wraps_illegal_state_exception_from_reading_the_body() {
        IllegalStateException failure = new IllegalStateException("boom");
        InputStream stream = new InputStream() {
            @Override
            public int read() {
                throw failure;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                throw failure;
            }
        };
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();

        assertThatThrownBy(() -> response.parseResponse(new HttpResponseDecorator(httpResponse("text/plain"), new HttpContextDecorator(), null), stream,
                true, new ResponseParserRegistrar()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to parse response.")
                .hasCause(failure);
    }

    // Body as String, byte[] and InputStream

    @Test
    void null_content_is_an_empty_body() throws IOException {
        RestAssuredResponseImpl response = response(null, "text/plain");

        assertThat(response.asString()).isEmpty();
        assertThat(response.asByteArray()).isEmpty();
        assertThat(response.isInputStream()).isFalse();
        InputStream stream = response.asInputStream();
        assertThat(stream).isInstanceOf(CloseHTTPClientConnectionInputStreamWrapper.class);
        assertThat(stream.read()).isEqualTo(-1);
    }

    @Test
    void string_content_can_be_read_any_number_of_times() throws IOException {
        RestAssuredResponseImpl response = response("åäö", "text/plain; charset=UTF-8");

        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.asByteArray()).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
        assertThat(response.asInputStream()).isInstanceOf(ByteArrayInputStream.class);
        assertThat(readFully(response.asInputStream())).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
        assertThat(readFully(response.asInputStream())).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getContent()).isEqualTo("åäö");
    }

    @Test
    void string_content_is_encoded_with_decoder_default_charset_when_no_charset_is_given() {
        RestAssuredResponseImpl response = response("åäö", "text/plain");

        assertThat(response.asByteArray()).isEqualTo("åäö".getBytes(StandardCharsets.ISO_8859_1));
        assertThat(readFully(response.asInputStream())).isEqualTo("åäö".getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void byte_array_content_is_decoded_with_charset_and_returned_as_is() {
        byte[] bytes = "åäö".getBytes(StandardCharsets.UTF_8);
        RestAssuredResponseImpl response = response(bytes, "text/plain; charset=UTF-8");

        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.asByteArray()).isSameAs(bytes);
        assertThat(response.asInputStream()).isInstanceOf(ByteArrayInputStream.class);
        assertThat(readFully(response.asInputStream())).isEqualTo(bytes);
        assertThat(readFully(response.asInputStream())).isEqualTo(bytes);
        assertThat(response.isInputStream()).isFalse();
    }

    @Test
    void byte_array_content_with_expectations_is_returned_as_is() {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        RestAssuredResponseImpl response = response(bytes, "text/plain");
        response.setHasExpectations(true);

        assertThat(response.asByteArray()).isSameAs(bytes);
    }

    @Test
    void string_content_with_expectations_is_encoded_with_the_response_charset() {
        RestAssuredResponseImpl response = response("åäö", "text/plain; charset=UTF-16");
        response.setHasExpectations(true);

        assertThat(response.asByteArray()).isEqualTo("åäö".getBytes(StandardCharsets.UTF_16));
    }

    @Test
    void stream_content_is_read_once_by_as_string_and_then_kept_as_bytes() {
        InputStream stream = new ByteArrayInputStream("åäö".getBytes(StandardCharsets.UTF_8));
        RestAssuredResponseImpl response = response(stream, "text/plain; charset=UTF-8");

        assertThat(response.isInputStream()).isTrue();
        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.isInputStream()).isFalse();
        assertThat(response.getContent()).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
        assertThat(response.asString()).isEqualTo("åäö");
        assertThat(response.asByteArray()).isEqualTo("åäö".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void stream_content_is_read_once_by_as_byte_array_and_then_kept_as_bytes() {
        InputStream stream = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        RestAssuredResponseImpl response = response(stream, "text/plain");

        byte[] bytes = response.asByteArray();

        assertThat(bytes).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getContent()).isSameAs(bytes);
        assertThat(response.asByteArray()).isSameAs(bytes);
        assertThat(response.asString()).isEqualTo("abc");
    }

    @Test
    void stream_content_is_handed_out_wrapped_and_not_buffered_by_as_input_stream() throws IOException {
        InputStream stream = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
        RestAssuredResponseImpl response = response(stream, "text/plain");

        InputStream returned = response.asInputStream();

        assertThat(returned).isInstanceOf(CloseHTTPClientConnectionInputStreamWrapper.class);
        assertThat(readFully(returned)).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getContent()).isSameAs(stream);
        assertThat(response.isInputStream()).isTrue();
        // The stream is consumed, so nothing is left to read
        assertThat(response.asString()).isEmpty();
    }

    @Test
    void stream_content_closes_the_stream_when_read() {
        AtomicInteger closed = new AtomicInteger();
        InputStream stream = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed.incrementAndGet();
                super.close();
            }
        };
        RestAssuredResponseImpl response = response(stream, "text/plain");

        response.asString();

        assertThat(closed).hasValue(1);
    }

    @Test
    void as_string_with_force_platform_default_charset_ignores_decoder_config() {
        byte[] bytes = "åäö".getBytes(Charset.defaultCharset());
        RestAssuredResponseImpl response = response(bytes, "text/plain");
        response.setDecoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-16"));

        assertThat(response.asString(true)).isEqualTo("åäö");
        assertThat(response.asString(false)).isEqualTo(new String(bytes, StandardCharsets.UTF_16));
    }

    @Test
    void charset_of_content_type_wins_over_forced_platform_default() {
        byte[] bytes = "åäö".getBytes(StandardCharsets.UTF_16);
        RestAssuredResponseImpl response = response(bytes, "text/plain; charset=UTF-16");

        assertThat(response.asString(true)).isEqualTo("åäö");
    }

    @Test
    void charset_is_taken_from_default_content_type_when_content_type_is_blank() {
        byte[] bytes = "åäö".getBytes(StandardCharsets.UTF_16);
        RestAssuredResponseImpl response = response(bytes, "");
        response.setDefaultContentType("application/json; charset=UTF-16");

        assertThat(response.asString()).isEqualTo("åäö");
    }

    @Test
    void decoder_config_charset_for_content_type_is_used_when_no_charset_is_given() {
        byte[] bytes = "åäö".getBytes(StandardCharsets.UTF_8);
        RestAssuredResponseImpl response = response(bytes, "application/json");

        assertThat(response.asString()).isEqualTo("åäö");
    }

    @Test
    void platform_default_charset_is_used_without_decoder_config() {
        byte[] bytes = "åäö".getBytes(Charset.defaultCharset());
        RestAssuredResponseImpl response = response(bytes, "text/plain");
        response.setDecoderConfig(null);

        assertThat(response.asString()).isEqualTo("åäö");
    }

    @Test
    void binary_charset_is_replaced_by_decoder_config_charset_for_content_type() {
        byte[] bytes = "åäö".getBytes(StandardCharsets.UTF_16);
        RestAssuredResponseImpl response = response(bytes, "text/plain; charset=BINARY");
        response.setDecoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-16"));

        assertThat(response.asString()).isEqualTo("åäö");
    }

    @Test
    void unsupported_charset_throws_unsupported_encoding_exception() {
        RestAssuredResponseImpl bytes = response("abc".getBytes(StandardCharsets.UTF_8), "text/plain; charset=nope");
        RestAssuredResponseImpl string = response("abc", "text/plain; charset=nope");

        assertThatThrownBy(bytes::asString).isExactlyInstanceOf(UnsupportedEncodingException.class).hasMessage("nope");
        assertThatThrownBy(string::asByteArray).isExactlyInstanceOf(UnsupportedEncodingException.class).hasMessage("nope");
        assertThatThrownBy(string::asInputStream).isExactlyInstanceOf(UnsupportedEncodingException.class).hasMessage("nope");
    }

    // Printing

    @Test
    void print_prints_the_body_and_keeps_it_as_string() {
        RestAssuredResponseImpl response = response(new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8)), "application/json");

        assertThat(response.print()).isEqualTo("{\"a\":1}");

        assertThat(out.toString()).isEqualTo("{\"a\":1}" + NL);
        assertThat(response.getContent()).isEqualTo("{\"a\":1}");
    }

    @Test
    void pretty_print_prints_the_prettified_body() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "application/json");

        String pretty = response.prettyPrint();

        assertThat(pretty).isEqualTo("{\n    \"a\": 1\n}");
        assertThat(response.asPrettyString()).isEqualTo(pretty);
        assertThat(out.toString()).isEqualTo(pretty + NL);
    }

    @Test
    void peek_prints_the_whole_response_without_blacklisted_headers() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "application/json");
        response.setStatusLine("HTTP/1.1 200 OK");
        response.setResponseHeaders(new Headers(new Header("Content-Type", "application/json"), new Header("Secret", "s")));
        response.setConfig(RestAssuredConfig.config().logConfig(LogConfig.logConfig().blacklistHeader("Secret")));

        assertThat(response.peek()).isSameAs(response);

        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK" + NL + "Content-Type: application/json" + NL + "Secret: [ BLACKLISTED ]" + NL + NL
                + "{\"a\":1}" + NL);
    }

    @Test
    void pretty_peek_prints_the_whole_response_pretty_printed() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "application/json");
        response.setStatusLine("HTTP/1.1 200 OK");
        response.setResponseHeaders(new Headers(new Header("Content-Type", "application/json")));

        assertThat(response.prettyPeek()).isSameAs(response);

        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK" + NL + "Content-Type: application/json" + NL + NL
                + "{\n    \"a\": 1\n}" + NL);
    }

    @Test
    void peek_without_config_prints_all_headers() {
        RestAssuredResponseImpl response = response("x", "text/plain");
        response.setConfig(null);
        response.setStatusLine("HTTP/1.1 200 OK");
        response.setResponseHeaders(new Headers(new Header("Secret", "s")));

        response.peek();

        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK" + NL + "Secret: s" + NL + NL + "x" + NL);
    }

    // Paths

    @Test
    void path_uses_json_path_for_json_content_type() {
        RestAssuredResponseImpl response = response("{\"a\":{\"b\":1.5}}", "application/json");

        assertThat(response.<Float>path("a.b")).isEqualTo(1.5f);
        assertThat(response.<Float>path("%s.%s", "a", "b")).isEqualTo(1.5f);
    }

    @Test
    void path_uses_number_return_type_from_json_config() {
        RestAssuredResponseImpl response = response("{\"a\":1.5}", "application/json");
        response.setConfig(RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL)));

        assertThat(response.<BigDecimal>path("a")).isEqualTo(new BigDecimal("1.5"));
        assertThat(response.jsonPath().<BigDecimal>get("a")).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    void path_uses_xml_path_for_xml_content_type() {
        RestAssuredResponseImpl response = response("<a><b>x</b></a>", "application/xml");

        assertThat(response.<String>path("a.b")).isEqualTo("x");
        assertThat(response.<String>path("a.%s", "b")).isEqualTo("x");
    }

    @Test
    void path_uses_declared_namespaces_from_xml_config() {
        RestAssuredResponseImpl response = response("<foo xmlns:ns=\"http://localhost/\"><ns:bar>x</ns:bar></foo>", "application/xml");
        response.setConfig(RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().declareNamespace("ns", "http://localhost/")));

        assertThat(response.<String>path("foo.ns:bar")).isEqualTo("x");
        assertThat(response.xmlPath().getString("foo.ns:bar")).isEqualTo("x");
    }

    @Test
    void path_returns_the_xml_path_itself_for_html_content_type() {
        RestAssuredResponseImpl response = response("<html><body><p>x</p></body></html>", "text/html");

        Object result = response.path("html.body.p");

        assertThat(result).isInstanceOf(XmlPath.class);
        assertThat(((XmlPath) result).getString("html.body.p")).isEqualTo("x");
    }

    @Test
    void path_uses_default_content_type_when_content_type_is_empty() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "");
        response.setDefaultContentType("application/json");

        assertThat(response.<Integer>path("a")).isEqualTo(1);
    }

    @Test
    void path_uses_custom_parser_registered_for_content_type() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "text/plain");
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("text/plain", Parser.JSON);
        response.setRpr(rpr);

        assertThat(response.<Integer>path("a")).isEqualTo(1);
    }

    @Test
    void path_without_content_type_and_default_parser_throws() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "");

        assertThatThrownBy(() -> response.path("a"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot invoke the path method because no content-type was present in the response and no default parser has been set.\n\n"
                        + "You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n");
    }

    @Test
    void path_with_unsupported_content_type_throws() {
        RestAssuredResponseImpl response = response("a", "text/plain");

        assertThatThrownBy(() -> response.path("a"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot determine which path implementation to use because the content-type text/plain doesn't map to a path implementation.");
    }

    @Test
    void path_with_null_content_type_throws() {
        RestAssuredResponseImpl response = response("a", null);

        assertThatThrownBy(() -> response.path("a"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot determine which path implementation to use because the content-type null doesn't map to a path implementation.");
    }

    @Test
    void path_with_null_path_throws() {
        assertThatThrownBy(() -> response("{}", "application/json").path(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Path cannot be null");
    }

    @Test
    void json_path_decodes_body_with_response_charset() {
        RestAssuredResponseImpl response = response("{\"a\":\"åäö\"}".getBytes(StandardCharsets.UTF_16), "application/json; charset=UTF-16");

        assertThat(response.jsonPath().getString("a")).isEqualTo("åäö");
    }

    @Test
    void json_path_with_explicit_config_uses_that_config() {
        RestAssuredResponseImpl response = response("{\"a\":1.5}", "application/json");

        assertThat(response.jsonPath(JsonPathConfig.jsonPathConfig().numberReturnType(NumberReturnType.DOUBLE)).<Double>get("a")).isEqualTo(1.5d);
        assertThatThrownBy(() -> response.jsonPath(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("JsonPathConfig cannot be null");
    }

    @Test
    void xml_path_with_explicit_config_uses_that_config() {
        RestAssuredResponseImpl response = response("<foo xmlns:ns=\"http://localhost/\"><ns:bar>x</ns:bar></foo>", "application/xml");

        assertThat(response.xmlPath(XmlPathConfig.xmlPathConfig().declaredNamespace("ns", "http://localhost/")).getString("foo.ns:bar")).isEqualTo("x");
        assertThat(response.xmlPath().getString("foo.ns:bar")).isEmpty();
        assertThatThrownBy(() -> response.xmlPath((XmlPathConfig) null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("XmlPathConfig cannot be null");
    }

    @Test
    void xml_path_and_html_path_with_compatibility_mode() {
        RestAssuredResponseImpl response = response("<html><body><p>x</p><br></body></html>", "text/html");

        assertThat(response.htmlPath().getString("html.body.p")).isEqualTo("x");
        assertThat(response.xmlPath(CompatibilityMode.HTML).getString("html.body.p")).isEqualTo("x");
        assertThatThrownBy(() -> response.xmlPath((CompatibilityMode) null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compatibility mode cannot be null");
    }

    // Object mapping

    @Test
    void as_with_object_mapper_passes_type_charset_content_type_and_body() {
        RestAssuredResponseImpl response = response("{\"a\":1}".getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8");
        CapturingMapper mapper = new CapturingMapper();

        Object result = response.as(String.class, mapper);

        assertThat(result).isEqualTo("mapped");
        ObjectMapperDeserializationContext ctx = mapper.context.get();
        assertThat(ctx.getType()).isEqualTo(String.class);
        assertThat(ctx.getCharset()).isEqualTo("UTF-8");
        assertThat(ctx.getContentType()).isEqualTo("text/plain; charset=UTF-8");
        assertThat(ctx.getDataToDeserialize().asString()).isEqualTo("{\"a\":1}");
        assertThat(ctx.getDataToDeserialize().asByteArray()).isEqualTo("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat(readFully(ctx.getDataToDeserialize().asInputStream())).isEqualTo("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat((Object) response.as((java.lang.reflect.Type) String.class, mapper)).isEqualTo("mapped");
    }

    @Test
    void as_with_null_arguments_throws() {
        RestAssuredResponseImpl response = response("{}", "application/json");

        assertThatThrownBy(() -> response.as(String.class, (ObjectMapper) null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object mapper cannot be null");
        assertThatThrownBy(() -> response.as(String.class, (ObjectMapperType) null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Object mapper type cannot be null");
        assertThatThrownBy(() -> response.as((TypeRef<Object>) null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type ref cannot be null");
    }

    @Test
    void as_uses_content_type_of_custom_parser_and_response_charset() {
        CapturingMapper mapper = new CapturingMapper();
        RestAssuredResponseImpl response = response("{\"a\":1}", "text/plain; charset=UTF-16");
        response.setConfig(RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(mapper)));
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("text/plain", Parser.JSON);
        response.setRpr(rpr);

        assertThat((Object) response.as(String.class)).isEqualTo("mapped");

        assertThat(mapper.context.get().getContentType()).isEqualTo("application/json");
        assertThat(mapper.context.get().getCharset()).isEqualTo("UTF-16");
        assertThat(mapper.context.get().getType()).isEqualTo(String.class);
    }

    @Test
    void as_uses_default_content_type_when_content_type_is_empty() {
        CapturingMapper mapper = new CapturingMapper();
        RestAssuredResponseImpl response = response("{\"a\":1}", "");
        response.setDefaultContentType("application/json");
        response.setConfig(RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(mapper)));

        assertThat((Object) response.as(new TypeRef<List<String>>() {
        })).isEqualTo("mapped");

        assertThat(mapper.context.get().getContentType()).isEqualTo("application/json");
        assertThat(mapper.context.get().getType().getTypeName()).isEqualTo("java.util.List<java.lang.String>");
    }

    @Test
    void as_maps_json_with_jackson() {
        RestAssuredResponseImpl response = response("{\"a\":1}", "application/json");

        Map<?, ?> map = response.as(Map.class);
        Map<?, ?> mapWithType = response.as((java.lang.reflect.Type) Map.class);
        Map<?, ?> mapWithMapperType = response.as(Map.class, ObjectMapperType.GSON);
        Map<?, ?> mapWithTypeAndMapperType = response.as((java.lang.reflect.Type) Map.class, ObjectMapperType.JACKSON_2);

        assertThat(map).isEqualTo(Map.of("a", 1));
        assertThat(mapWithType).isEqualTo(Map.of("a", 1));
        assertThat(mapWithMapperType).isEqualTo(Map.of("a", 1.0d));
        assertThat(mapWithTypeAndMapperType).isEqualTo(Map.of("a", 1));
    }

    @Test
    void as_without_content_type_and_default_parser_throws() {
        RestAssuredResponseImpl response = response("{}", "");

        assertThatThrownBy(() -> response.as(Map.class))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot parse content to interface java.util.Map because no content-type was present in the response and no default parser has been set.\n"
                        + "You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n\n"
                        + "or you can specify an explicit ObjectMapper using as(interface java.util.Map, <ObjectMapper>);");
    }

    // Time

    @Test
    void time_is_minus_one_without_timing_information() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        assertThat(response.time()).isEqualTo(-1);
        assertThat(response.timeIn(TimeUnit.SECONDS)).isEqualTo(-1);

        response.setFilterContextProperties(new HashMap<>());
        assertThat(response.getTime()).isEqualTo(-1);
        assertThat(response.getTimeIn(TimeUnit.MINUTES)).isEqualTo(-1);
    }

    @Test
    void time_is_taken_from_filter_context_properties() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        Map<String, Object> properties = new HashMap<>();
        properties.put(TimingFilter.RESPONSE_TIME_MILLISECONDS, 2500L);
        response.setFilterContextProperties(properties);

        assertThat(response.time()).isEqualTo(2500);
        assertThat(response.getTime()).isEqualTo(2500);
        assertThat(response.timeIn(TimeUnit.MILLISECONDS)).isEqualTo(2500);
        assertThat(response.timeIn(TimeUnit.SECONDS)).isEqualTo(2);
        assertThat(response.getTimeIn(TimeUnit.MICROSECONDS)).isEqualTo(2_500_000);
    }

    @Test
    void time_stored_as_integer_is_widened() {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        Map<String, Object> properties = new HashMap<>();
        properties.put(TimingFilter.RESPONSE_TIME_MILLISECONDS, 42);
        response.setFilterContextProperties(properties);

        assertThat(response.time()).isEqualTo(42);
    }

    @Test
    void time_in_null_unit_throws() {
        assertThatThrownBy(() -> new RestAssuredResponseImpl().timeIn(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("TimeUnit cannot be null");
    }

    // Helpers

    private static RestAssuredResponseImpl response(Object content, String contentType) {
        RestAssuredResponseImpl response = new RestAssuredResponseImpl();
        response.setContent(content);
        response.setContentType(contentType);
        response.setDecoderConfig(DecoderConfig.decoderConfig().defaultContentCharset(ISO));
        response.setConfig(RestAssuredConfig.config());
        response.setRpr(new ResponseParserRegistrar());
        return response;
    }

    private static BasicHttpResponse httpResponse(String contentType) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 201, "Created"));
        if (contentType != null) {
            response.addHeader("Content-Type", contentType);
        }
        return response;
    }

    private static byte[] readFully(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CapturingMapper implements ObjectMapper {
        final AtomicReference<ObjectMapperDeserializationContext> context = new AtomicReference<>();

        @Override
        public Object deserialize(ObjectMapperDeserializationContext context) {
            this.context.set(context);
            return "mapped";
        }

        @Override
        public Object serialize(ObjectMapperSerializationContext context) {
            throw new UnsupportedOperationException();
        }
    }
}
