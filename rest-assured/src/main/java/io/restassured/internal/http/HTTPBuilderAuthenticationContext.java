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

import io.restassured.authentication.AuthenticationContext;
import io.restassured.authentication.OAuthSignature;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.Map;

/**
 * The {@link AuthenticationContext} of REST Assured 6.x, which applies everything to an {@link HTTPBuilder}.
 */
public class HTTPBuilderAuthenticationContext implements AuthenticationContext {
    private final HTTPBuilder httpBuilder;

    public HTTPBuilderAuthenticationContext(HTTPBuilder httpBuilder) {
        this.httpBuilder = httpBuilder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void header(String name, String value) {
        ((Map<Object, Object>) httpBuilder.getHeaders()).put(name, value);
    }

    @Override
    public void basic(String userName, String password) {
        httpBuilder.getAuth().basic(userName, password);
    }

    @Override
    public void digest(String userName, String password) {
        httpBuilder.getAuth().digest(userName, password);
    }

    @Override
    public void ntlm(String userName, String password, String workstation, String domain) {
        httpBuilder.getAuth().ntlm(userName, password, workstation, domain);
    }

    @Override
    public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        httpBuilder.getAuth().oauth(consumerKey, consumerSecret, accessToken, secretToken);
    }

    @Override
    public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature) {
        httpBuilder.getAuth().oauth(consumerKey, consumerSecret, accessToken, secretToken, signature);
    }

    @Override
    public void oauth2(String accessToken) {
        httpBuilder.getAuth().oauth2(accessToken);
    }

    @Override
    public void oauth2(String accessToken, OAuthSignature signature) {
        httpBuilder.getAuth().oauth2(accessToken, signature);
    }

    @Override
    public void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                            Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                            int port, HostnameVerifier hostnameVerifier, SSLContext sslContext) {
        X509HostnameVerifier x509HostnameVerifier = JsseToHttpClient4Ssl.toX509HostnameVerifier(hostnameVerifier);
        httpBuilder.getAuth().certificate(keyStorePath, keyStorePassword, keyStoreType, keyStore,
                trustStorePath, trustStorePassword, trustStoreType, trustStore,
                port, x509HostnameVerifier, JsseToHttpClient4Ssl.toSSLSocketFactory(null, sslContext, x509HostnameVerifier));
    }
}
