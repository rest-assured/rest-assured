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

package io.restassured.authentication;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * What an {@link AuthenticationScheme} can do to a request, without depending on the HTTP client that sends it.
 * REST Assured passes one to {@link AuthenticationScheme#authenticate(AuthenticationContext)} for each request.
 *
 * @see AuthenticationScheme
 */
public interface AuthenticationContext {

    /**
     * Set a header on the request, for example an <code>Authorization</code> header that's sent without waiting for a
     * challenge from the server. It replaces a header with the same name.
     *
     * @param name  The header name
     * @param value The header value
     */
    void header(String name, String value);

    /**
     * Answer a challenge from the server with Basic or Digest authentication. The credentials are only sent to the host
     * and port of the request.
     *
     * @param userName The user name
     * @param password The password
     */
    void basic(String userName, String password);

    /**
     * Answer a challenge from the server with Digest authentication only. A server that only asks for Basic
     * authentication doesn't get the credentials, so the password is never sent in clear text. The credentials are only
     * sent to the host and port of the request.
     *
     * @param userName The user name
     * @param password The password
     */
    void digest(String userName, String password);

    /**
     * Answer a challenge from the server with NTLM authentication. The credentials are only sent to the host and port of
     * the request.
     *
     * @param userName    The user name
     * @param password    The password
     * @param workstation The NTLM workstation
     * @param domain      The NTLM domain
     */
    void ntlm(String userName, String password, String workstation, String domain);

    /**
     * Sign the request with OAuth 1, with the signature in the <code>Authorization</code> header. It replaces earlier
     * OAuth or OAuth 2 signing, and a <code>null</code> consumer key turns signing off.
     *
     * @param consumerKey    The consumer key
     * @param consumerSecret The consumer secret
     * @param accessToken    The access token
     * @param secretToken    The secret token
     */
    void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken);

    /**
     * Sign the request with OAuth 1. It replaces earlier OAuth or OAuth 2 signing, and a <code>null</code> consumer key
     * turns signing off.
     *
     * @param consumerKey    The consumer key
     * @param consumerSecret The consumer secret
     * @param accessToken    The access token
     * @param secretToken    The secret token
     * @param signature      Where the signature goes, in a header or in the query string
     */
    void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature);

    /**
     * Send an OAuth 2 access token in the <code>Authorization</code> header. It replaces earlier OAuth or OAuth 2
     * signing, and a <code>null</code> access token turns it off.
     *
     * @param accessToken The access token
     */
    void oauth2(String accessToken);

    /**
     * Send an OAuth 2 access token. It replaces earlier OAuth or OAuth 2 signing, and a <code>null</code> access token
     * turns it off.
     *
     * @param accessToken The access token
     * @param signature   Where the token goes, in a header or in the query string
     */
    void oauth2(String accessToken, OAuthSignature signature);

    /**
     * Use a client certificate and trust store for the request. Each key store and trust store is given either as a
     * path (a file or class path resource) with its password and type, or as a loaded {@link KeyStore}. When the
     * request isn't HTTPS, the certificate is used for HTTPS connections the request is redirected to, on the given
     * port or 443.
     *
     * @param keyStorePath       The path to the key store, or <code>null</code>
     * @param keyStorePassword   The key store password
     * @param keyStoreType       The key store type
     * @param keyStore           A loaded key store, or <code>null</code>
     * @param trustStorePath     The path to the trust store, or <code>null</code>
     * @param trustStorePassword The trust store password
     * @param trustStoreType     The trust store type
     * @param trustStore         A loaded trust store, or <code>null</code>
     * @param port               The HTTPS port, or <code>-1</code> to use the port of the request
     * @param hostnameVerifier   How host names are verified. With <code>null</code>, any host name is allowed for
     *                           the key store and trust store, and host names are verified strictly for an SSLContext
     * @param sslContext         An SSLContext to use instead of the key store and trust store, or <code>null</code>
     */
    void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                     Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                     int port, HostnameVerifier hostnameVerifier, SSLContext sslContext);
}
