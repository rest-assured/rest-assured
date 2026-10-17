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

class MigrateRestAssured6DeprecationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaults(spec);
        spec.recipeFromResources("io.restassured.MigrateRestAssured6Deprecations");
    }

    @Test
    void migrates_rest_assured_configuration() {
        rewriteRun(
                java(
                        """
                        import io.restassured.RestAssured;
                        import io.restassured.config.HttpClientConfig;
                        import io.restassured.config.JsonConfig;
                        import io.restassured.config.ObjectMapperConfig;
                        import io.restassured.config.RestAssuredConfig;
                        import io.restassured.config.SSLConfig;
                        import io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory;
                        import org.apache.http.client.params.ClientPNames;
                        import org.apache.http.client.params.CookiePolicy;
                        import org.apache.http.conn.ssl.SSLSocketFactory;
                        import org.apache.http.entity.mime.HttpMultipartMode;
                        import org.apache.http.params.CoreConnectionPNames;

                        import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;

                        class RestAssuredSetup {
                            static void configure() {
                                RestAssured.config = RestAssuredConfig.config()
                                        .httpClient(HttpClientConfig.httpClientConfig()
                                                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
                                                .setParam(CoreConnectionPNames.SO_TIMEOUT, 10000)
                                                .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                                                .setParam("http.useragent", "my-agent")
                                                .httpMultipartMode(HttpMultipartMode.BROWSER_COMPATIBLE))
                                        .sslConfig(SSLConfig.sslConfig().x509HostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
                                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(new DefaultJackson2ObjectMapperFactory()))
                                        .jsonConfig(JsonConfig.jsonConfig().numberReturnType(BIG_DECIMAL));
                            }

                            static boolean usesBigDecimal() {
                                return RestAssured.config().getJsonConfig().numberReturnType() == BIG_DECIMAL;
                            }

                            static boolean usesFloatOrDouble() {
                                return RestAssured.config().getJsonConfig().numberReturnType().isFloatOrDouble();
                            }
                        }
                        """,
                        """
                        import io.restassured.RestAssured;
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.common.mapper.factory.DefaultJackson2ObjectMapperFactory;
                        import io.restassured.config.*;
                        import org.apache.http.conn.ssl.SSLSocketFactory;

                        import java.time.Duration;

                        import static io.restassured.common.config.NumberReturnType.BIG_DECIMAL;

                        class RestAssuredSetup {
                            static void configure() {
                                RestAssured.config = RestAssuredConfig.config()
                                        .httpClient(HttpClientConfig.httpClientConfig()
                                                .connectTimeout(Duration.ofMillis(5000))
                                                .readTimeout(Duration.ofMillis(10000))
                                                .keepResponseCookies(true)
                                                .setParam("http.useragent", "my-agent")
                                                .multipartMode(MultipartMode.BROWSER_COMPATIBLE))
                                        .sslConfig(SSLConfig.sslConfig().hostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
                                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(new DefaultJackson2ObjectMapperFactory()))
                                        .jsonConfig(JsonConfig.jsonConfig().numberReturnType(BIG_DECIMAL));
                            }

                            static boolean usesBigDecimal() {
                                return NumberReturnType.valueOf(RestAssured.config().getJsonConfig().numberReturnType().name()) == BIG_DECIMAL;
                            }

                            static boolean usesFloatOrDouble() {
                                return RestAssured.config().getJsonConfig().numberReturnType().isFloatOrDouble();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void converts_number_return_type_getters_where_the_new_type_is_expected() {
        rewriteRun(
                java(
                        """
                        import io.restassured.path.json.config.JsonPathConfig;

                        class A {
                            JsonPathConfig.NumberReturnType type(JsonPathConfig config) {
                                return config.numberReturnType();
                            }

                            boolean isDouble(JsonPathConfig config) {
                                return config.numberReturnType().equals(JsonPathConfig.NumberReturnType.DOUBLE);
                            }

                            String name(JsonPathConfig config) {
                                switch (config.numberReturnType()) {
                                    case BIG_DECIMAL:
                                        return "big decimal";
                                    default:
                                        return config.numberReturnType().name();
                                }
                            }
                        }
                        """,
                        """
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.path.json.config.JsonPathConfig;

                        class A {
                            NumberReturnType type(JsonPathConfig config) {
                                return NumberReturnType.valueOf(config.numberReturnType().name());
                            }

                            boolean isDouble(JsonPathConfig config) {
                                return NumberReturnType.valueOf(config.numberReturnType().name()).equals(NumberReturnType.DOUBLE);
                            }

                            String name(JsonPathConfig config) {
                                switch (config.numberReturnType()) {
                                    case BIG_DECIMAL:
                                        return "big decimal";
                                    default:
                                        return config.numberReturnType().name();
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_unrelated_code_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.RestAssured;

                        class A {
                            void test() {
                                RestAssured.given().param("name", "value").when().get("/greeting").then().statusCode(200);
                            }
                        }
                        """
                )
        );
    }
}
