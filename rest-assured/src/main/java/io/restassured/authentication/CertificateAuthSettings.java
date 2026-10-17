/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.authentication;

import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.http.JsseToHttpClient4Ssl;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;

/**
 * A specification for more advanced usages of certificate authentication. Example usage:
 * <pre>
 * given().auth().certificate("keystore.jks", "my_password", certAuthSettings().allowAllHostNames());
 * </pre>
 */
public class CertificateAuthSettings {
    private static final int UNDEFINED_PORT = -1;
    private final String keystoreType;
    private final String trustStoreType;
    private final int port;
    private final KeyStore trustStore;
    private final KeyStore keyStore;
    private final X509HostnameVerifier x509HostnameVerifier;
    private final SSLSocketFactory sslSocketFactory;
    private final SSLContext sslContext;

    /**
     * Create a new instance of the Certificate Authentication Options with the default settings of:
     * <ul>
     * <li>keystoreType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>trustStoreType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>port = 443</li>
     * <li>trustStore = null</li>
     * <li>keyStore = null</li>
     * <li>x509HostnameVerifier = {@link org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER}</li>
     * <li>SSLSocketFactory = null</li>
     * <li>SSLContext = null</li>
     * </ul>
     *
     * @see #certAuthSettings()
     */
    public CertificateAuthSettings() {
        this(KeyStore.getDefaultType(), KeyStore.getDefaultType(), UNDEFINED_PORT, null, null, STRICT_HOSTNAME_VERIFIER, null, null);
    }

    /**
     * @param keystoreType         The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param trustStoreType       The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param port                 The port, by default 443.
     * @param trustStore           The trust store, by default no provider is used (<code>null</code>).
     * @param x509HostnameVerifier The X509HostnameVerifier to use
     * @param sslSocketFactory     The SSLSocketFactory to use
     * @param sslContext           The SSLContext to use
     */
    private CertificateAuthSettings(String keystoreType, String trustStoreType, int port, KeyStore trustStore, KeyStore keyStore, X509HostnameVerifier x509HostnameVerifier,
                                    SSLSocketFactory sslSocketFactory, SSLContext sslContext) {
        AssertParameter.notNull(keystoreType, "Certificate type");
        AssertParameter.notNull(trustStoreType, "Trust store type");
        this.keystoreType = keystoreType;
        this.keyStore = keyStore;
        this.trustStoreType = trustStoreType;
        this.port = port;
        this.trustStore = trustStore;
        this.x509HostnameVerifier = x509HostnameVerifier;
        this.sslSocketFactory = sslSocketFactory;
        this.sslContext = sslContext;
    }

    public String getKeyStoreType() {
        return keystoreType;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public int getPort() {
        return port;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * @return The configured SSLSocketFactory, or one created from the {@link SSLContext} set by
     * {@link #sslContext(SSLContext)} and the configured host name verification.
     * @deprecated Use {@link #getSSLContext()} instead. HttpClient 4 types are removed from the API in REST Assured 7.0.0.
     */
    @Deprecated
    public SSLSocketFactory getSSLSocketFactory() {
        return JsseToHttpClient4Ssl.toSSLSocketFactory(sslSocketFactory, sslContext, x509HostnameVerifier);
    }

    /**
     * @return The SSL context set by {@link #sslContext(SSLContext)}, or {@code null} if none is set.
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }

    /**
     * Configure the CertificateAuthSettings to use strict host name verification (this is the default behavior).
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings strictHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, STRICT_HOSTNAME_VERIFIER, sslSocketFactory, sslContext);
    }

    /**
     * Configure the CertificateAuthSettings to allow all host names.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings allowAllHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, ALLOW_ALL_HOSTNAME_VERIFIER, sslSocketFactory, sslContext);
    }

    /**
     * Configure the CertificateAuthSettings to use the provided {@link HostnameVerifier}, which decides whether the
     * host name of the server matches its certificate. It isn't used when an SSLSocketFactory is set with
     * sslSocketFactory(..), since that factory verifies host names itself.
     *
     * @param hostnameVerifier The HostnameVerifier to use
     * @return A new CertificateAuthSettings instance
     * @see #allowAllHostnames()
     * @see #strictHostnames()
     */
    public CertificateAuthSettings hostnameVerifier(HostnameVerifier hostnameVerifier) {
        AssertParameter.notNull(hostnameVerifier, HostnameVerifier.class);
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, JsseToHttpClient4Ssl.toX509HostnameVerifier(hostnameVerifier), sslSocketFactory, sslContext);
    }

    /**
     * Configure the CertificateAuthSettings to use the provided {@link X509HostnameVerifier} instance.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     * @see #allowAllHostnames()
     * @deprecated Use {@link #hostnameVerifier(HostnameVerifier)} instead. HttpClient 4 types are removed from the API
     * in REST Assured 7.0.0.
     */
    @Deprecated
    public CertificateAuthSettings x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @return The configured X509HostnameVerifier
     * @deprecated Use {@link #getHostnameVerifier()} instead. HttpClient 4 types are removed from the API in REST
     * Assured 7.0.0.
     */
    @Deprecated
    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    /**
     * @return The host name verifier, either the one set by {@link #hostnameVerifier(HostnameVerifier)} or the one
     * chosen by {@link #strictHostnames()} (the default) or {@link #allowAllHostnames()}.
     */
    public HostnameVerifier getHostnameVerifier() {
        return JsseToHttpClient4Ssl.toHostnameVerifier(x509HostnameVerifier);
    }

    /**
     * @param keystoreType The keystore type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStoreType(String keystoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @param trustStoreType The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStoreType(String trustStoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @param port The port, by default 443.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings port(int port) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @param trustStore The trust store to use, by default no (<code>null</code>) trust store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStore(KeyStore trustStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @param keyStore The key store to use, by default no (<code>null</code>) key store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStore(KeyStore keyStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, sslContext);
    }

    /**
     * @param sslSocketFactory The SSLSocketFactory to use. It replaces an {@link SSLContext} set by {@link #sslContext(SSLContext)}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     * @deprecated Use {@link #sslContext(SSLContext)} instead, and {@link #hostnameVerifier(HostnameVerifier)} for the
     * host name verification. HttpClient 4 types are removed from the API in REST Assured 7.0.0.
     */
    @Deprecated
    public CertificateAuthSettings sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory, null);
    }

    /**
     * Use an {@link SSLContext} that you have set up yourself. This overrides the key store and trust store. Host names
     * are still verified as configured by {@link #hostnameVerifier(HostnameVerifier)}, {@link #allowAllHostnames()} or
     * {@link #strictHostnames()} (the default).
     *
     * @param sslContext The SSLContext to use
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings sslContext(SSLContext sslContext) {
        AssertParameter.notNull(sslContext, SSLContext.class);
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, null, sslContext);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings with() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings using() {
        return this;
    }

    /**
     * Create a new instance of {@link CertificateAuthSettings} with default values. Same as called {@link CertificateAuthSettings#CertificateAuthSettings()} but a bit more "fluent".
     *
     * @return a new instance of {@link CertificateAuthSettings} with default values.
     */
    public static CertificateAuthSettings certAuthSettings() {
        return new CertificateAuthSettings();
    }
}