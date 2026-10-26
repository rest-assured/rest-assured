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

import io.restassured.authentication.AuthenticationContext;
import io.restassured.common.spi.http.TlsSettings;

/**
 * An {@link AuthenticationContext} that takes the TLS settings of certificate authentication as they are, which
 * REST Assured's own contexts do, so that relaxed HTTPS validation can be combined with a client certificate.
 */
public interface CertificateAuthenticationContext extends AuthenticationContext {

    /**
     * Use these TLS settings for https connections, which may have relaxed validation unlike those of
     * {@link #certificate}.
     *
     * @param tls the TLS settings
     */
    void certificate(TlsSettings tls);
}
