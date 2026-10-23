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

class UpgradeToRestAssured7Test implements RewriteTest {

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
        spec.recipeFromResources("io.restassured.UpgradeToRestAssured7");
    }

    @Test
    void upgrades_the_rest_assured_version_property_and_renames_json_path() {
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
                                        <rest-assured.version>6.3.0</rest-assured.version>
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
                                            <artifactId>json-path</artifactId>
                                            <version>${rest-assured.version}</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """,
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
    void adds_json_path_gpath_to_a_maven_build_that_only_has_rest_assured_and_xml_path() {
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
                                            <version>6.3.0</version>
                                            <scope>test</scope>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.rest-assured</groupId>
                                            <artifactId>xml-path</artifactId>
                                            <version>6.3.0</version>
                                            <scope>test</scope>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """,
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
    void upgrades_and_renames_in_a_gradle_build() {
        rewriteRun(
                mavenProject("app",
                        buildGradle(
                                """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    testImplementation 'io.rest-assured:json-path:6.3.0'
                                    testImplementation 'io.rest-assured:rest-assured:6.3.0'
                                }
                                """,
                                """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    testImplementation 'io.rest-assured:json-path-gpath:%1$s'
                                    testImplementation 'io.rest-assured:rest-assured:%1$s'
                                }
                                """.formatted(VERSION),
                                spec -> spec.markers(gradleProject("io.rest-assured:json-path:6.3.0", "io.rest-assured:rest-assured:6.3.0"))
                        ),
                        srcTestJava(java(BODY_PATH_TEST))
                )
        );
    }

    @Test
    void upgrades_and_adds_json_path_gpath_in_a_gradle_kotlin_build() {
        rewriteRun(
                mavenProject("app",
                        buildGradleKts(
                                """
                                plugins {
                                    java
                                }

                                dependencies {
                                    testImplementation("io.rest-assured:rest-assured:6.3.0")
                                }
                                """,
                                """
                                plugins {
                                    java
                                }

                                dependencies {
                                    testImplementation("io.rest-assured:json-path-gpath:%1$s")
                                    testImplementation("io.rest-assured:rest-assured:%1$s")
                                }
                                """.formatted(VERSION),
                                spec -> spec.markers(gradleProject("io.rest-assured:rest-assured:6.3.0"))
                        ),
                        srcTestJava(java(BODY_PATH_TEST))
                )
        );
    }

    @Test
    void leaves_a_build_that_is_already_upgraded_alone() {
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
}
