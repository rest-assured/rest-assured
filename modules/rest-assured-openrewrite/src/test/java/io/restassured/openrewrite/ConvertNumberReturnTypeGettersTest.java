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

class ConvertNumberReturnTypeGettersTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaults(spec);
        spec.recipe(new ConvertNumberReturnTypeGetters());
    }

    @Test
    void converts_getter_compared_with_new_number_return_type() {
        rewriteRun(
                java(
                        """
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.config.JsonConfig;

                        class A {
                            boolean isBigDecimal(JsonConfig config) {
                                return config.numberReturnType() == NumberReturnType.BIG_DECIMAL;
                            }
                        }
                        """,
                        """
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.config.JsonConfig;

                        class A {
                            boolean isBigDecimal(JsonConfig config) {
                                return NumberReturnType.valueOf(config.numberReturnType().name()) == NumberReturnType.BIG_DECIMAL;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leaves_getters_alone_in_code_that_does_not_import_new_number_return_type() {
        rewriteRun(
                java(
                        """
                        import io.restassured.path.json.config.JsonPathConfig;

                        class A {
                            Object type(JsonPathConfig config) {
                                return config.numberReturnType();
                            }
                        }
                        """
                )
        );
    }
}
