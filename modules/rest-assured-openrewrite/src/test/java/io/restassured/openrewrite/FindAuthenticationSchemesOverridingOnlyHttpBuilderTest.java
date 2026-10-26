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

class FindAuthenticationSchemesOverridingOnlyHttpBuilderTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // With REST Assured 6's PreemptiveBasicAuthScheme, which has the authenticate(HTTPBuilder) that super calls
        RecipeTestSupport.defaultsWithRestAssured6Configuration(spec);
        spec.recipe(new FindAuthenticationSchemesOverridingOnlyHttpBuilder());
    }

    @Test
    void finds_subclass_of_authentication_scheme_that_only_overrides_authenticate_http_builder() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.PreemptiveBasicAuthScheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class MyScheme extends PreemptiveBasicAuthScheme {
                            @Override
                            public void authenticate(HTTPBuilder httpBuilder) {
                                super.authenticate(httpBuilder);
                            }
                        }
                        """,
                        """
                        import io.restassured.authentication.PreemptiveBasicAuthScheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class MyScheme extends PreemptiveBasicAuthScheme {
                            /*~~(REST Assured 7.0 calls authenticate(AuthenticationContext) instead of authenticate(HTTPBuilder), so this override won't be called. Override authenticate(AuthenticationContext) instead.)~~>*/@Override
                            public void authenticate(HTTPBuilder httpBuilder) {
                                super.authenticate(httpBuilder);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void finds_anonymous_and_indirect_subclasses() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.AuthenticationContext;
                        import io.restassured.authentication.AuthenticationScheme;
                        import io.restassured.authentication.OAuth2Scheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class A {
                            AuthenticationScheme scheme = new OAuth2Scheme() {
                                @Override
                                public void authenticate(HTTPBuilder httpBuilder) {
                                }
                            };

                            static class Base extends OAuth2Scheme {
                                @Override
                                public void authenticate(AuthenticationContext context) {
                                }
                            }

                            static class Sub extends Base {
                                @Override
                                public void authenticate(HTTPBuilder httpBuilder) {
                                }
                            }
                        }
                        """,
                        """
                        import io.restassured.authentication.AuthenticationContext;
                        import io.restassured.authentication.AuthenticationScheme;
                        import io.restassured.authentication.OAuth2Scheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class A {
                            AuthenticationScheme scheme = new OAuth2Scheme() {
                                /*~~(REST Assured 7.0 calls authenticate(AuthenticationContext) instead of authenticate(HTTPBuilder), so this override won't be called. Override authenticate(AuthenticationContext) instead.)~~>*/@Override
                                public void authenticate(HTTPBuilder httpBuilder) {
                                }
                            };

                            static class Base extends OAuth2Scheme {
                                @Override
                                public void authenticate(AuthenticationContext context) {
                                }
                            }

                            static class Sub extends Base {
                                /*~~(REST Assured 7.0 calls authenticate(AuthenticationContext) instead of authenticate(HTTPBuilder), so this override won't be called. Override authenticate(AuthenticationContext) instead.)~~>*/@Override
                                public void authenticate(HTTPBuilder httpBuilder) {
                                }
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_schemes_that_override_authenticate_authentication_context_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.AuthenticationContext;
                        import io.restassured.authentication.BasicAuthScheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class MyScheme extends BasicAuthScheme {
                            @Override
                            public void authenticate(HTTPBuilder httpBuilder) {
                            }

                            @Override
                            public void authenticate(AuthenticationContext context) {
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_schemes_that_do_not_extend_a_built_in_scheme_alone() {
        rewriteRun(
                java(
                        """
                        import io.restassured.authentication.AuthenticationScheme;
                        import io.restassured.internal.http.HTTPBuilder;

                        class MyScheme implements AuthenticationScheme {
                            @Override
                            public void authenticate(HTTPBuilder httpBuilder) {
                            }
                        }
                        """
                )
        );
    }
}
