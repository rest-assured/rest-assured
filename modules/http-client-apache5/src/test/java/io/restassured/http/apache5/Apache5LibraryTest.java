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

package io.restassured.http.apache5;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static io.restassured.http.apache5.Apache5Library.CLIENT_BUILDER;
import static io.restassured.http.apache5.Apache5Library.CONNECTION_CONFIG;
import static io.restassured.http.apache5.Apache5Library.HTTP_HOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Apache5LibraryTest {
    private static final Predicate<String> ALL_PRESENT = className -> true;

    private static Predicate<String> present(String... classNames) {
        return Set.of(classNames)::contains;
    }

    @Test
    void the_library_on_the_classpath_passes() {
        assertThatCode(Apache5Library::check).doesNotThrowAnyException();
        assertThat(Apache5Library.description()).matches("Apache HttpClient 5\\.\\d+.*");
    }

    @Test
    void fails_without_http_client_5() {
        assertThatThrownBy(() -> Apache5Library.check(present(), () -> null, () -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later (org.apache.httpcomponents.client5:httpclient5), "
                        + "but it isn't on the classpath.");
    }

    @Test
    void fails_without_http_core_5() {
        assertThatThrownBy(() -> Apache5Library.check(present(CLIENT_BUILDER, CONNECTION_CONFIG), () -> "5.6.5", () -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later with Apache HttpCore 5.2 or later "
                        + "(org.apache.httpcomponents.core5:httpcore5), but HttpCore 5 isn't on the classpath.");
    }

    @Test
    void fails_without_http_core_5_h2_which_the_classic_api_uses_too() {
        assertThatThrownBy(() -> Apache5Library.check(present(CLIENT_BUILDER, HTTP_HOST, CONNECTION_CONFIG), () -> "5.6.5", () -> "5.4.4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later with Apache HttpCore 5.2 or later, including "
                        + "org.apache.httpcomponents.core5:httpcore5-h2, which HttpClient 5 uses for HTTP/1.1 too, but httpcore5-h2 isn't on "
                        + "the classpath. Don't exclude it from the dependencies.");
    }

    @Test
    void fails_with_an_http_client_older_than_5_2_and_names_its_version_if_it_can_be_read() {
        Predicate<String> before5_2 = present(CLIENT_BUILDER, HTTP_HOST);

        assertThatThrownBy(() -> Apache5Library.check(before5_2, () -> "5.1.4", () -> "5.1.5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later, but 5.1.4 is on the classpath, probably set by a BOM such as Spring Boot's.");
        assertThatThrownBy(() -> Apache5Library.check(before5_2, () -> null, () -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later, but an older version is on the classpath, "
                        + "probably set by a BOM such as Spring Boot's.");
    }

    @Test
    void fails_with_an_http_core_older_than_5_2() {
        assertThatThrownBy(() -> Apache5Library.check(ALL_PRESENT, () -> "5.6.5", () -> "5.1.5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("http-client-apache5 needs Apache HttpClient 5.2.1 or later with Apache HttpCore 5.2 or later "
                        + "(org.apache.httpcomponents.core5:httpcore5), but HttpCore 5.1.5 is on the classpath, probably set by a BOM such as Spring Boot's.");
    }

    @Test
    void passes_when_the_versions_cant_be_read() {
        assertThatCode(() -> Apache5Library.check(ALL_PRESENT, () -> null, () -> null)).doesNotThrowAnyException();
        assertThatCode(() -> Apache5Library.check(ALL_PRESENT, () -> "5.2", () -> "unreadable")).doesNotThrowAnyException();
    }

    @Test
    void a_release_older_than_5_2() {
        assertThat(Apache5Library.isOlderThan5_2("5.1.5")).isTrue();
        assertThat(Apache5Library.isOlderThan5_2("4.4.16")).isTrue();
        assertThat(Apache5Library.isOlderThan5_2("5.2")).isFalse();
        assertThat(Apache5Library.isOlderThan5_2("5.2-beta1")).isFalse();
        assertThat(Apache5Library.isOlderThan5_2("5.10.1")).isFalse();
        assertThat(Apache5Library.isOlderThan5_2("6.0")).isFalse();
        assertThat(Apache5Library.isOlderThan5_2("x.y")).isFalse();
    }
}
