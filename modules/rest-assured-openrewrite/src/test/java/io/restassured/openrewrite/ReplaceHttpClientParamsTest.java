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

class ReplaceHttpClientParamsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaults(spec);
        spec.recipe(new ReplaceHttpClientParams());
    }

    @Test
    void replaces_parameters_given_as_http_client_constants() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.client.params.ClientPNames;
                        import org.apache.http.client.params.CookiePolicy;
                        import org.apache.http.params.CoreConnectionPNames;
                        import org.apache.http.params.CoreProtocolPNames;

                        class A {
                            HttpClientConfig config = HttpClientConfig.httpClientConfig()
                                    .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
                                    .setParam(CoreConnectionPNames.SO_TIMEOUT, 10000)
                                    .setParam(ClientPNames.CONN_MANAGER_TIMEOUT, 2000L)
                                    .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
                                    .setParam(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;

                        import java.time.Duration;

                        class A {
                            HttpClientConfig config = HttpClientConfig.httpClientConfig()
                                    .connectTimeout(Duration.ofMillis(5000))
                                    .readTimeout(Duration.ofMillis(10000))
                                    .connectionRequestTimeout(Duration.ofMillis(2000L))
                                    .keepResponseCookies(false)
                                    .expectContinue(true);
                        }
                        """
                )
        );
    }

    @Test
    void replaces_parameters_given_as_string_literals() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;

                        class A {
                            HttpClientConfig configure(int timeout, boolean expectContinue) {
                                return new HttpClientConfig()
                                        .setParam("http.connection.timeout", timeout)
                                        .setParam("http.socket.timeout", timeout * 2)
                                        .setParam("http.conn-manager.timeout", Integer.valueOf(1000))
                                        .setParam("http.protocol.cookie-policy", "ignoreCookies")
                                        .setParam("http.protocol.expect-continue", expectContinue);
                            }
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;

                        import java.time.Duration;

                        class A {
                            HttpClientConfig configure(int timeout, boolean expectContinue) {
                                return new HttpClientConfig()
                                        .connectTimeout(Duration.ofMillis(timeout))
                                        .readTimeout(Duration.ofMillis(timeout * 2))
                                        .connectionRequestTimeout(Duration.ofMillis(Integer.valueOf(1000)))
                                        .keepResponseCookies(false)
                                        .expectContinue(expectContinue);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void replaces_statically_imported_constants_and_keeps_cookies_for_other_cookie_policies() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.client.config.CookieSpecs;

                        import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
                        import static org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY;
                        import static org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT;

                        class A {
                            HttpClientConfig browser = new HttpClientConfig().setParam(COOKIE_POLICY, BROWSER_COMPATIBILITY);
                            HttpClientConfig standard = new HttpClientConfig().setParam(COOKIE_POLICY, CookieSpecs.STANDARD);
                            HttpClientConfig literal = new HttpClientConfig().setParam(COOKIE_POLICY, "compatibility");
                            HttpClientConfig ignore = new HttpClientConfig().setParam(COOKIE_POLICY, CookieSpecs.IGNORE_COOKIES);
                            HttpClientConfig timeout = new HttpClientConfig().setParam(CONNECTION_TIMEOUT, 1000);
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;

                        import java.time.Duration;

                        class A {
                            HttpClientConfig browser = new HttpClientConfig().keepResponseCookies(true);
                            HttpClientConfig standard = new HttpClientConfig().keepResponseCookies(true);
                            HttpClientConfig literal = new HttpClientConfig().keepResponseCookies(true);
                            HttpClientConfig ignore = new HttpClientConfig().keepResponseCookies(false);
                            HttpClientConfig timeout = new HttpClientConfig().connectTimeout(Duration.ofMillis(1000));
                        }
                        """
                )
        );
    }

    @Test
    void leaves_other_parameters_and_unsupported_values_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.client.params.ClientPNames;
                        import org.apache.http.cookie.params.CookieSpecPNames;
                        import org.apache.http.params.CoreConnectionPNames;
                        import org.apache.http.params.CoreProtocolPNames;

                        import java.util.List;

                        class A {
                            HttpClientConfig configure(String name, Object value, String cookiePolicy) {
                                return new HttpClientConfig()
                                        .setParam(name, 1000)
                                        .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, value)
                                        .setParam(CoreConnectionPNames.SO_TIMEOUT, "1000")
                                        .setParam(CoreConnectionPNames.SO_TIMEOUT, -1)
                                        .setParam(ClientPNames.COOKIE_POLICY, cookiePolicy)
                                        .setParam(CoreProtocolPNames.USE_EXPECT_CONTINUE, "true")
                                        .setParam(CookieSpecPNames.DATE_PATTERNS, List.of("EEE, dd-MMM-yyyy HH:mm:ss z"))
                                        .setParam(CoreProtocolPNames.USER_AGENT, "agent")
                                        .setParam(ClientPNames.HANDLE_REDIRECTS, false)
                                        .setParam("http.protocol.allow-circular-redirects", true);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_set_param_of_other_classes_alone() {
        rewriteRun(
                java(
                        """
                        class Params {
                            <T> Params setParam(String name, T value) {
                                return this;
                            }

                            Params timeout() {
                                return setParam("http.connection.timeout", 1000);
                            }
                        }
                        """
                )
        );
    }
}
