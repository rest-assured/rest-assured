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
import io.restassured.internal.http.HostnameVerifiers;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * A specification for more advanced usages of certificate authentication. Example usage:
 * <pre>
 * given().auth().certificate("keystore.jks", "my_password", certAuthSettings().allowAllHostNames());
 * </pre>
 */
public class CertificateAuthSettings {
    private final String keystoreType;
    private final String trustStoreType;
    private final KeyStore trustStore;
    private final KeyStore keyStore;
    private final HostnameVerifier hostnameVerifier;
    private final SSLContext sslContext;

    /**
     * Create a new instance of the Certificate Authentication Options with the default settings of:
     * <ul>
     * <li>keystoreType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>trustStoreType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>trustStore = null</li>
     * <li>keyStore = null</li>
     * <li>strict host name verification (see {@link #strictHostnames()})</li>
     * <li>SSLContext = null</li>
     * </ul>
     *
     * @see #certAuthSettings()
     */
    public CertificateAuthSettings() {
        this(KeyStore.getDefaultType(), KeyStore.getDefaultType(), null, null, HostnameVerifiers.STRICT, null);
    }

    /**
     * @param keystoreType         The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param trustStoreType       The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param trustStore           The trust store, by default no provider is used (<code>null</code>).
     * @param hostnameVerifier     The HostnameVerifier to use
     * @param sslContext           The SSLContext to use
     */
    private CertificateAuthSettings(String keystoreType, String trustStoreType, KeyStore trustStore, KeyStore keyStore, HostnameVerifier hostnameVerifier,
                                    SSLContext sslContext) {
        AssertParameter.notNull(keystoreType, "Certificate type");
        AssertParameter.notNull(trustStoreType, "Trust store type");
        this.keystoreType = keystoreType;
        this.keyStore = keyStore;
        this.trustStoreType = trustStoreType;
        this.trustStore = trustStore;
        this.hostnameVerifier = hostnameVerifier;
        this.sslContext = sslContext;
    }

    public String getKeyStoreType() {
        return keystoreType;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
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
     */
    public CertificateAuthSettings strictHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, HostnameVerifiers.STRICT, sslContext);
    }

    /**
     * Configure the CertificateAuthSettings to allow all host names.
     *
     * @return A new CertificateAuthSettings instance
     */
    public CertificateAuthSettings allowAllHostnames() {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, HostnameVerifiers.ALLOW_ALL, sslContext);
    }

    /**
     * Configure the CertificateAuthSettings to use the provided {@link HostnameVerifier}, which decides whether the
     * host name of the server matches its certificate.
     *
     * @param hostnameVerifier The HostnameVerifier to use
     * @return A new CertificateAuthSettings instance
     * @see #allowAllHostnames()
     * @see #strictHostnames()
     */
    public CertificateAuthSettings hostnameVerifier(HostnameVerifier hostnameVerifier) {
        AssertParameter.notNull(hostnameVerifier, HostnameVerifier.class);
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
    }

    /**
     * @return The host name verifier, either the one set by {@link #hostnameVerifier(HostnameVerifier)} or the one
     * chosen by {@link #strictHostnames()} (the default) or {@link #allowAllHostnames()}.
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * @param keystoreType The keystore type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStoreType(String keystoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
    }

    /**
     * @param trustStoreType The trust store type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStoreType(String trustStoreType) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
    }

    /**
     * @param trustStore The trust store to use, by default no (<code>null</code>) trust store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStore(KeyStore trustStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
    }

    /**
     * @param keyStore The key store to use, by default no (<code>null</code>) key store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStore(KeyStore keyStore) {
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
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
        return new CertificateAuthSettings(keystoreType, trustStoreType, trustStore, keyStore, hostnameVerifier, sslContext);
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