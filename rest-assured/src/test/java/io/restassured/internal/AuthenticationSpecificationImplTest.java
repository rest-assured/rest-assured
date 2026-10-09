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

package io.restassured.internal;

import io.restassured.authentication.*;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static io.restassured.RestAssured.given;
import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs every place where {@link AuthenticationSpecificationImpl} and {@link PreemptiveAuthSpecImpl} create an
 * authentication scheme, and verifies the properties of the scheme that is created.
 */
class AuthenticationSpecificationImplTest {

    @Test
    void basic_creates_a_basic_auth_scheme() {
        BasicAuthScheme scheme = schemeOf(given().auth().basic("user", "pass"), BasicAuthScheme.class);

        assertThat(scheme.getUserName()).isEqualTo("user");
        assertThat(scheme.getPassword()).isEqualTo("pass");
    }

    @Test
    void digest_creates_a_basic_auth_scheme() {
        BasicAuthScheme scheme = schemeOf(given().auth().digest("user", "pass"), BasicAuthScheme.class);

        assertThat(scheme.getUserName()).isEqualTo("user");
        assertThat(scheme.getPassword()).isEqualTo("pass");
    }

    @Test
    void ntlm_creates_an_ntlm_auth_scheme() {
        NTLMAuthScheme scheme = schemeOf(given().auth().ntlm("user", "pass", "workstation", "domain"), NTLMAuthScheme.class);

        assertThat(scheme.getUserName()).isEqualTo("user");
        assertThat(scheme.getPassword()).isEqualTo("pass");
        assertThat(scheme.getWorkstation()).isEqualTo("workstation");
        assertThat(scheme.getDomain()).isEqualTo("domain");
    }

    @Test
    void certificate_with_default_settings_creates_a_cert_auth_scheme() {
        CertAuthScheme scheme = schemeOf(given().auth().certificate("truststore.jks", "secret"), CertAuthScheme.class);

        assertThat(scheme.getPathToTrustStore()).isEqualTo("truststore.jks");
        assertThat(scheme.getTrustStorePassword()).isEqualTo("secret");
        assertThat(scheme.getTrustStoreType()).isEqualTo(KeyStore.getDefaultType());
        assertThat(scheme.getKeystoreType()).isEqualTo(KeyStore.getDefaultType());
        assertThat(scheme.getPort()).isEqualTo(-1);
        assertThat(scheme.getX509HostnameVerifier()).isSameAs(STRICT_HOSTNAME_VERIFIER);
        assertThat(scheme.getPathToKeyStore()).isNull();
        assertThat(scheme.getKeyStorePassword()).isNull();
        assertThat(scheme.getKeyStore()).isNull();
        assertThat(scheme.getTrustStore()).isNull();
        assertThat(scheme.getSslSocketFactory()).isNull();
    }

    @Test
    void certificate_with_settings_creates_a_cert_auth_scheme_with_these_settings() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();

        CertAuthScheme scheme = schemeOf(given().auth().certificate("truststore.p12", "secret", certAuthSettings().
                keyStoreType("PKCS12").trustStoreType("JKS").port(8443).keyStore(keyStore).trustStore(trustStore).
                allowAllHostnames().sslSocketFactory(sslSocketFactory)), CertAuthScheme.class);

        assertThat(scheme.getPathToTrustStore()).isEqualTo("truststore.p12");
        assertThat(scheme.getTrustStorePassword()).isEqualTo("secret");
        assertThat(scheme.getKeystoreType()).isEqualTo("PKCS12");
        assertThat(scheme.getTrustStoreType()).isEqualTo("JKS");
        assertThat(scheme.getPort()).isEqualTo(8443);
        assertThat(scheme.getKeyStore()).isSameAs(keyStore);
        assertThat(scheme.getTrustStore()).isSameAs(trustStore);
        assertThat(scheme.getX509HostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
        assertThat(scheme.getSslSocketFactory()).isSameAs(sslSocketFactory);
        assertThat(scheme.getPathToKeyStore()).isNull();
        assertThat(scheme.getKeyStorePassword()).isNull();
    }

