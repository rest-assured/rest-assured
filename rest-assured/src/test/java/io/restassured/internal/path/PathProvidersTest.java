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

package io.restassured.internal.path;

import io.restassured.config.JsonConfig;
import io.restassured.config.JsonPathEngine;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.path.PathProviders.JsonEngine;
import io.restassured.internal.path.json.GPathJsonProvider;
import io.restassured.internal.path.xml.GPathXmlProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathProvidersTest {
    private static final String FAKE_JAYWAY = FakeJaywayProvider.class.getName();

    @Test
    void finds_gpath_for_each_document_type() {
        RestAssuredConfig config = RestAssuredConfig.config();

        assertThat(PathProviders.forType(DocumentType.JSON, config)).isExactlyInstanceOf(GPathJsonProvider.class);
        assertThat(PathProviders.forType(DocumentType.XML, config)).isExactlyInstanceOf(GPathXmlProvider.class);
        assertThat(PathProviders.forType(DocumentType.HTML, config)).isExactlyInstanceOf(GPathXmlProvider.class);
    }

    @Test
    void json_engine_can_be_configured() {
        RestAssuredConfig config = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().pathEngine(JsonPathEngine.GPATH));

        assertThat(PathProviders.forType(DocumentType.JSON, config)).isExactlyInstanceOf(GPathJsonProvider.class);
    }

    @Test
    void the_first_json_engine_on_the_classpath_is_used_when_none_is_configured() {
        List<JsonEngine> engines = List.of(new JsonEngine(JsonPathEngine.GPATH, "io.restassured.path.missing.Provider", "missing"),
                new JsonEngine(JsonPathEngine.GPATH, PathProviders.GPATH_JSON, "json-path-gpath"));

        assertThat(PathProviders.forJson(null, engines)).isExactlyInstanceOf(GPathJsonProvider.class);
    }

    @Test
    void a_configured_json_engine_that_isnt_on_the_classpath_fails() {
        List<JsonEngine> engines = List.of(new JsonEngine(JsonPathEngine.GPATH, "io.restassured.path.missing.Provider", "json-path-gpath"));

        assertThatThrownBy(() -> PathProviders.forJson(JsonPathEngine.GPATH, engines))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("JsonConfig.pathEngine is GPATH but io.rest-assured:json-path-gpath isn't on the classpath.");
    }

    @Test
    void no_json_engine_on_the_classpath_fails() {
        List<JsonEngine> engines = List.of(new JsonEngine(JsonPathEngine.GPATH, "io.restassured.path.missing.Provider", "json-path-gpath"));

        assertThatThrownBy(() -> PathProviders.forJson(null, engines))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message(null, DocumentType.JSON));
    }

    @Test
    void loads_each_provider_once() {
        assertThat(PathProviders.load(PathProviders.GPATH_JSON, "json-path-gpath")).containsSame(PathProviders.load(PathProviders.GPATH_JSON, "json-path-gpath").orElseThrow());
    }

    @Test
    void a_provider_that_isnt_on_the_classpath_is_absent() {
        assertThat(PathProviders.load("io.restassured.path.missing.Provider", "missing")).isEmpty();
    }

    @Test
    void only_jayway_is_used_when_gpath_isnt_on_the_classpath() {
        List<String> logged = new ArrayList<>();
        List<JsonEngine> engines = List.of(new JsonEngine(JsonPathEngine.GPATH, "io.restassured.path.missing.Provider", "json-path-gpath"),
                new JsonEngine(JsonPathEngine.JAYWAY, FAKE_JAYWAY, "fake-jayway"));

        assertThat(PathProviders.forJson(null, engines, null, new PathProviders.OnceLog(logged::add))).isExactlyInstanceOf(FakeJaywayProvider.class);
        assertThat(logged).isEmpty();
    }

    @Test
    void gpath_is_used_when_both_engines_are_on_the_classpath_and_that_is_logged_once() {
        List<String> logged = new ArrayList<>();
        PathProviders.OnceLog log = new PathProviders.OnceLog(logged::add);
        List<JsonEngine> engines = bothEngines();

        assertThat(PathProviders.forJson(null, engines, null, log)).isExactlyInstanceOf(GPathJsonProvider.class);
        assertThat(PathProviders.forJson(null, engines, null, log)).isExactlyInstanceOf(GPathJsonProvider.class);
        assertThat(logged).containsExactly("REST Assured uses GPath (io.rest-assured:json-path-gpath) for JSON paths, although "
                + "io.rest-assured:fake-jayway is also on the classpath. To use Jayway, set jsonConfig().pathEngine(JAYWAY) globally or on a specification.");
    }

    @Test
    void a_configured_engine_is_used_when_both_engines_are_on_the_classpath_and_nothing_is_logged() {
        List<String> logged = new ArrayList<>();
        PathProviders.OnceLog log = new PathProviders.OnceLog(logged::add);

        assertThat(PathProviders.forJson(JsonPathEngine.JAYWAY, bothEngines(), null, log)).isExactlyInstanceOf(FakeJaywayProvider.class);
        assertThat(PathProviders.forJson(JsonPathEngine.GPATH, bothEngines(), null, log)).isExactlyInstanceOf(GPathJsonProvider.class);
        assertThat(logged).isEmpty();
    }

    @Test
    void jayway_is_the_second_json_engine() {
        assertThat(PathProviders.JSON_ENGINES).containsExactly(
                new JsonEngine(JsonPathEngine.GPATH, PathProviders.GPATH_JSON, "json-path-gpath"),
                new JsonEngine(JsonPathEngine.JAYWAY, PathProviders.JAYWAY_JSON, "json-path-jayway"));
    }

    @Test
    void configured_jayway_that_isnt_on_the_classpath_fails() {
        assertThatThrownBy(() -> PathProviders.forType(DocumentType.JSON, RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().pathEngine(JsonPathEngine.JAYWAY))))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("JsonConfig.pathEngine is JAYWAY but io.rest-assured:json-path-jayway isn't on the classpath.");
    }

    @Test
    void a_class_that_isnt_on_the_classpath_isnt_present() {
        assertThat(PathProviders.isPresent(PathProviders.GPATH_JSON)).isTrue();
        assertThat(PathProviders.isPresent("io.restassured.path.missing.Provider")).isFalse();
    }

    @Test
    void a_missing_path_is_null() {
        PathProvider provider = new FakeJaywayProvider();

        assertThat(PathProviders.valueOf(provider, PathResult.value("x"))).isEqualTo("x");
        assertThat(PathProviders.valueOf(provider, PathResult.value(null))).isNull();
        assertThat(PathProviders.valueOf(provider, PathResult.missing())).isNull();
    }

    @Test
    void a_list_addressed_as_an_object_fails_with_the_message_of_the_engine() {
        assertThatThrownBy(() -> PathProviders.valueOf(new FakeJaywayProvider(), PathResult.listAddressedAsObject("a is a list, use a[*].b"), () -> true))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("a is a list, use a[*].b");
    }

    @Test
    void an_invalid_path_fails_with_the_message_of_the_engine() {
        assertThatThrownBy(() -> PathProviders.valueOf(new FakeJaywayProvider(), PathResult.invalidPath("a { b } is invalid"), () -> true))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("a { b } is invalid");
    }

    @Test
    void without_gpath_a_failure_of_another_engine_suggests_gpath() {
        String hint = " If this is a GPath path from REST Assured 6.x, add io.rest-assured:json-path-gpath:" + MissingPathEngine.version()
                + " to your test dependencies.";

        assertThatThrownBy(() -> PathProviders.valueOf(new FakeJaywayProvider(), PathResult.listAddressedAsObject("a is a list, use a[*].b"), () -> false))
                .hasMessage("a is a list, use a[*].b" + hint);
        assertThatThrownBy(() -> PathProviders.valueOf(new FakeJaywayProvider(), PathResult.invalidPath("a { b } is invalid"), () -> false))
                .hasMessage("a { b } is invalid" + hint);
    }

    @Test
    void a_failure_of_gpath_suggests_nothing() {
        assertThatThrownBy(() -> PathProviders.valueOf(new GPathJsonProvider(), PathResult.invalidPath("invalid"), () -> false))
                .hasMessage("invalid");
    }

    private static List<JsonEngine> bothEngines() {
        return List.of(new JsonEngine(JsonPathEngine.GPATH, PathProviders.GPATH_JSON, "json-path-gpath"),
                new JsonEngine(JsonPathEngine.JAYWAY, FAKE_JAYWAY, "fake-jayway"));
    }
}
