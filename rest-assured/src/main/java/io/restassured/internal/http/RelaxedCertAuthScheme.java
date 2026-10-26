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
import io.restassured.authentication.CertAuthScheme;
import io.restassured.internal.http.auth.CertificateAuthenticationContext;
import io.restassured.internal.http.client.ClientSettingsFactory;

/**
 * Certificate authentication with relaxed HTTPS validation, which {@code RestAssured.certificate(certURL, password)}
 * takes from the SSL config of {@code RestAssured.config}: the client certificate of the key store is sent, and every
 * server certificate and host name is trusted. An SSLContext set on the scheme replaces relaxed validation.
 */
public class RelaxedCertAuthScheme extends CertAuthScheme {
    private final String relaxedProtocol;

    /**
     * @param scheme          the scheme to copy
     * @param relaxedProtocol the protocol given to {@code SSLConfig.relaxedHTTPSValidation(..)}
     */
    public RelaxedCertAuthScheme(CertAuthScheme scheme, String relaxedProtocol) {
        this.relaxedProtocol = relaxedProtocol;
        setPathToKeyStore(scheme.getPathToKeyStore());
        setKeyStorePassword(scheme.getKeyStorePassword());
        setKeystoreType(scheme.getKeystoreType());
        setKeyStore(scheme.getKeyStore());
        setPathToTrustStore(scheme.getPathToTrustStore());
        setTrustStorePassword(scheme.getTrustStorePassword());
        setTrustStoreType(scheme.getTrustStoreType());
        setTrustStore(scheme.getTrustStore());
        setHostnameVerifier(scheme.getHostnameVerifier());
        setSslContext(scheme.getSslContext());
    }

    /**
     * @return the protocol of relaxed validation, or {@code null} if an SSLContext replaces it
     */
    public String getRelaxedProtocol() {
        return getSslContext() == null ? relaxedProtocol : null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the context isn't REST Assured's own, since an {@link AuthenticationContext}
     *                               can't take relaxed validation
     */
    @Override
    public void authenticate(AuthenticationContext context) {
        if (getRelaxedProtocol() == null) {
            super.authenticate(context);
        } else if (context instanceof CertificateAuthenticationContext) {
            ((CertificateAuthenticationContext) context).certificate(ClientSettingsFactory.certificateTlsSettings(this));
        } else {
            throw new IllegalStateException("Certificate authentication with the relaxed HTTPS validation of RestAssured.config " +
                    "can't be applied to " + context.getClass().getName() + ".");
        }
    }
}
