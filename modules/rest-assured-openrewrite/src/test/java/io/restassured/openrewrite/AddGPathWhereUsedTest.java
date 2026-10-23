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

import static io.restassured.openrewrite.BuildTestSupport.VERSION;
import static io.restassured.openrewrite.BuildTestSupport.gradleProject;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.maven.Assertions.pomXml;

class AddGPathWhereUsedTest implements RewriteTest {

    private static final String BODY_PATH_TEST = """
            import static io.restassured.RestAssured.get;
            import static org.hamcrest.Matchers.equalTo;

            class GreetingTest {
                void greets() {
                    get("/greeting").then().body("greeting.firstName", equalTo("John"));
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaults(spec);
        BuildTestSupport.offlineRepository(spec);
        spec.recipe(new AddGPathWhereUsed(VERSION));
    }

    @Test
    void adds_json_path_gpath_with_the_version_and_scope_of_rest_assured_to_a_maven_build() {
        rewriteRun(
                mavenProject("app",
                        pomXml(
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <properties>
                                        <rest-assured.version>%s</rest-assured.version>
                                    </properties>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>${rest-assured.version}</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION),
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <properties>
                                        <rest-assured.version>%s</rest-assured.version>
                                    </properties>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>${rest-assured.version}</version>
                                            <scope>test</scope>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>json-path-gpath</artifactId>
                                            <version>${rest-assured.version}</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION)
                        ),
                        srcTestJava(java(BODY_PATH_TEST))
                )
        );
    }

    @Test
    void adds_only_xml_path_gpath_when_the_code_only_uses_xml_path() {
        rewriteRun(
                mavenProject("app",
                        pomXml(
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>%1$s</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION),
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>%1$s</version>
                                            <scope>test</scope>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>xml-path-gpath</artifactId>
                                            <version>%1$s</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION)
                        ),
                        srcTestJava(java(
                                """
                                import static io.restassured.RestAssured.get;

                                class GreetingTest {
                                    String greeting() {
                                        return get("/greeting.xml").xmlPath().getString("greeting.firstName");
                                    }
                                }
                                """
                        ))
                )
        );
    }

    @Test
    void leaves_a_maven_build_that_has_json_path_gpath_alone() {
        rewriteRun(
                mavenProject("app",
                        pomXml(
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>%1$s</version>
                                            <scope>test</scope>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>json-path-gpath</artifactId>
                                            <version>%1$s</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION)
                        ),
                        srcTestJava(java(BODY_PATH_TEST))
                )
        );
    }

    @Test
    void leaves_a_maven_build_whose_code_uses_no_paths_alone() {
        rewriteRun(
                mavenProject("app",
                        pomXml(
                                """
                                <project>
                                    <modelVersion>4.0.0</modelVersion>
                                    <groupId>org.example</groupId>
                                    <artifactId>app</artifactId>
                                    <version>1.0</version>
                                    <dependencies>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>rest-assured</artifactId>
                                            <version>%s</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """.formatted(VERSION)
                        ),
                        srcTestJava(java(
                                """
                                import static io.restassured.RestAssured.get;
                                import static org.hamcrest.Matchers.containsString;

                                class GreetingTest {
                                    void greets() {
                                        get("/greeting").then().statusCode(200).body(containsString("John"));
                                    }
                                }
                                """
                        ))
                )
        );
    }

    @Test
    void adds_json_path_gpath_to_a_gradle_build() {
        rewriteRun(
                mavenProject("app",
                        buildGradle(
                                """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    testImplementation 'io.rest-assured:rest-assured:%1$s'
                                }
                                """.formatted(VERSION),
                                """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    testImplementation "io.rest-assured:json-path-gpath:%1$s"
                                    testImplementation 'io.rest-assured:rest-assured:%1$s'
                                }
                                """.formatted(VERSION),
                                spec -> spec.markers(gradleProject("io.rest-assured:rest-assured:" + VERSION))
                        ),
                        srcTestJava(java(
                                """
                                import static io.restassured.RestAssured.get;

                                class GreetingTest {
                                    String firstName() {
                                        return get("/greeting").then().extract().path("greeting.firstName");
                                    }
                                }
                                """
                        ))
                )
        );
    }

    @Test
    void adds_json_path_gpath_to_a_gradle_kotlin_build() {
        rewriteRun(
                mavenProject("app",
                        buildGradleKts(
                                """
                                plugins {
                                    java
                                }

                                dependencies {
                                    testImplementation("io.rest-assured:rest-assured:%1$s")
                                }
                                """.formatted(VERSION),
                                """
                                plugins {
                                    java
                                }

                                dependencies {
                                    testImplementation("io.rest-assured:json-path-gpath:%1$s")
                                    testImplementation("io.rest-assured:rest-assured:%1$s")
                                }
                                """.formatted(VERSION),
                                spec -> spec.markers(gradleProject("io.rest-assured:rest-assured:" + VERSION))
                        ),
                        srcTestJava(java(
                                """
                                import io.restassured.path.json.JsonPath;

                                import static io.restassured.RestAssured.get;

                                class GreetingTest {
                                    String firstName() {
                                        JsonPath greeting = get("/greeting").jsonPath();
                                        return greeting.getString("greeting.firstName");
                                    }
                                }
                                """
                        ))
                )
        );
    }

    @Test
    void leaves_a_gradle_build_that_has_json_path_gpath_alone() {
        rewriteRun(
                mavenProject("app",
                        buildGradleKts(
                                """
                                plugins {
                                    java
                                }

                                dependencies {
                                    testImplementation("io.rest-assured:rest-assured:%1$s")
                                    testImplementation("io.rest-assured:json-path-gpath:%1$s")
                                }
                                """.formatted(VERSION),
                                spec -> spec.markers(gradleProject("io.rest-assured:rest-assured:" + VERSION, "io.rest-assured:json-path-gpath:" + VERSION))
                        ),
                        srcTestJava(java(BODY_PATH_TEST))
                )
        );
    }
}
