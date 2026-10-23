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

package io.restassured.internal.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import static io.restassured.config.DecoderConfig.ContentDecoder.DEFLATE;
import static io.restassured.config.DecoderConfig.ContentDecoder.GZIP;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentDecodingTest {
    private static final byte[] BODY = "{\"message\":\"hello hällo\"}".getBytes(UTF_8);

    private final ContentDecoding byDefault = new ContentDecoding(decoderConfig());

    // Accept-Encoding

    @Test void
    a_missing_decoder_config_accepts_gzip_and_deflate() {
        assertThat(new ContentDecoding(null).acceptEncoding(List.of())).isEqualTo("gzip,deflate");
    }

    @Test void
    accepts_the_decoders_in_the_configured_order() {
        assertThat(new ContentDecoding(decoderConfig().contentDecoders(DEFLATE, GZIP)).acceptEncoding(List.of())).isEqualTo("deflate,gzip");
    }

    @Test void
    accepts_a_decoder_configured_twice_once() {
        assertThat(new ContentDecoding(decoderConfig().contentDecoders(GZIP, GZIP)).acceptEncoding(List.of())).isEqualTo("gzip");
    }

    @Test void
    leaves_the_accept_encoding_as_it_is_without_decoders() {
        assertThat(new ContentDecoding(decoderConfig().noContentDecoders()).acceptEncoding(List.of("br"))).isNull();
    }

    @Test void
    a_decoder_contained_in_another_coding_of_the_users_accept_encoding_is_not_added() {
        // Today's behavior: the check is a substring match, so "x-gzip" counts as gzip
        assertThat(byDefault.acceptEncoding(List.of("x-gzip"))).isEqualTo("x-gzip,deflate");
    }

    @Test void
    the_check_for_a_decoder_in_the_users_accept_encoding_is_case_sensitive() {
        assertThat(byDefault.acceptEncoding(List.of("GZIP"))).isEqualTo("GZIP,gzip,deflate");
    }

    @Test void
    several_accept_encoding_headers_are_merged_into_the_first() {
        // Today's behavior: only the first header is replaced, and each decoder joins all values again
        assertThat(byDefault.acceptEncoding(List.of("br", "zstd"))).isEqualTo("br,zstd,gzip,zstd,deflate");
    }

    // Decoding responses

    @Test void
    decodes_only_the_configured_encodings_whatever_their_case_and_parameters() {
        assertThat(byDefault.decodes("GZIP")).isTrue();
        assertThat(byDefault.decodes(" deflate ;q=1")).isTrue();
        assertThat(byDefault.decodes("br, gzip")).isTrue();
        assertThat(byDefault.decodes("x-gzip")).isFalse();
        assertThat(byDefault.decodes("identity")).isFalse();
        assertThat(byDefault.decodes("")).isFalse();
        assertThat(byDefault.decodes(null)).isFalse();
        assertThat(new ContentDecoding(decoderConfig().contentDecoders(DEFLATE)).decodes("gzip")).isFalse();
    }

    @Test void
    a_body_without_a_configured_encoding_is_returned_as_it_is() throws IOException {
        InputStream body = new ByteArrayInputStream(BODY);

        assertThat(byDefault.decode(body, null)).isSameAs(body);
        assertThat(byDefault.decode(body, "br")).isSameAs(body);
    }

    @Test void
    decodes_gzip() throws IOException {
        assertThat(byDefault.decode(new ByteArrayInputStream(gzip(BODY)), "gzip").readAllBytes()).isEqualTo(BODY);
    }

    @Test void
    an_empty_gzip_body_is_empty() throws IOException {
        assertThat(byDefault.decode(new ByteArrayInputStream(new byte[0]), "gzip").readAllBytes()).isEmpty();
    }

    @Test void
    a_body_that_isnt_gzip_fails_when_it_is_decoded() {
        assertThatThrownBy(() -> byDefault.decode(new ByteArrayInputStream(BODY), "gzip")).isInstanceOf(ZipException.class);
    }

    @Test void
    decodes_zlib_wrapped_deflate() throws IOException {
        assertThat(byDefault.decode(new ByteArrayInputStream(zlibDeflate(BODY)), "deflate").readAllBytes()).isEqualTo(BODY);
    }

    @Test void
    decodes_raw_deflate_only_with_no_wrap() throws IOException {
        ContentDecoding noWrap = new ContentDecoding(decoderConfig().useNoWrapForInflateDecoding(true));

        assertThat(noWrap.decode(new ByteArrayInputStream(rawDeflate(BODY)), "deflate").readAllBytes()).isEqualTo(BODY);
    }

    @Test void
    a_body_that_isnt_deflate_fails_only_when_it_is_read() throws IOException {
        InputStream decoded = byDefault.decode(new ByteArrayInputStream(rawDeflate(BODY)), "deflate");

        assertThatThrownBy(decoded::readAllBytes).isInstanceOf(ZipException.class).hasMessage("incorrect header check");
    }

    @Test void
    stacked_encodings_are_decoded_in_the_order_of_the_configured_decoders() throws IOException {
        // Today's behavior: gzip, then deflate by default, whatever the order in Content-Encoding
        byte[] deflatedThenGzipped = gzip(zlibDeflate(BODY));

        assertThat(byDefault.decode(new ByteArrayInputStream(deflatedThenGzipped), "deflate, gzip").readAllBytes()).isEqualTo(BODY);
        assertThat(byDefault.decode(new ByteArrayInputStream(deflatedThenGzipped), "gzip, deflate").readAllBytes()).isEqualTo(BODY);
    }

    @Test void
    stacked_encodings_are_decoded_deflate_first_when_deflate_is_configured_first() throws IOException {
        ContentDecoding deflateFirst = new ContentDecoding(decoderConfig().contentDecoders(DEFLATE, GZIP));
        byte[] gzippedThenDeflated = zlibDeflate(gzip(BODY));

        assertThat(deflateFirst.decode(new ByteArrayInputStream(gzippedThenDeflated), "deflate, gzip").readAllBytes()).isEqualTo(BODY);
    }

    @Test void
    an_encoding_listed_twice_is_decoded_once() throws IOException {
        assertThat(byDefault.decode(new ByteArrayInputStream(gzip(BODY)), "gzip, gzip").readAllBytes()).isEqualTo(BODY);
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
