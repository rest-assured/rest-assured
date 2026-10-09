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

import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import io.restassured.authentication.*;
import io.restassured.config.OAuthConfig;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies what each {@link AuthenticationScheme} does to the {@link HTTPBuilder} it authenticates.
 */
class AuthenticationSchemeAuthenticateTest {

    private final DefaultHttpClient client = new DefaultHttpClient();
    private final HTTPBuilder http = httpBuilder("http://localhost:8080/base", client);

    @Test
    void basic_auth_scheme_sets_credentials_for_the_host_and_port_of_the_uri() {
        BasicAuthScheme scheme = new BasicAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");

        scheme.authenticate(http);

        Credentials credentials = client.getCredentialsProvider().getCredentials(new AuthScope("localhost", 8080));
        assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
        assertThat(credentials.getPassword()).isEqualTo("pass");
        assertThat(client.getCredentialsProvider().getCredentials(new AuthScope("otherhost", 8080))).isNull();
    }

    @Test
    void ntlm_auth_scheme_sets_nt_credentials_for_the_host_and_port_of_the_uri() {
        NTLMAuthScheme scheme = new NTLMAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");
        scheme.setWorkstation("workstation");
        scheme.setDomain("domain");

        scheme.authenticate(http);

        NTCredentials credentials = (NTCredentials) client.getCredentialsProvider().getCredentials(new AuthScope("localhost", 8080));
        assertThat(credentials.getUserName()).isEqualTo("user");
        assertThat(credentials.getPassword()).isEqualTo("pass");
        assertThat(credentials.getWorkstation()).isEqualTo("WORKSTATION");
        assertThat(credentials.getDomain()).isEqualTo("DOMAIN");
    }

    @Test
    void oauth_scheme_without_signature_signs_requests_in_the_header() {
        OAuthScheme scheme = new OAuthScheme();
        scheme.setConsumerKey("consumerKey");
        scheme.setConsumerSecret("consumerSecret");
        scheme.setAccessToken("accessToken");
        scheme.setSecretToken("secretToken");

        scheme.authenticate(http);

        AuthConfig.OAuthSigner signer = onlyOAuthSigner();
        assertThat(signer.isOAuth1).isTrue();
        assertThat(signer.signature).isEqualTo(OAuthSignature.HEADER);
        assertThat(signer.oauthConfig.getApiKey()).isEqualTo("consumerKey");
        assertThat(signer.oauthConfig.getApiSecret()).isEqualTo("consumerSecret");
        assertThat(((OAuth1AccessToken) signer.token).getToken()).isEqualTo("accessToken");
        assertThat(((OAuth1AccessToken) signer.token).getTokenSecret()).isEqualTo("secretToken");
    }

    @Test
    void oauth_scheme_with_signature_signs_requests_with_this_signature() {
        OAuthScheme scheme = new OAuthScheme();
        scheme.setConsumerKey("consumerKey");
        scheme.setConsumerSecret("consumerSecret");
        scheme.setAccessToken("accessToken");
        scheme.setSecretToken("secretToken");
        scheme.setSignature(OAuthSignature.QUERY_STRING);

        scheme.authenticate(http);

        AuthConfig.OAuthSigner signer = onlyOAuthSigner();
        assertThat(signer.isOAuth1).isTrue();
        assertThat(signer.signature).isEqualTo(OAuthSignature.QUERY_STRING);
        assertThat(signer.oauthConfig.getApiKey()).isEqualTo("consumerKey");
    }

    @Test
    void oauth_scheme_with_null_consumer_key_signs_nothing() {
        OAuthScheme scheme = new OAuthScheme();

        scheme.authenticate(http);

        assertThat(oauthSigners()).isEmpty();
    }

