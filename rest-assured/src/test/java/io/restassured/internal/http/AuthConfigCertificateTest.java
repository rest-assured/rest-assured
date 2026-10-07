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

import io.restassured.config.EncoderConfig;
import io.restassured.config.OAuthConfig;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies how {@link AuthConfig#certificate} registers the https scheme depending on the scheme of the request (issue #790).
 */
class AuthConfigCertificateTest {

    @Test
    void registers_https_scheme_with_default_https_port_and_lazy_factory_for_http_request() {
        DefaultHttpClient client = new DefaultHttpClient();

        certificate(client, "http://127.0.0.1:9080/start", -1);

        Scheme https = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        assertThat(https.getDefaultPort()).isEqualTo(443);
        assertThat(https.getSchemeSocketFactory()).isInstanceOf(LazySSLSocketFactory.class);
    }

    @Test
    void registers_https_scheme_with_explicitly_configured_port_for_http_request() {
        DefaultHttpClient client = new DefaultHttpClient();

        certificate(client, "http://127.0.0.1:9080/start", 9443);

        assertThat(client.getConnectionManager().getSchemeRegistry().getScheme("https").getDefaultPort()).isEqualTo(9443);
    }

    @Test
    void registers_https_scheme_with_port_of_uri_and_eager_factory_for_https_request() {
        DefaultHttpClient client = new DefaultHttpClient();

        certificate(client, "https://127.0.0.1:8443/start", -1);

        Scheme https = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        assertThat(https.getDefaultPort()).isEqualTo(8443);
        assertThat(https.getSchemeSocketFactory()).isInstanceOf(SSLSocketFactory.class);
    }

    @Test
    void registers_https_scheme_with_default_https_port_for_https_request_without_port() {
        DefaultHttpClient client = new DefaultHttpClient();

        certificate(client, "https://127.0.0.1/start", -1);

        assertThat(client.getConnectionManager().getSchemeRegistry().getScheme("https").getDefaultPort()).isEqualTo(443);
    }

    private static void certificate(DefaultHttpClient client, String uri, int port) {
        HTTPBuilder builder = mock(HTTPBuilder.class);
        when(builder.getUri()).thenReturn(new URIBuilder(URI.create(uri), true, new EncoderConfig()));
        when(builder.getClient()).thenReturn(client);

        new AuthConfig(builder, new OAuthConfig()).certificate(null, null, KeyStore.getDefaultType(), null,
                null, null, KeyStore.getDefaultType(), null, port, null, null);
    }
}
