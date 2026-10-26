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

import java.util.Base64;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tokens of the preemptive schemes and the properties of {@link FormAuthScheme}.
 */
class AuthenticationSchemePropertiesTest {

    @Test
    void preemptive_basic_auth_scheme_encodes_the_token_in_iso_8859_1() {
        PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
        scheme.setUserName("åäö");
        scheme.setPassword("€");

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
    void preemptive_oauth2_header_scheme_uses_the_access_token_as_a_bearer_token() {
        PreemptiveOAuth2HeaderScheme scheme = new PreemptiveOAuth2HeaderScheme();
        scheme.setAccessToken("accessToken");

        assertThat(scheme.generateAuthToken()).isEqualTo("Bearer accessToken");
    }

    @Test
    void preemptive_oauth2_header_scheme_without_access_token_uses_null() {
        assertThat(new PreemptiveOAuth2HeaderScheme().generateAuthToken()).isEqualTo("Bearer null");
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
}
