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

package io.restassured.http.apache5;

import io.restassured.common.spi.http.TlsSettings;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The socket factory for HTTPS. The API of HttpClient 5.2 is used, which later versions deprecate but keep, so that one
 * jar works with every supported version.
 */
@SuppressWarnings("deprecation")
final class Tls {

    private Tls() {
    }

    /**
     * @param tls the TLS settings, or {@code null} for a new default context, which ignores the {@code javax.net.ssl.keyStore}
     *            property, as REST Assured 6.x and the JDK client do, and checks host names strictly
     * @throws IOException              if a key or trust store can't be read
     * @throws GeneralSecurityException if a key or trust store can't be loaded
     */
    static LayeredConnectionSocketFactory socketFactory(TlsSettings tls) throws IOException, GeneralSecurityException {
        if (tls == null) {
            return defaultSocketFactory();
        }
        SSLConnectionSocketFactoryBuilder builder = SSLConnectionSocketFactoryBuilder.create().setSslContext(tls.createSslContext());
        HostnameVerifier verifier = hostnameVerifier(tls);
        // Without one, HttpClient's DefaultHostnameVerifier, where a wildcard matches one label only
        if (verifier != null) {
            builder.setHostnameVerifier(verifier);
        }
        return builder.build();
    }

    /**
     * @return the socket factory without TLS settings
     */
    static LayeredConnectionSocketFactory defaultSocketFactory() {
        return SSLConnectionSocketFactoryBuilder.create().setSslContext(SSLContexts.createDefault()).build();
    }

    private static HostnameVerifier hostnameVerifier(TlsSettings tls) {
        // Relaxed validation trusts every certificate for every host
        if (tls.relaxedProtocol() != null) {
            return NoopHostnameVerifier.INSTANCE;
        }
        switch (tls.hostnames()) {
            case ALLOW_ALL:
                return NoopHostnameVerifier.INSTANCE;
            case CUSTOM:
                return tls.hostnameVerifier();
            default:
                return null;
        }
    }
}
