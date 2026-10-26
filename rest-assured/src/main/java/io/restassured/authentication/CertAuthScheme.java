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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

public class CertAuthScheme implements AuthenticationScheme {

    private Object pathToKeyStore;
    private String keyStorePassword;
    private String keystoreType = KeyStore.getDefaultType();
    private Object pathToTrustStore;
    private String trustStorePassword;
    private String trustStoreType = KeyStore.getDefaultType();
    private KeyStore trustStore;
    private KeyStore keyStore;
    private HostnameVerifier hostnameVerifier;
    private SSLContext sslContext;

    @Override
    public void authenticate(AuthenticationContext context) {
        context.certificate(pathToKeyStore, keyStorePassword, keystoreType, keyStore,
                pathToTrustStore, trustStorePassword, trustStoreType, trustStore,
                hostnameVerifier, sslContext);
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

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Use an {@link SSLContext} that you have set up yourself. Host names are verified strictly unless another verifier
     * is set with {@link #setHostnameVerifier(HostnameVerifier)}.
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }
}
