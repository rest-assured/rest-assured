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

package io.restassured.common.spi.http;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpSpiTest {

    private static final HostnameVerifier ACCEPT_ALL = (host, session) -> true;

    @Test
    void spi_types_use_only_jdk_types_so_that_any_client_module_can_implement_them() throws IOException {
        Path packageDirectory = Paths.get("src/main/java/io/restassured/common/spi/http");
        List<String> imports;
        try (Stream<Path> files = Files.list(packageDirectory)) {
            imports = files.flatMap(file -> {
                try {
                    return Files.readAllLines(file).stream().filter(line -> line.startsWith("import "));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        }

        assertThat(imports, not(empty()));
        assertThat(imports, everyItem(matchesPattern("import (static )?javax?\\..*")));
    }

    @Test
    void request_copies_the_headers_and_needs_method_uri_and_options() {
        List<HttpHeader> headers = new ArrayList<>(List.of(new HttpHeader("A", "1")));
        HttpRequest request = new HttpRequest("GET", URI.create("http://localhost/"), headers, null, RequestOptions.DEFAULT);
        headers.add(new HttpHeader("B", "2"));

        assertThat(request.headers(), contains(new HttpHeader("A", "1")));
        assertThrows(NullPointerException.class, () -> new HttpRequest(null, URI.create("http://localhost/"), List.of(), null, RequestOptions.DEFAULT));
        assertThrows(NullPointerException.class, () -> new HttpRequest("GET", URI.create("http://localhost/"), List.of(), null, null));
    }

    @Test
    void a_null_header_value_is_sent_empty() {
        assertThat(new HttpHeader("A", null).value(), is(""));
    }

    @Test
    void response_finds_the_first_header_ignoring_case_and_closes_through_its_hook() throws IOException {
        AtomicInteger closed = new AtomicInteger();
        HttpResponse response = new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", "a"), new HttpHeader("content-type", "b")),
                -1, null, closed::incrementAndGet);

        assertThat(response.firstHeader("CONTENT-TYPE"), is("a"));
        assertThat(response.firstHeader("Accept"), nullValue());
        response.close();
        response.close();
        assertThat(closed.get(), is(2));
    }

    @Test
    void customizers_are_equal_for_the_same_consumer_and_apply_it_to_a_builder_of_their_type() {
        List<StringBuilder> customized = new ArrayList<>();
        Consumer<StringBuilder> consumer = customized::add;
        ClientCustomizer customizer = new ClientCustomizer(StringBuilder.class, consumer);

        assertThat(customizer, is(new ClientCustomizer(StringBuilder.class, consumer)));
        assertThat(customizer.hashCode(), is(new ClientCustomizer(StringBuilder.class, consumer).hashCode()));
        assertThat(customizer, not(new ClientCustomizer(StringBuilder.class, customized::add)));
        StringBuilder builder = new StringBuilder();
        customizer.applyTo(builder);
        assertThat(customized, contains(builder));
        assertThrows(ClassCastException.class, () -> customizer.applyTo("not a StringBuilder"));
    }

    @Test
    void unsupported_feature_exception_names_the_feature_and_the_workaround() {
        UnsupportedFeatureException withWorkaround = new UnsupportedFeatureException("send a Host header", "start the JVM with -Dx=host");
        UnsupportedFeatureException withoutWorkaround = new UnsupportedFeatureException("authenticate with NTLM", null);

        assertThat(withWorkaround.getMessage(), is("The HTTP client can't send a Host header. To do it anyway, start the JVM with -Dx=host."));
        assertThat(withWorkaround.feature(), is("send a Host header"));
        assertThat(withWorkaround.workaround(), is("start the JVM with -Dx=host"));
        assertThat(withoutWorkaround.getMessage(), is("The HTTP client can't authenticate with NTLM."));
    }

    @Test
    void tls_settings_with_the_same_values_are_equal() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        SSLContext sslContext = SSLContext.getDefault();

        assertThat(tls().build(), is(tls().build()));
        assertThat(tls().build().hashCode(), is(tls().build().hashCode()));
        assertThat(tls().relaxedProtocol("TLS").build(), is(tls().relaxedProtocol("TLS").build()));
        assertThat(tls().keyStore(new KeyMaterial(new File("ks.p12"), "pw", "PKCS12", null)).build(),
                is(tls().keyStore(new KeyMaterial(new File("ks.p12"), "pw", "PKCS12", null)).build()));
        assertThat(tls().trustStore(new KeyMaterial(null, "pw", "JKS", keyStore)).build(), is(tls().trustStore(new KeyMaterial(null, "pw", "JKS", keyStore)).build()));
        assertThat(tls().sslContext(sslContext).build(), is(tls().sslContext(sslContext).build()));
        assertThat(tls().hostnameVerifier(ACCEPT_ALL).build(), is(tls().hostnameVerifier(ACCEPT_ALL).build()));
    }

    @Test
    void tls_settings_differ_in_every_value() throws Exception {
        TlsSettings defaults = tls().build();
        List<TlsSettings> others = List.of(
                tls().keyStore(new KeyMaterial("ks.p12", null, KeyStore.getDefaultType(), null)).build(),
                tls().keyStore(new KeyMaterial(null, "pw", KeyStore.getDefaultType(), null)).build(),
                tls().keyStore(new KeyMaterial(null, null, "other", null)).build(),
                tls().keyStore(new KeyMaterial(null, null, KeyStore.getDefaultType(), KeyStore.getInstance(KeyStore.getDefaultType()))).build(),
                tls().trustStore(new KeyMaterial("ts.p12", null, KeyStore.getDefaultType(), null)).build(),
                tls().sslContext(SSLContext.getDefault()).build(),
                tls().relaxedProtocol("SSL").build(),
                tls().hostnames(HostnameVerification.ALLOW_ALL).build(),
                tls().hostnameVerifier(ACCEPT_ALL).build());

        for (TlsSettings other : others) {
            assertThat(other.toString(), other, not(defaults));
        }
        // Another key store instance with the same content is another key store
        assertThat(tls().keyStore(new KeyMaterial(null, null, "PKCS12", KeyStore.getInstance("PKCS12"))).build(),
                not(tls().keyStore(new KeyMaterial(null, null, "PKCS12", KeyStore.getInstance("PKCS12"))).build()));
    }

    @Test
    void tls_settings_check_host_names_strictly_by_default_and_a_verifier_makes_the_check_custom() {
        TlsSettings defaults = tls().build();
        TlsSettings custom = tls().hostnameVerifier(ACCEPT_ALL).build();

        assertThat(defaults.hostnames(), is(HostnameVerification.STRICT));
        assertThat(defaults.hostnameVerifier(), nullValue());
        assertThat(custom.hostnames(), is(HostnameVerification.CUSTOM));
        assertThat(custom.hostnameVerifier(), is(ACCEPT_ALL));
        assertThat(tls().hostnameVerifier(ACCEPT_ALL).hostnames(HostnameVerification.STRICT).build(), is(defaults));
        assertThrows(IllegalArgumentException.class, () -> tls().hostnames(HostnameVerification.CUSTOM));
    }

    @Test
    void tls_settings_return_the_users_own_context() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        assertThat(tls().sslContext(sslContext).build().createSslContext() == sslContext, is(true));
    }

    @Test
    void tls_settings_reject_a_context_together_with_relaxed_validation() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> tls().sslContext(sslContext).relaxedProtocol("TLS").build());
        assertThat(e.getMessage(), is("An SSLContext and relaxed HTTPS validation can't be used together, since relaxed validation " +
                "creates an SSLContext of its own. Set only one of them."));
    }

    @Test
    void tls_settings_without_stores_create_a_tls_context() throws Exception {
        assertThat(tls().build().createSslContext().getProtocol(), is("TLS"));
        assertThat(tls().relaxedProtocol("TLSv1.2").build().createSslContext().getProtocol(), is("TLSv1.2"));
    }

    @Test
    void key_material_without_path_or_store_loads_nothing_but_checks_the_type() throws Exception {
        assertThat(new KeyMaterial(null, null, KeyStore.getDefaultType(), null).load(), nullValue());
        assertThat(new KeyMaterial("", null, KeyStore.getDefaultType(), null).load(), nullValue());
        assertThrows(java.security.KeyStoreException.class, () -> new KeyMaterial(null, null, "not-a-type", null).load());
    }

    @Test
    void key_material_without_type_uses_the_default_type() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        assertThat(new KeyMaterial(null, null, null, null).load(), nullValue());
        assertThat(new KeyMaterial(null, null, null, keyStore).load() == keyStore, is(true));
        // A missing file is only opened after the store of the default type was created
        assertThrows(java.io.FileNotFoundException.class, () -> new KeyMaterial(new File("does-not-exist.p12"), null, null, null).load());
    }

    @Test
    void key_material_path_must_be_a_string_or_a_file() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new KeyMaterial(Paths.get("ks.p12"), null, KeyStore.getDefaultType(), null));
        assertThat(e.getMessage(), is("The path to a key store or trust store must be a String or a java.io.File but was " + Paths.get("ks.p12").getClass().getName() + "."));
    }

    private static TlsSettings.Builder tls() {
        return TlsSettings.builder();
    }
}
