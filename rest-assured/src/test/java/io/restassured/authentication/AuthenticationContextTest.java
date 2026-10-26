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

import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What each built-in scheme asks of the {@link AuthenticationContext}.
 */
@SuppressWarnings("deprecation")
class AuthenticationContextTest {

    private final RecordingContext context = new RecordingContext();

    @Test
    void basic_scheme_answers_challenges_with_basic_authentication() {
        BasicAuthScheme scheme = new BasicAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");

        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("basic[user, pass]");
    }

    @Test
    void digest_scheme_answers_challenges_with_digest_authentication() {
        DigestAuthScheme scheme = new DigestAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");

        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("digest[user, pass]");
    }

    @Test
    void ntlm_scheme_answers_challenges_with_ntlm_authentication() {
        NTLMAuthScheme scheme = new NTLMAuthScheme();
        scheme.setUserName("user");
        scheme.setPassword("pass");
        scheme.setWorkstation("workstation");
        scheme.setDomain("domain");

        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("ntlm[user, pass, workstation, domain]");
    }

    @Test
    void oauth_scheme_signs_with_oauth_and_its_signature_when_set() {
        OAuthScheme scheme = new OAuthScheme();
        scheme.setConsumerKey("key");
        scheme.setConsumerSecret("secret");
        scheme.setAccessToken("access");
        scheme.setSecretToken("token");

        scheme.authenticate(context);
        scheme.setSignature(OAuthSignature.QUERY_STRING);
        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("oauth[key, secret, access, token]", "oauth[key, secret, access, token, QUERY_STRING]");
    }

    @Test
    void oauth2_scheme_sends_the_token_and_its_signature_when_set() {
        OAuth2Scheme scheme = new OAuth2Scheme();
        scheme.setAccessToken("access");

        scheme.authenticate(context);
        scheme.setSignature(OAuthSignature.QUERY_STRING);
        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("oauth2[access]", "oauth2[access, QUERY_STRING]");
    }

    @Test
    void preemptive_schemes_set_the_authorization_header() {
        PreemptiveBasicAuthScheme basic = new PreemptiveBasicAuthScheme();
        basic.setUserName("user");
        basic.setPassword("pass");
        PreemptiveOAuth2HeaderScheme oauth2 = new PreemptiveOAuth2HeaderScheme();
        oauth2.setAccessToken("access");

        basic.authenticate(context);
        oauth2.authenticate(context);

        assertThat(context.calls).containsExactly("header[Authorization, " + basic.generateAuthToken() + "]",
                "header[Authorization, " + oauth2.generateAuthToken() + "]");
    }

    @Test
    void schemes_without_request_changes_do_nothing() {
        new NoAuthScheme().authenticate(context);
        new ExplicitNoAuthScheme().authenticate(context);
        new FormAuthScheme().authenticate(context);

        assertThat(context.calls).isEmpty();
    }

    @Test
    void certificate_scheme_uses_the_certificate_with_jsse_types() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        HostnameVerifier hostnameVerifier = (host, session) -> true;
        CertAuthScheme scheme = new CertAuthScheme();
        scheme.setPathToKeyStore("keystore.p12");
        scheme.setKeyStorePassword("password");
        scheme.setKeystoreType("PKCS12");
        scheme.setPort(8443);
        scheme.setHostnameVerifier(hostnameVerifier);
        scheme.setSslContext(sslContext);

        scheme.authenticate(context);

        assertThat(context.calls).containsExactly("certificate[keystore.p12, password, PKCS12, null, null, null, " + KeyStore.getDefaultType()
                + ", null, 8443, " + hostnameVerifier + ", " + sslContext + "]");
    }

    private static class RecordingContext implements AuthenticationContext {
        private final List<String> calls = new ArrayList<>();

        private void record(String method, Object... args) {
            calls.add(method + Arrays.toString(args));
        }

        @Override
        public void header(String name, String value) {
            record("header", name, value);
        }

        @Override
        public void basic(String userName, String password) {
            record("basic", userName, password);
        }

        @Override
        public void digest(String userName, String password) {
            record("digest", userName, password);
        }

        @Override
        public void ntlm(String userName, String password, String workstation, String domain) {
            record("ntlm", userName, password, workstation, domain);
        }

        @Override
        public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
            record("oauth", consumerKey, consumerSecret, accessToken, secretToken);
        }

        @Override
        public void oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature) {
            record("oauth", consumerKey, consumerSecret, accessToken, secretToken, signature);
        }

        @Override
        public void oauth2(String accessToken) {
            record("oauth2", accessToken);
        }

        @Override
        public void oauth2(String accessToken, OAuthSignature signature) {
            record("oauth2", accessToken, signature);
        }

        @Override
        public void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                                Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                                int port, HostnameVerifier hostnameVerifier, SSLContext sslContext) {
            record("certificate", keyStorePath, keyStorePassword, keyStoreType, keyStore, trustStorePath, trustStorePassword,
                    trustStoreType, trustStore, port, hostnameVerifier, sslContext);
        }
    }
}