    @Test
    void oauth2_scheme_without_signature_signs_requests_in_the_header() {
        OAuth2Scheme scheme = new OAuth2Scheme();
        scheme.setAccessToken("accessToken");

        scheme.authenticate(http);

        AuthConfig.OAuthSigner signer = onlyOAuthSigner();
        assertThat(signer.isOAuth1).isFalse();
        assertThat(signer.signature).isEqualTo(OAuthSignature.HEADER);
        assertThat(((OAuth2AccessToken) signer.token).getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    void oauth2_scheme_with_signature_signs_requests_with_this_signature() {
        OAuth2Scheme scheme = new OAuth2Scheme();
        scheme.setAccessToken("accessToken");
        scheme.setSignature(OAuthSignature.QUERY_STRING);

        scheme.authenticate(http);

        AuthConfig.OAuthSigner signer = onlyOAuthSigner();
        assertThat(signer.isOAuth1).isFalse();
        assertThat(signer.signature).isEqualTo(OAuthSignature.QUERY_STRING);
        assertThat(((OAuth2AccessToken) signer.token).getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    void cert_auth_scheme_registers_an_https_scheme_on_the_client() {
        CertAuthScheme scheme = new CertAuthScheme();
        scheme.setPathToTrustStore("does-not-exist.p12");
        scheme.setTrustStorePassword("whatever");
        scheme.setPort(9443);

        scheme.authenticate(http);

        // The uri is http so the socket factory is created lazily, which is why a missing trust store doesn't fail here
        Scheme https = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        assertThat(https.getDefaultPort()).isEqualTo(9443);
        assertThat(https.getSchemeSocketFactory()).isInstanceOf(LazySSLSocketFactory.class);
    }

    @Test
    void cert_auth_scheme_has_default_values() {
        CertAuthScheme scheme = new CertAuthScheme();

        assertThat(scheme.getKeystoreType()).isEqualTo(KeyStore.getDefaultType());
        assertThat(scheme.getTrustStoreType()).isEqualTo(KeyStore.getDefaultType());
        assertThat(scheme.getPort()).isEqualTo(-1);
        assertThat(scheme.getPathToKeyStore()).isNull();
        assertThat(scheme.getPathToTrustStore()).isNull();
        assertThat(scheme.getKeyStorePassword()).isNull();
        assertThat(scheme.getTrustStorePassword()).isNull();
        assertThat(scheme.getKeyStore()).isNull();
        assertThat(scheme.getTrustStore()).isNull();
        assertThat(scheme.getX509HostnameVerifier()).isNull();
        assertThat(scheme.getSslSocketFactory()).isNull();
    }

    @Test
    void cert_auth_scheme_path_properties_accept_a_file_or_a_string() {
        CertAuthScheme scheme = new CertAuthScheme();
        File keyStore = new File("keystore.p12");

        scheme.setPathToKeyStore(keyStore);
        scheme.setPathToTrustStore("truststore.p12");

        assertThat(scheme.getPathToKeyStore()).isSameAs(keyStore);
        assertThat(scheme.getPathToTrustStore()).isEqualTo("truststore.p12");
    }

    @Test
    void cert_auth_scheme_uses_the_given_ssl_socket_factory_for_an_https_uri() {
        HTTPBuilder https = httpBuilder("https://localhost:8443/base", client);
        CertAuthScheme scheme = new CertAuthScheme();
        SSLSocketFactory factory = SSLSocketFactory.getSocketFactory();
        scheme.setSslSocketFactory(factory);

        scheme.authenticate(https);

        Scheme httpsScheme = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        assertThat(httpsScheme.getDefaultPort()).isEqualTo(8443);
        assertThat(httpsScheme.getSchemeSocketFactory()).isSameAs(factory);
    }

    @Test
    void preemptive_basic_auth_scheme_adds_a_basic_authorization_header() {
        PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");

        scheme.authenticate(http);

        assertThat(http.getHeaders().get("Authorization")).isEqualTo("Basic dXNlcjpwYXNz");
    }

    @Test
    void preemptive_basic_auth_scheme_encodes_the_token_in_iso_8859_1() {
        PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
        scheme.setUserName("\u00e5\u00e4\u00f6");
        scheme.setPassword("\u20ac");

        // The euro sign can't be represented in ISO-8859-1 and becomes a question mark
        assertThat(scheme.generateAuthToken()).isEqualTo("Basic 5eT2Oj8=");
    }

    @Test
    void preemptive_basic_auth_scheme_without_user_name_and_password_uses_null() {
        assertThat(new PreemptiveBasicAuthScheme().generateAuthToken()).isEqualTo("Basic bnVsbDpudWxs");
    }

    @Test
    void preemptive_basic_auth_scheme_does_not_chunk_long_tokens() {
        PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
        scheme.setUserName("a-rather-long-user-name-that-is-longer-than-seventy-six-characters-when-encoded");
        scheme.setPassword("password");

        assertThat(scheme.generateAuthToken()).doesNotContain("\n").doesNotContain("\r").
                isEqualTo("Basic " + Base64.getEncoder().encodeToString(
                        "a-rather-long-user-name-that-is-longer-than-seventy-six-characters-when-encoded:password".getBytes(ISO_8859_1)));
    }

    @Test
    void preemptive_oauth2_header_scheme_adds_a_bearer_authorization_header() {
        PreemptiveOAuth2HeaderScheme scheme = new PreemptiveOAuth2HeaderScheme();
        scheme.setAccessToken("accessToken");

        scheme.authenticate(http);

        assertThat(http.getHeaders().get("Authorization")).isEqualTo("Bearer accessToken");
    }

    @Test
    void preemptive_oauth2_header_scheme_without_access_token_uses_null() {
        assertThat(new PreemptiveOAuth2HeaderScheme().generateAuthToken()).isEqualTo("Bearer null");
    }

    @Test
    void schemes_that_do_not_authenticate_leave_the_http_builder_unchanged() {
        FormAuthScheme formAuthScheme = new FormAuthScheme();
        formAuthScheme.setUserName("user");
        formAuthScheme.setPassword("pass");
        int interceptorCount = client.getRequestInterceptorCount();
        Scheme httpsScheme = client.getConnectionManager().getSchemeRegistry().get("https");

        new NoAuthScheme().authenticate(http);
        new ExplicitNoAuthScheme().authenticate(http);
        formAuthScheme.authenticate(http);

        assertThat(client.getCredentialsProvider().getCredentials(AuthScope.ANY)).isNull();
        assertThat(client.getRequestInterceptorCount()).isEqualTo(interceptorCount);
        assertThat(http.getHeaders()).isEmpty();
        assertThat(client.getConnectionManager().getSchemeRegistry().get("https")).isSameAs(httpsScheme);
    }

    @Test
    void form_auth_scheme_properties_are_untyped() {
        FormAuthScheme scheme = new FormAuthScheme();
        Object userName = new StringBuilder("user");

        scheme.setUserName(userName);
        scheme.setPassword(42);

        assertThat(scheme.getUserName()).isSameAs(userName);
        assertThat(scheme.getPassword()).isEqualTo(42);
    }

    private static HTTPBuilder httpBuilder(String uri, DefaultHttpClient client) {
        return new HTTPBuilder(uri, true, null, null, new OAuthConfig(), client) {
            @Override
            protected Object doRequest(RequestConfigDelegate delegate) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private AuthConfig.OAuthSigner onlyOAuthSigner() {
        List<AuthConfig.OAuthSigner> signers = oauthSigners();
        assertThat(signers).hasSize(1);
        return signers.get(0);
    }

    private List<AuthConfig.OAuthSigner> oauthSigners() {
        List<AuthConfig.OAuthSigner> signers = new ArrayList<>();
        for (int i = 0; i < client.getRequestInterceptorCount(); i++) {
            HttpRequestInterceptor interceptor = client.getRequestInterceptor(i);
            if (interceptor instanceof AuthConfig.OAuthSigner) {
                signers.add((AuthConfig.OAuthSigner) interceptor);
            }
        }
        return signers;
    }
}
