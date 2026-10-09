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

import io.restassured.internal.http.HTTPBuilder;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import java.security.KeyStore;

public class CertAuthScheme implements AuthenticationScheme {

    private Object pathToKeyStore;
    private String keyStorePassword;
    private String keystoreType = KeyStore.getDefaultType();
    private Object pathToTrustStore;
    private String trustStorePassword;
    private String trustStoreType = KeyStore.getDefaultType();
    private int port = -1;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private X509HostnameVerifier x509HostnameVerifier;
    private SSLSocketFactory sslSocketFactory;

    @Override
    public void authenticate(HTTPBuilder httpBuilder) {
        httpBuilder.getAuth().certificate(pathToKeyStore, keyStorePassword, keystoreType, keyStore,
                pathToTrustStore, trustStorePassword, trustStoreType, trustStore,
                port, x509HostnameVerifier, sslSocketFactory);
    }

    public Object getPathToKeyStore() {
        return pathToKeyStore;
    }

    public void setPathToKeyStore(Object pathToKeyStore) {
        this.pathToKeyStore = pathToKeyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public Object getPathToTrustStore() {
        return pathToTrustStore;
    }

    public void setPathToTrustStore(Object pathToTrustStore) {
        this.pathToTrustStore = pathToTrustStore;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    public void setX509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        this.x509HostnameVerifier = x509HostnameVerifier;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }
}
