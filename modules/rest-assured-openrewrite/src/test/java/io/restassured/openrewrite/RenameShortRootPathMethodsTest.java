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

class RenameShortRootPathMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaultsWithRestAssured6RootPathMethods(spec);
        spec.recipeFromResources("io.restassured.RenameShortRootPathMethods");
    }

    @Test
    void renames_the_root_path_methods_of_a_validatable_response() {
        rewriteRun(
                java(
                        """
                        import static io.restassured.RestAssured.get;
                        import static io.restassured.RestAssured.withArgs;
                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void validates() {
                                get("/x").then()
                                        .root("a.b").body("c", equalTo(1))
                                        .root("a.%s", withArgs("b")).body("c", equalTo(1))
                                        .appendRoot("c").body("d", equalTo(2))
                                        .appendRoot("%s", withArgs("d")).body("e", equalTo(3))
                                        .detachRoot("d")
                                        .noRoot().body("f", equalTo(4));
                            }
                        }
                        """,
                        """
                        import static io.restassured.RestAssured.get;
                        import static io.restassured.RestAssured.withArgs;
                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void validates() {
                                get("/x").then()
                                        .rootPath("a.b").body("c", equalTo(1))
                                        .rootPath("a.%s", withArgs("b")).body("c", equalTo(1))
                                        .appendRootPath("c").body("d", equalTo(2))
                                        .appendRootPath("%s", withArgs("d")).body("e", equalTo(3))
                                        .detachRootPath("d")
                                        .noRootPath().body("f", equalTo(4));
                            }
                        }
                        """
                )
        );
    }

    @Test
    void renames_the_root_path_methods_of_a_response_specification() {
        rewriteRun(
                java(
                        """
                        import io.restassured.specification.ResponseSpecification;

                        import static io.restassured.RestAssured.expect;
                        import static io.restassured.RestAssured.withArgs;
                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void expects() {
                                expect()
                                        .root("a.b").body("c", equalTo(1))
                                        .appendRoot("c").body("d", equalTo(2))
                                        .detachRoot("c")
                                        .noRoot().body("f", equalTo(4))
                                        .when().get("/x");
                            }

                            ResponseSpecification configure(ResponseSpecification spec) {
                                return spec.root("a.%s", withArgs("b")).appendRoot("%s", withArgs("c"));
                            }
                        }
                        """,
                        """
                        import io.restassured.specification.ResponseSpecification;

                        import static io.restassured.RestAssured.expect;
                        import static io.restassured.RestAssured.withArgs;
                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void expects() {
                                expect()
                                        .rootPath("a.b").body("c", equalTo(1))
                                        .appendRootPath("c").body("d", equalTo(2))
                                        .detachRootPath("c")
                                        .noRootPath().body("f", equalTo(4))
                                        .when().get("/x");
                            }

                            ResponseSpecification configure(ResponseSpecification spec) {
                                return spec.rootPath("a.%s", withArgs("b")).appendRootPath("%s", withArgs("c"));
                            }
                        }
                        """
                )
        );
    }

    @Test
    void renames_the_root_path_methods_of_the_mock_mvc_and_web_test_client_responses() {
        rewriteRun(
                java(
                        """
                        import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
                        import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;

                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void validates(ValidatableMockMvcResponse response) {
                                response.root("a").body("b", equalTo(1)).appendRoot("c").detachRoot("c").noRoot();
                            }

                            void validates(ValidatableWebTestClientResponse response) {
                                response.root("a").body("b", equalTo(1)).appendRoot("c").detachRoot("c").noRoot();
                            }
                        }
                        """,
                        """
                        import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
                        import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;

                        import static org.hamcrest.Matchers.equalTo;

                        class A {
                            void validates(ValidatableMockMvcResponse response) {
                                response.rootPath("a").body("b", equalTo(1)).appendRootPath("c").detachRootPath("c").noRootPath();
                            }

                            void validates(ValidatableWebTestClientResponse response) {
                                response.rootPath("a").body("b", equalTo(1)).appendRootPath("c").detachRootPath("c").noRootPath();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void renames_set_root_of_json_path_and_xml_path() {
        rewriteRun(
                java(
                        """
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.xml.XmlPath;

                        class A {
                            String json = new JsonPath("{}").setRoot("store.book").getString("author");
                            String xml = new XmlPath("<a/>").setRoot("shopping.category").getString("item");
                        }
                        """,
                        """
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.xml.XmlPath;

                        class A {
                            String json = new JsonPath("{}").setRootPath("store.book").getString("author");
                            String xml = new XmlPath("<a/>").setRootPath("shopping.category").getString("item");
                        }
                        """
                )
        );
    }
}
