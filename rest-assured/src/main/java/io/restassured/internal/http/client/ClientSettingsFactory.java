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

package io.restassured.internal.http.client;

import io.restassured.authentication.CertAuthScheme;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.KeyMaterial;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.SSLConfig;
import io.restassured.internal.http.HostnameVerifiers;
import io.restassured.internal.util.GroovyTypes;
import io.restassured.specification.ProxySpecification;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * Translates REST Assured's configuration into the settings of the HTTP client SPI.
 */
public final class ClientSettingsFactory {

    private ClientSettingsFactory() {
    }

    /**
     * @param sslConfig        the SSL config of the request
     * @param certificate      the TLS settings of certificate authentication, which replace those of the SSL config, or {@code null}
     * @param proxy            the proxy of the request, or {@code null}
     * @param httpClientConfig the HTTP client config of the request
     * @return the settings of the client that sends the request. Without certificate authentication and an SSL config
     * that the user set, the client uses the TLS defaults of its library.
     */
    public static ClientSettings clientSettings(SSLConfig sslConfig, TlsSettings certificate, ProxySpecification proxy, HttpClientConfig httpClientConfig) {
        TlsSettings tls = certificate != null ? certificate : sslConfig.isUserConfigured() ? tlsSettings(sslConfig) : null;
        return new ClientSettings(tls, proxySettings(proxy), HttpVersion.HTTP_1_1, httpClientConfig.connectTimeout(),
                httpClientConfig.connectionRequestTimeout(), httpClientConfig.customizers());
    }

    /**
     * @param ntlm the NTLM credentials of the request, or {@code null}
     */
    public static RequestOptions requestOptions(HttpClientConfig httpClientConfig, NtlmCredentials ntlm) {
        return new RequestOptions(httpClientConfig.readTimeout(), httpClientConfig.expectsContinue(), ntlm);
    }

    /**
     * @return what the SSL config asks for
     */
    public static TlsSettings tlsSettings(SSLConfig sslConfig) {
        TlsSettings.Builder tls = TlsSettings.builder()
                .keyStore(keyMaterial(sslConfig.getPathToKeyStore(), sslConfig.getKeyStorePassword(), sslConfig.getKeyStoreType(), sslConfig.getKeyStore()))
                .trustStore(keyMaterial(sslConfig.getPathToTrustStore(), sslConfig.getTrustStorePassword(), sslConfig.getTrustStoreType(), sslConfig.getTrustStore()))
                .sslContext(sslConfig.getSSLContext())
                .relaxedProtocol(sslConfig.getRelaxedHTTPSValidationProtocol());
        return HostnameVerifiers.hostnames(tls, sslConfig.getHostnameVerifier()).build();
    }

    /**
     * @return what certificate authentication asks for, with the parameters of
     * {@link io.restassured.authentication.AuthenticationContext#certificate}. Without a host name verifier any host
     * name is allowed for the key store and trust store, and host names are verified strictly for an SSLContext.
     */
    public static TlsSettings certificateTlsSettings(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                                                     Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                                                     HostnameVerifier hostnameVerifier, SSLContext sslContext) {
        return certificateTls(keyStorePath, keyStorePassword, keyStoreType, keyStore, trustStorePath, trustStorePassword, trustStoreType,
                trustStore, hostnameVerifier, sslContext).build();
    }

    /**
     * @param relaxedProtocol the protocol of relaxed HTTPS validation, or {@code null} for none
     * @return what the certificate authentication scheme asks for, with relaxed validation if there is a protocol for it
     */
    public static TlsSettings certificateTlsSettings(CertAuthScheme scheme, String relaxedProtocol) {
        return certificateTls(scheme.getPathToKeyStore(), scheme.getKeyStorePassword(), scheme.getKeystoreType(), scheme.getKeyStore(),
                scheme.getPathToTrustStore(), scheme.getTrustStorePassword(), scheme.getTrustStoreType(), scheme.getTrustStore(),
                scheme.getHostnameVerifier(), scheme.getSslContext()).relaxedProtocol(relaxedProtocol).build();
    }

    private static TlsSettings.Builder certificateTls(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                                                      Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                                                      HostnameVerifier hostnameVerifier, SSLContext sslContext) {
        TlsSettings.Builder tls = TlsSettings.builder()
                .keyStore(keyMaterial(keyStorePath, keyStorePassword, keyStoreType, keyStore))
                .trustStore(keyMaterial(trustStorePath, trustStorePassword, trustStoreType, trustStore))
                .sslContext(sslContext);
        if (hostnameVerifier == null) {
            return tls.hostnames(sslContext == null ? HostnameVerification.ALLOW_ALL : HostnameVerification.STRICT);
        }
        return HostnameVerifiers.hostnames(tls, hostnameVerifier);
    }

    /**
     * @return the proxy, or {@code null} if there is none
     */
    public static ProxySettings proxySettings(ProxySpecification proxy) {
        if (proxy == null) {
            return null;
        }
        return new ProxySettings(proxy.getScheme(), proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
    }

    /**
     * @return the key material, with a Groovy GString path as a String
     */
    public static KeyMaterial keyMaterial(Object path, String password, String type, KeyStore keyStore) {
        // Groovy users may give a GString
        return new KeyMaterial(GroovyTypes.isGString(path) ? path.toString() : path, password, type, keyStore);
    }
}
