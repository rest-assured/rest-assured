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

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Maps the JSSE types of the SSL settings added in 6.3.0 ({@link SSLContext} and {@link HostnameVerifier}) onto the
 * HttpClient 4 types that the HTTP client still uses in 6.x.
 */
public class JsseToHttpClient4Ssl {

    private JsseToHttpClient4Ssl() {
    }

    /**
     * @return The given verifier if it's already an {@link X509HostnameVerifier}, otherwise an adapter for it.
     */
    public static X509HostnameVerifier toX509HostnameVerifier(HostnameVerifier hostnameVerifier) {
        if (hostnameVerifier == null || hostnameVerifier instanceof X509HostnameVerifier) {
            return (X509HostnameVerifier) hostnameVerifier;
        }
        return new HostnameVerifierAdapter(hostnameVerifier);
    }

    /**
     * @return The verifier that was given to {@link #toX509HostnameVerifier(HostnameVerifier)}.
     */
    public static HostnameVerifier toHostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        if (x509HostnameVerifier instanceof HostnameVerifierAdapter) {
            return ((HostnameVerifierAdapter) x509HostnameVerifier).hostnameVerifier;
        }
        return x509HostnameVerifier;
    }

    /**
     * @return The explicitly configured socket factory if there is one, otherwise a socket factory for the SSL context
     * (verifying host names with the given verifier), or {@code null} if neither is configured.
     */
    public static SSLSocketFactory toSSLSocketFactory(SSLSocketFactory sslSocketFactory, SSLContext sslContext, X509HostnameVerifier x509HostnameVerifier) {
        if (sslSocketFactory != null || sslContext == null) {
            return sslSocketFactory;
        }
        return new SSLSocketFactory(sslContext, x509HostnameVerifier == null ? SSLSocketFactory.STRICT_HOSTNAME_VERIFIER : x509HostnameVerifier);
    }

    private static class HostnameVerifierAdapter implements X509HostnameVerifier {
        private final HostnameVerifier hostnameVerifier;

        private HostnameVerifierAdapter(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
        }

        @Override
        public boolean verify(String host, SSLSession session) {
            return hostnameVerifier.verify(host, session);
        }

        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {
            if (!hostnameVerifier.verify(host, ssl.getSession())) {
                throw new SSLPeerUnverifiedException("Host name '" + host + "' was rejected by the configured HostnameVerifier " + hostnameVerifier);
            }
        }

        // HttpClient 4 verifies host names through verify(String, SSLSocket). A JSSE HostnameVerifier needs an SSL
        // session, so the two methods below, which don't have one, can't be supported.
        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {
            throw new SSLException("A " + HostnameVerifier.class.getName() + " can only verify a host name against an SSL session");
        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            throw new SSLException("A " + HostnameVerifier.class.getName() + " can only verify a host name against an SSL session");
        }
    }
}
