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

class RemoveSslPortSettingsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaultsWithRestAssured6Configuration(spec);
        spec.recipeFromResources("io.restassured.RemoveSslPortSettings");
    }

    @Test
    void removes_the_port_settings() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.CertAuthScheme;
                        import io.restassured.authentication.CertificateAuthSettings;
                        import io.restassured.config.SSLConfig;

                        class A {
                            SSLConfig sslConfig = SSLConfig.sslConfig().port(8443).allowAllHostnames();
                            CertificateAuthSettings settings = CertificateAuthSettings.certAuthSettings().allowAllHostnames().port(8443);

                            void configure(CertAuthScheme scheme) {
                                scheme.setPort(8443);
                                scheme.setKeyStorePassword("secret");
                            }
                        }
                        """,
                        """
                        import io.restassured.authentication.CertAuthScheme;
                        import io.restassured.authentication.CertificateAuthSettings;
                        import io.restassured.config.SSLConfig;

                        class A {
                            SSLConfig sslConfig = SSLConfig.sslConfig().allowAllHostnames();
                            CertificateAuthSettings settings = CertificateAuthSettings.certAuthSettings().allowAllHostnames();

                            void configure(CertAuthScheme scheme) {
                                scheme.setKeyStorePassword("secret");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void removes_the_port_argument_of_a_certificate_in_an_authentication_context() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.AuthenticationContext;

                        class A {
                            void authenticate(AuthenticationContext context) {
                                context.certificate("keystore.p12", "secret", "PKCS12", null, null, null, null, null, 8443, null, null);
                            }
                        }
                        """,
                        """
                        import io.restassured.authentication.AuthenticationContext;

                        class A {
                            void authenticate(AuthenticationContext context) {
                                context.certificate("keystore.p12", "secret", "PKCS12", null, null, null, null, null, null, null);
                            }
                        }
                        """
                )
        );
    }
}
