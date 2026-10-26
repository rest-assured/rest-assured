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

package io.restassured.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceX509HostnameVerifierSettersTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaultsWithRestAssured6Configuration(spec);
        spec.recipeFromResources("io.restassured.ReplaceX509HostnameVerifierSetters");
    }

    @Test
    void replaces_x509_hostname_verifier_setters() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.CertAuthScheme;
                        import io.restassured.authentication.CertificateAuthSettings;
                        import io.restassured.config.SSLConfig;
                        import org.apache.http.conn.ssl.SSLSocketFactory;

                        class A {
                            SSLConfig sslConfig = SSLConfig.sslConfig().x509HostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                            CertificateAuthSettings settings = CertificateAuthSettings.certAuthSettings().x509HostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

                            void configure(CertAuthScheme scheme) {
                                scheme.setX509HostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
                            }
                        }
                        """,
                        """
                        import io.restassured.authentication.CertAuthScheme;
                        import io.restassured.authentication.CertificateAuthSettings;
                        import io.restassured.config.SSLConfig;
                        import org.apache.http.conn.ssl.SSLSocketFactory;

                        class A {
                            SSLConfig sslConfig = SSLConfig.sslConfig().hostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                            CertificateAuthSettings settings = CertificateAuthSettings.certAuthSettings().hostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

                            void configure(CertAuthScheme scheme) {
                                scheme.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_getters_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.SSLConfig;
                        import org.apache.http.conn.ssl.X509HostnameVerifier;

                        class A {
                            X509HostnameVerifier verifier = SSLConfig.sslConfig().getX509HostnameVerifier();
                        }
                        """
                )
        );
    }
}
