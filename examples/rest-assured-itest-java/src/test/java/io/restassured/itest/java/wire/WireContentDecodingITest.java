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

import io.restassured.config.DecoderConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import static io.restassured.RestAssured.given;
import static io.restassured.config.DecoderConfig.ContentDecoder.DEFLATE;
import static io.restassured.config.DecoderConfig.ContentDecoder.GZIP;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Which encodings REST Assured asks for, and what the user gets back for each {@code Content-Encoding} of a response.
 * Decoding gzip and deflate is going to move from HttpClient 4 interceptors into REST Assured's core.
 */
public class WireContentDecodingITest extends WireTest {
    private static final String JSON = "{\"message\":\"hello hällo hello hello hello\"}";
    private static final byte[] JSON_BYTES = JSON.getBytes(UTF_8);

    // Accept-Encoding

    @Test public void
    accepts_gzip_and_deflate_by_default() {
        given().get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("gzip,deflate");
    }

    @Test public void
    accepts_only_deflate_when_it_is_the_only_decoder() {
        given().config(config().decoderConfig(decoderConfig().contentDecoders(DEFLATE))).get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("deflate");
    }

    @Test public void
    accepts_only_gzip_when_it_is_the_only_decoder() {
        given().config(config().decoderConfig(decoderConfig().contentDecoders(GZIP))).get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("gzip");
    }

    @Test public void
    accepts_the_decoders_in_the_order_they_are_configured() {
        given().config(config().decoderConfig(decoderConfig().contentDecoders(DEFLATE, GZIP))).get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("deflate,gzip");
    }

    @Test public void
    sends_no_accept_encoding_without_decoders() {
        given().config(config().decoderConfig(decoderConfig().noContentDecoders())).get(server.url("/plain"));

        assertThat(server.onlyRequest().hasHeader("Accept-Encoding")).isFalse();
    }

    @Test public void
    appends_the_decoders_to_an_accept_encoding_set_by_the_user() {
        given().header("Accept-Encoding", "br").get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("br,gzip,deflate");
    }

    @Test public void
    does_not_repeat_a_decoder_already_in_the_accept_encoding_set_by_the_user() {
        given().header("Accept-Encoding", "gzip").get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("gzip,deflate");
    }

    @Test public void
    sends_the_accept_encoding_set_by_the_user_as_is_without_decoders() {
        given().config(config().decoderConfig(decoderConfig().noContentDecoders())).header("Accept-Encoding", "br").get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("br");
    }

    // Decoding responses

    @Test public void
    gzip_response_is_decoded() {
        server.respond("/encoded", encoded("gzip", gzip(JSON_BYTES)));

        Response response = given().get(server.url("/encoded"));

        response.then().body(equalTo(JSON)).body("message", equalTo("hello hällo hello hello hello"));
        assertThat(response.asByteArray()).isEqualTo(JSON_BYTES);
    }

    @Test public void
    gzip_response_keeps_its_content_encoding_and_compressed_content_length_headers() {
        byte[] compressed = gzip(JSON_BYTES);
        server.respond("/encoded", encoded("gzip", compressed));

        given().get(server.url("/encoded")).then()
                .header("Content-Encoding", "gzip")
                .header("Content-Length", String.valueOf(compressed.length));
    }

    @Test public void
    uppercase_gzip_response_is_decoded() {
        server.respond("/encoded", encoded("GZIP", gzip(JSON_BYTES)));

        given().get(server.url("/encoded")).then().body(equalTo(JSON)).header("Content-Encoding", "GZIP");
    }

    @Test public void
    x_gzip_response_is_not_decoded() {
        byte[] compressed = gzip(JSON_BYTES);
        server.respond("/encoded", encoded("x-gzip", compressed));

        Response response = given().get(server.url("/encoded"));

        // Today's behavior: only "gzip" is recognized, so the user gets the compressed bytes
        assertThat(response.asByteArray()).isEqualTo(compressed);
        response.then().header("Content-Encoding", "x-gzip");
    }

    @Test public void
    empty_gzip_response_is_an_empty_body() {
        server.respond("/encoded", encoded("gzip", new byte[0]));

        Response response = given().get(server.url("/encoded"));

        response.then().statusCode(200).body(equalTo("")).header("Content-Encoding", "gzip").header("Content-Length", "0");
        assertThat(response.asByteArray()).isEmpty();
    }

    @Test public void
    zlib_wrapped_deflate_response_is_decoded() {
        server.respond("/encoded", encoded("deflate", zlibDeflate(JSON_BYTES)));

        Response response = given().get(server.url("/encoded"));

        response.then().body(equalTo(JSON)).body("message", equalTo("hello hällo hello hello hello")).header("Content-Encoding", "deflate");
        assertThat(response.asByteArray()).isEqualTo(JSON_BYTES);
    }

