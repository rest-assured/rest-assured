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

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.ContextAuthenticationScheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs every case of {@link AuthenticationSchemeAuthenticateTest} through {@link AuthenticationScheme#authenticate(io.restassured.authentication.AuthenticationContext)},
 * to show that a scheme has the same effect through an AuthenticationContext as through HTTPBuilder.
 */
class AuthenticationSchemeAuthenticateThroughContextTest extends AuthenticationSchemeAuthenticateTest {

    @Override
    @SuppressWarnings("deprecation")
    void authenticate(AuthenticationScheme scheme, HTTPBuilder httpBuilder) {
        ((ContextAuthenticationScheme) scheme::authenticate).authenticate(httpBuilder);
    }

    @Test
    @Override
    @SuppressWarnings("deprecation")
    void cert_auth_scheme_uses_the_given_ssl_socket_factory_for_an_https_uri() {
        CertAuthScheme scheme = new CertAuthScheme();
        scheme.setSslSocketFactory(SSLSocketFactory.getSocketFactory());

        assertThatThrownBy(() -> authenticate(scheme, http)).isInstanceOf(IllegalStateException.class);
    }
}