    @Test
    void oauth_creates_an_oauth_scheme_without_signature() {
        OAuthScheme scheme = schemeOf(given().auth().oauth("consumerKey", "consumerSecret", "accessToken", "secretToken"), OAuthScheme.class);

        assertThat(scheme.getConsumerKey()).isEqualTo("consumerKey");
        assertThat(scheme.getConsumerSecret()).isEqualTo("consumerSecret");
        assertThat(scheme.getAccessToken()).isEqualTo("accessToken");
        assertThat(scheme.getSecretToken()).isEqualTo("secretToken");
        assertThat(scheme.getSignature()).isNull();
    }

    @Test
    void oauth_with_signature_creates_an_oauth_scheme_with_signature() {
        OAuthScheme scheme = schemeOf(given().auth().oauth("consumerKey", "consumerSecret", "accessToken", "secretToken", OAuthSignature.QUERY_STRING), OAuthScheme.class);

        assertThat(scheme.getConsumerKey()).isEqualTo("consumerKey");
        assertThat(scheme.getConsumerSecret()).isEqualTo("consumerSecret");
        assertThat(scheme.getAccessToken()).isEqualTo("accessToken");
        assertThat(scheme.getSecretToken()).isEqualTo("secretToken");
        assertThat(scheme.getSignature()).isEqualTo(OAuthSignature.QUERY_STRING);
    }

    @Test
    void oauth2_creates_a_preemptive_oauth2_header_scheme() {
        PreemptiveOAuth2HeaderScheme scheme = schemeOf(given().auth().oauth2("accessToken"), PreemptiveOAuth2HeaderScheme.class);

        assertThat(scheme.getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    void oauth2_with_header_signature_creates_a_preemptive_oauth2_header_scheme() {
        PreemptiveOAuth2HeaderScheme scheme = schemeOf(given().auth().oauth2("accessToken", OAuthSignature.HEADER), PreemptiveOAuth2HeaderScheme.class);

        assertThat(scheme.getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    void oauth2_with_query_string_signature_creates_an_oauth2_scheme() {
        OAuth2Scheme scheme = schemeOf(given().auth().oauth2("accessToken", OAuthSignature.QUERY_STRING), OAuth2Scheme.class);

        assertThat(scheme.getAccessToken()).isEqualTo("accessToken");
        assertThat(scheme.getSignature()).isEqualTo(OAuthSignature.QUERY_STRING);
    }

    @Test
    void form_creates_a_form_auth_scheme_without_config() {
        FormAuthScheme scheme = schemeOf(given().auth().form("user", "pass"), FormAuthScheme.class);

        assertThat(scheme.getUserName()).isEqualTo("user");
        assertThat(scheme.getPassword()).isEqualTo("pass");
        assertThat(scheme.getConfig()).isNull();
    }

    @Test
    void form_with_config_creates_a_form_auth_scheme_with_config() {
        FormAuthConfig config = FormAuthConfig.springSecurity();

        FormAuthScheme scheme = schemeOf(given().auth().form("user", "pass", config), FormAuthScheme.class);

        assertThat(scheme.getUserName()).isEqualTo("user");
        assertThat(scheme.getPassword()).isEqualTo("pass");
        assertThat(scheme.getConfig()).isSameAs(config);
    }

    @Test
    void none_creates_an_explicit_no_auth_scheme_and_removes_the_authorization_header() {
        RequestSpecification spec = given().header("Authorization", "Basic abc").auth().none();

        schemeOf(spec, ExplicitNoAuthScheme.class);
        assertThat(((FilterableRequestSpecification) spec).getHeaders().hasHeaderWithName("Authorization")).isFalse();
    }

    @Test
    void preemptive_basic_sets_a_basic_authorization_header() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().auth().preemptive().basic("user", "pass");

        assertThat(spec.getHeaders().getValue("Authorization")).isEqualTo("Basic dXNlcjpwYXNz");
        schemeOf(spec, ExplicitNoAuthScheme.class);
    }

    @Test
    void preemptive_oauth2_sets_a_bearer_authorization_header() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().auth().preemptive().oauth2("accessToken");

        assertThat(spec.getHeaders().getValue("Authorization")).isEqualTo("Bearer accessToken");
        schemeOf(spec, ExplicitNoAuthScheme.class);
    }

    private static <T extends AuthenticationScheme> T schemeOf(RequestSpecification spec, Class<T> type) {
        AuthenticationScheme scheme = ((FilterableRequestSpecification) spec).getAuthenticationScheme();
        assertThat(scheme).isExactlyInstanceOf(type);
        return type.cast(scheme);
    }
}
