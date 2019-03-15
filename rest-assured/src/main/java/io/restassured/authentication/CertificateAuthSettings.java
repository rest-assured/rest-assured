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
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

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
     * </ul>
     *
     * @see #certAuthSettings()
     */
    public CertificateAuthSettings() {
        this(KeyStore.getDefaultType(), KeyStore.getDefaultType(), UNDEFINED_PORT, null, null, STRICT_HOSTNAME_VERIFIER, null);
    }

    /**
     * @param keystoreType         The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param trustStoreType       The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param port                 The port, by default 443.
     * @param trustStore           The trust store, by default no provider is used (<code>null</code>).
     * @param x509HostnameVerifier The X509HostnameVerifier to use
     * @param sslSocketFactory     The SSLSocketFactory to use
     */
    private CertificateAuthSettings(String keystoreType, String trustStoreType, int port, KeyStore trustStore, KeyStore keyStore, X509HostnameVerifier x509HostnameVerifier,
                                    SSLSocketFactory sslSocketFactory) {
        AssertParameter.notNull(keystoreType, "Certificate type");
        AssertParameter.notNull(trustStoreType, "Trust store type");
        this.keystoreType = keystoreType;
        this.keyStore = keyStore;
        this.trustStoreType = trustStoreType;
        this.port = port;
        this.trustStore = trustStore;
        this.x509HostnameVerifier = x509HostnameVerifier;
        this.sslSocketFactory = sslSocketFactory;
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

    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Configure the CertificateAuthSettings to use strict host name verification (this is the default behavior).
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings strictHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, STRICT_HOSTNAME_VERIFIER, sslSocketFactory);
    }

    /**
     * Configure the CertificateAuthSettings to allow all host names.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings allowAllHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, ALLOW_ALL_HOSTNAME_VERIFIER, sslSocketFactory);
    }

    /**
     * Configure the CertificateAuthSettings to use the provided {@link X509HostnameVerifier} instance.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     * @see #allowAllHostnames()
     */
    public CertificateAuthSettings x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @return The configured X509HostnameVerifier
     */
    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    /**
     * @param keystoreType The keystore type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStoreType(String keystoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @param trustStoreType The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStoreType(String trustStoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @param port The port, by default 443.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings port(int port) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @param trustStore The trust store to use, by default no (<code>null</code>) trust store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStore(KeyStore trustStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @param keyStore The key store to use, by default no (<code>null</code>) key store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStore(KeyStore keyStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
    }

    /**
     * @param sslSocketFactory The SSLSocketFactory to use
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, port, trustStore, keyStore, x509HostnameVerifier, sslSocketFactory);
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