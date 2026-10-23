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

package io.restassured.internal;

import io.restassured.common.spi.http.TlsSettings;
import io.restassured.internal.http.HTTPBuilder;
import io.restassured.internal.http.JsseToHttpClient4Ssl;
import io.restassured.internal.http.LazySSLSocketFactory;
import io.restassured.internal.http.client.ClientSettingsFactory;
import io.restassured.internal.util.SafeExceptionRethrower;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class TrustAndKeystoreSpecImpl implements TrustAndKeystoreSpec {

    private Object keyStorePath;
    private String keyStorePassword;
    private String keyStoreType;
    private KeyStore keyStore;

    private Object trustStorePath;
    private String trustStorePassword;
    private String trustStoreType;
    private KeyStore trustStore;

    private int port;
    private SSLSocketFactory factory;
    private X509HostnameVerifier x509HostnameVerifier;
    private TlsSettings tlsSettings;

    @Override
    public void apply(HTTPBuilder builder, int port) {
        apply(builder, port, false);
    }

    /**
     * Register the https scheme on the builder's client.
     *
     * @param builder The http builder
     * @param port The default port of the https scheme (used when an https URI doesn't specify a port)
     * @param lazy <code>true</code> to defer creating the SSL socket factory (and loading key/trust stores) until an https
     *             connection is actually opened, for example when a plain http request is redirected to https.
     */
    public void apply(HTTPBuilder builder, int port, boolean lazy) {
        int portToUse = this.port == -1 ? port : this.port;
        // A checked exception from loading a key or trust store passes through the supplier unwrapped, so that
        // LazySSLSocketFactory reports its message
        SchemeSocketFactory schemeSocketFactory = lazy ? new LazySSLSocketFactory(this::getOrCreateSSLSocketFactory) : getOrCreateSSLSocketFactory();
        builder.getClient().getConnectionManager().getSchemeRegistry().register(new Scheme("https", portToUse, schemeSocketFactory));
    }

    private SSLSocketFactory getOrCreateSSLSocketFactory() {
        if (factory == null) {
            factory = JsseToHttpClient4Ssl.toSSLSocketFactory(tlsSettings != null ? tlsSettings : ClientSettingsFactory.certificateTlsSettings(keyStorePath,
                    keyStorePassword, keyStoreType, keyStore, trustStorePath, trustStorePassword, trustStoreType, trustStore,
                    JsseToHttpClient4Ssl.toHostnameVerifier(x509HostnameVerifier), null));
        }
        return factory;
    }

    /**
     * Load a key store.
     *
     * @param keyStoreType     The key store type (a String)
     * @param keyStorePath     A {@link File}, or the path to the key store in classpath or in the file system (a String)
     * @param keyStorePassword The key store password (a String), or <code>null</code>
     * @return The key store, or <code>null</code> if <code>keyStorePath</code> is <code>null</code> or empty
     */
    public KeyStore createStore(Object keyStoreType, Object keyStorePath, Object keyStorePassword) {
        try {
            return ClientSettingsFactory.keyMaterial(keyStorePath, keyStorePassword == null ? null : keyStorePassword.toString(),
                    keyStoreType == null ? null : keyStoreType.toString(), null).load();
        } catch (IOException | GeneralSecurityException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    /**
     * Use these TLS settings instead of the key store, trust store and host name verifier of this spec.
     */
    public void setTlsSettings(TlsSettings tlsSettings) {
        this.tlsSettings = tlsSettings;
    }

    public Object getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(Object keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public Object getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(Object trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SSLSocketFactory getFactory() {
        return factory;
    }

    public void setFactory(SSLSocketFactory factory) {
        this.factory = factory;
    }

    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    public void setX509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        this.x509HostnameVerifier = x509HostnameVerifier;
    }
}