    @Test public void
    raw_deflate_response_fails_when_the_body_is_read_by_default() {
        server.respond("/encoded", encoded("deflate", rawDeflate(JSON_BYTES)));

        Response response = given().get(server.url("/encoded"));

        // Today's behavior: the request succeeds, and the error only shows when the body is read
        response.then().statusCode(200).header("Content-Encoding", "deflate");
        assertThatThrownBy(response::asByteArray).isInstanceOf(ZipException.class).hasMessage("incorrect header check");
    }

    @Test public void
    raw_deflate_response_is_decoded_with_no_wrap() {
        server.respond("/encoded", encoded("deflate", rawDeflate(JSON_BYTES)));

        Response response = given().config(config().decoderConfig(decoderConfig().useNoWrapForInflateDecoding(true))).get(server.url("/encoded"));

        response.then().body(equalTo(JSON));
        assertThat(response.asByteArray()).isEqualTo(JSON_BYTES);
    }

    @Test public void
    zlib_wrapped_deflate_response_fails_when_the_body_is_read_with_no_wrap() {
        server.respond("/encoded", encoded("deflate", zlibDeflate(JSON_BYTES)));

        Response response = given().config(config().decoderConfig(decoderConfig().useNoWrapForInflateDecoding(true))).get(server.url("/encoded"));

        response.then().statusCode(200);
        assertThatThrownBy(response::asString).isInstanceOf(ZipException.class);
    }

    @Test public void
    response_with_both_encodings_is_decoded_in_reverse_order() {
        server.respond("/encoded", encoded("deflate, gzip", gzip(zlibDeflate(JSON_BYTES))));

        given().get(server.url("/encoded")).then().body(equalTo(JSON));
    }

    @Test public void
    response_with_an_unknown_encoding_is_not_decoded() {
        byte[] body = {1, 2, 3, (byte) 0xFF};
        server.respond("/encoded", encoded("br", body));

        Response response = given().get(server.url("/encoded"));

        assertThat(response.asByteArray()).isEqualTo(body);
        response.then().header("Content-Encoding", "br").header("Content-Length", "4");
    }

    @Test public void
    gzip_response_is_not_decoded_without_decoders() {
        byte[] compressed = gzip(JSON_BYTES);
        server.respond("/encoded", encoded("gzip", compressed));

        Response response = given().config(config().decoderConfig(decoderConfig().noContentDecoders())).get(server.url("/encoded"));

        assertThat(response.asByteArray()).isEqualTo(compressed);
        response.then().header("Content-Encoding", "gzip").header("Content-Length", String.valueOf(compressed.length));
    }

    @Test public void
    deflate_response_is_not_decoded_without_decoders() {
        byte[] compressed = zlibDeflate(JSON_BYTES);
        server.respond("/encoded", encoded("deflate", compressed));

        Response response = given().config(config().decoderConfig(decoderConfig().noContentDecoders())).get(server.url("/encoded"));

        assertThat(response.asByteArray()).isEqualTo(compressed);
    }

    @Test public void
    gzip_response_is_not_decoded_when_only_deflate_is_configured() {
        byte[] compressed = gzip(JSON_BYTES);
        server.respond("/encoded", encoded("gzip", compressed));

        Response response = given().config(config().decoderConfig(decoderConfig().contentDecoders(DEFLATE))).get(server.url("/encoded"));

        assertThat(response.asByteArray()).isEqualTo(compressed);
    }

    @Test public void
    deflate_response_is_not_decoded_when_only_gzip_is_configured() {
        byte[] compressed = zlibDeflate(JSON_BYTES);
        server.respond("/encoded", encoded("deflate", compressed));

        Response response = given().config(config().decoderConfig(decoderConfig().contentDecoders(GZIP))).get(server.url("/encoded"));

        assertThat(response.asByteArray()).isEqualTo(compressed);
    }

    @Test public void
    decoder_config_constructor_with_decoders_sets_the_accept_encoding() {
        given().config(config().decoderConfig(new DecoderConfig(GZIP))).get(server.url("/plain"));

        assertThat(server.onlyRequest().headers("Accept-Encoding")).containsExactly("gzip");
    }

    private static Responder encoded(String contentEncoding, byte[] body) {
        return (request, response) -> {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Content-Encoding", contentEncoding);
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        };
    }

    private static byte[] gzip(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static byte[] zlibDeflate(byte[] bytes) {
        return deflate(bytes, new Deflater());
    }

    private static byte[] rawDeflate(byte[] bytes) {
        return deflate(bytes, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
    }

    private static byte[] deflate(byte[] bytes, Deflater deflater) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflate = new DeflaterOutputStream(out, deflater)) {
            deflate.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deflater.end();
        }
        return out.toByteArray();
    }
}
