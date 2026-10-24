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

package io.restassured.internal.http.auth;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.OAuthSignature;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.config.OAuthConfig;
import io.restassured.internal.http.Origin;
import io.restassured.internal.http.client.ClientSettingsFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map;

/**
 * What an {@link AuthenticationScheme} asked for, whichever HTTP client sends the request: headers, the credentials
 * that answer a challenge, NTLM credentials, an OAuth signature and the TLS settings of certificate authentication.
 * REST Assured applies all of it itself, except the TLS settings and the NTLM credentials, which the HTTP client
 * needs.
 */
public final class CoreAuthenticationContext implements CertificateAuthenticationContext {
    private static final int PORT_OF_THE_REQUEST = -1;

    private final Map<Object, Object> headers;
    private final URI uri;
    private final OAuthConfig oauthConfig;

    private Credentials credentials;
    private NtlmCredentials ntlm;
    private CoreOAuthSigner oauthSigner;
    private TlsSettings certificate;
    private int certificatePort = PORT_OF_THE_REQUEST;

    /**
     * @param headers     the headers of the request, which {@link #header(String, String)} adds to
     * @param uri         the URI of the request, which the credentials are for
     * @param oauthConfig the OAuth config of the request
     */
    public CoreAuthenticationContext(Map<Object, Object> headers, URI uri, OAuthConfig oauthConfig) {
        this.headers = headers;
        this.uri = uri;
        this.oauthConfig = oauthConfig == null ? new OAuthConfig() : oauthConfig;
    }

    @Override
    public void header(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void basic(String userName, String password) {
        credentials = Credentials.basic(userName, password, uri);
    }

    @Override
    public void digest(String userName, String password) {
        credentials = Credentials.digest(userName, password, uri);
    }

    @Override
    public void ntlm(String userName, String password, String workstation, String domain) {
        ntlm = new NtlmCredentials(Origin.host(uri), Origin.port(uri), userName, password, workstation, domain);
    }

    @Override
    public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        oauth(consumerKey, consumerSecret, accessToken, secretToken, OAuthSignature.HEADER);
    }

    @Override
    public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature) {
        oauthSigner = consumerKey == null ? null : CoreOAuthSigner.oauth(consumerKey, consumerSecret, accessToken, secretToken,
                signature, oauthConfig.shouldAddEmptyAccessOAuthTokenToBaseString());
    }

    @Override
    public void oauth2(String accessToken) {
        oauth2(accessToken, OAuthSignature.HEADER);
    }

    @Override
    public void oauth2(String accessToken, OAuthSignature signature) {
        oauthSigner = accessToken == null ? null : CoreOAuthSigner.oauth2(accessToken, signature);
    }

    @Override
    public void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                            Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                            int port, HostnameVerifier hostnameVerifier, SSLContext sslContext) {
        certificate(ClientSettingsFactory.certificateTlsSettings(keyStorePath, keyStorePassword, keyStoreType, keyStore,
                trustStorePath, trustStorePassword, trustStoreType, trustStore, hostnameVerifier, sslContext), port);
    }

    @Override
    public void certificate(TlsSettings tls, int port) {
        certificate = tls;
        certificatePort = port;
    }

    /**
     * @return the credentials that answer a challenge, or {@code null} if the request has none
     */
    public Credentials credentials() {
        return credentials;
    }

    /**
     * @return the NTLM credentials of the request, or {@code null}
     */
    public NtlmCredentials ntlm() {
        return ntlm;
    }

    /**
     * @return what signs the requests, or {@code null} if they aren't signed
     */
    public CoreOAuthSigner oauthSigner() {
        return oauthSigner;
    }

    /**
     * @return the TLS settings of certificate authentication, which replace those of the SSL config, or {@code null}
     */
    public TlsSettings certificate() {
        return certificate;
    }

    /**
     * @return the port that certificate authentication is for, or {@code -1} for the port of the request
     */
    public int certificatePort() {
        return certificatePort;
    }
}
