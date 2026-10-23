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

package io.restassured.internal.path.jayway;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JaywayLibraryTest {

    @Test
    void the_jayway_on_the_classpath_is_recent_enough() {
        assertThatCode(JaywayLibrary::verify).doesNotThrowAnyException();
        assertThat(JaywayLibrary.problem(name -> true, name -> "2.10.0")).isNull();
    }

    @Test
    void jayway_missing() {
        assertThat(JaywayLibrary.problem(name -> false, name -> null))
                .isEqualTo("io.rest-assured:json-path-jayway needs Jayway JsonPath (com.jayway.jsonpath:json-path) 2.8.0 or later on the classpath.");
    }

    @Test
    void jayway_older_than_2_8_0() {
        Set<String> classesOf270 = Set.of(JaywayLibrary.JSON_PATH);

        assertThat(JaywayLibrary.problem(classesOf270::contains, name -> "2.7.0")).isEqualTo("io.rest-assured:json-path-jayway needs Jayway "
                + "JsonPath 2.8.0 or later, but 2.7.0 is on the classpath, probably set by dependency management such as Spring Boot's.");
        assertThat(JaywayLibrary.problem(classesOf270::contains, name -> null)).contains("but an older version is on the classpath");
    }

    @Test
    void the_class_that_tells_2_8_0_from_older_versions_exists() throws ClassNotFoundException {
        Class.forName(JaywayLibrary.SINCE_2_8_0);
    }
}
