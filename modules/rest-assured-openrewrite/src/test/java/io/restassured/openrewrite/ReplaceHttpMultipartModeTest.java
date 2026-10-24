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

class ReplaceHttpMultipartModeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaultsWithRestAssured6Configuration(spec);
        spec.recipe(new ReplaceHttpMultipartMode());
    }

    @Test
    void replaces_http_multipart_mode_constant() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.entity.mime.HttpMultipartMode;

                        class A {
                            HttpClientConfig config = HttpClientConfig.httpClientConfig()
                                    .httpMultipartMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                    .reuseHttpClientInstance();
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;
                        import io.restassured.config.MultipartMode;

                        class A {
                            HttpClientConfig config = HttpClientConfig.httpClientConfig()
                                    .multipartMode(MultipartMode.BROWSER_COMPATIBLE)
                                    .reuseHttpClientInstance();
                        }
                        """
                )
        );
    }

    @Test
    void replaces_statically_imported_http_multipart_mode_constants() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;

                        import static org.apache.http.entity.mime.HttpMultipartMode.RFC6532;
                        import static org.apache.http.entity.mime.HttpMultipartMode.STRICT;

                        class A {
                            HttpClientConfig strict = new HttpClientConfig().httpMultipartMode(STRICT);
                            HttpClientConfig rfc6532 = new HttpClientConfig().httpMultipartMode(RFC6532);
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;
                        import io.restassured.config.MultipartMode;

                        class A {
                            HttpClientConfig strict = new HttpClientConfig().multipartMode(MultipartMode.STRICT);
                            HttpClientConfig rfc6532 = new HttpClientConfig().multipartMode(MultipartMode.RFC6532);
                        }
                        """
                )
        );
    }

    @Test
    void keeps_http_multipart_mode_import_when_still_used() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.entity.mime.HttpMultipartMode;

                        class A {
                            HttpClientConfig strict = new HttpClientConfig().httpMultipartMode(HttpMultipartMode.STRICT);
                            HttpMultipartMode mode = HttpMultipartMode.STRICT;
                        }
                        """,
                        """
                        import io.restassured.config.HttpClientConfig;
                        import io.restassured.config.MultipartMode;
                        import org.apache.http.entity.mime.HttpMultipartMode;

                        class A {
                            HttpClientConfig strict = new HttpClientConfig().multipartMode(MultipartMode.STRICT);
                            HttpMultipartMode mode = HttpMultipartMode.STRICT;
                        }
                        """
                )
        );
    }

    @Test
    void leaves_http_multipart_mode_that_is_not_a_constant_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.HttpClientConfig;
                        import org.apache.http.entity.mime.HttpMultipartMode;

                        class A {
                            HttpClientConfig configure(HttpMultipartMode mode) {
                                return new HttpClientConfig().httpMultipartMode(mode);
                            }
                        }
                        """
                )
        );
    }
}
