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

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.JsonPathEngine;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.path.PathProviders.JsonEngine;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Runs in its own surefire execution, without the GPath modules and Groovy on the classpath (see the pom).
 */
class MissingPathEngineTest {
    private static final String NL = System.lineSeparator();

    @Test
    void the_gpath_modules_and_groovy_arent_on_the_classpath() {
        for (String className : List.of(PathProviders.GPATH_JSON, PathProviders.GPATH_XML, "groovy.lang.GroovyObject")) {
            assertThatThrownBy(() -> Class.forName(className)).as(className).isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void json_message_names_the_path_and_how_to_add_gpath() {
        assertThat(MissingPathEngine.message("lotto.winners.winnerId", DocumentType.JSON, "7.0.1")).isEqualTo(String.join(NL,
                "REST Assured can't evaluate the path \"lotto.winners.winnerId\" because no JSON path engine is on the classpath.",
                "",
                "Since REST Assured 7.0.0 the path engine is a separate dependency. Add GPath, the path syntax REST Assured has always used, "
                        + "to your test dependencies. If you're upgrading from 6.x, your existing paths keep working unchanged:",
                "  Maven:",
                "    <dependency>",
                "        <groupId>io.rest-assured</groupId>",
                "        <artifactId>json-path-gpath</artifactId>",
                "        <version>7.0.1</version>",
                "        <scope>test</scope>",
                "    </dependency>",
                "  Gradle:",
                "    testImplementation(\"io.rest-assured:json-path-gpath:7.0.1\")",
                "",
                "Or let OpenRewrite update your build and code (Gradle: see the link):",
                "  mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=io.rest-assured:rest-assured-openrewrite:7.0.1 "
                        + "-Drewrite.activeRecipes=io.restassured.UpgradeToRestAssured7",
                "",
                "More: https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes70#path-engines"));
    }

    @Test
    void xml_and_html_messages_name_xml_path_gpath() {
        assertThat(MissingPathEngine.message("shopping.category", DocumentType.XML, "7.0.1"))
                .startsWith("REST Assured can't evaluate the path \"shopping.category\" because no XML path engine is on the classpath.")
                .contains("<artifactId>xml-path-gpath</artifactId>", "testImplementation(\"io.rest-assured:xml-path-gpath:7.0.1\")")
                .doesNotContain("json-path-gpath");
        assertThat(MissingPathEngine.message("html.body", DocumentType.HTML, "7.0.1"))
                .startsWith("REST Assured can't evaluate the path \"html.body\" because no HTML path engine is on the classpath.")
                .contains("<artifactId>xml-path-gpath</artifactId>");
    }

    @Test
    void message_without_a_path() {
        assertThat(MissingPathEngine.message(null, DocumentType.JSON, "7.0.1"))
                .startsWith("REST Assured can't evaluate a path because no JSON path engine is on the classpath.");
    }

    @Test
    void version_is_read_from_the_version_file_of_rest_assured() {
        assertThat(MissingPathEngine.version()).matches("7\\.\\d+\\.\\d+(-SNAPSHOT)?");
        assertThat(MissingPathEngine.message("a", DocumentType.JSON)).contains("<version>" + MissingPathEngine.version() + "</version>");
    }

    @Test
    void finding_no_json_engine_fails_with_the_message() {
        List<JsonEngine> engines = List.of(new JsonEngine(JsonPathEngine.GPATH, PathProviders.GPATH_JSON, "json-path-gpath"));

        assertThatThrownBy(() -> PathProviders.forJson(null, engines, "a.b"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("a.b", DocumentType.JSON));
    }

    @Test
    void finding_no_xml_engine_fails_with_the_message() {
        assertThatThrownBy(() -> PathProviders.forType(DocumentType.HTML, null, "html.body"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("html.body", DocumentType.HTML));
    }

    @Test
    void json_path_of_a_response_fails_with_the_message() {
        Response response = response("application/json", "{\"a\":{\"b\":1}}");

        assertThatThrownBy(() -> response.jsonPath())
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("jsonPath()", DocumentType.JSON));
    }

    @Test
    void xml_and_html_path_of_a_response_fail_with_the_message() {
        Response response = response("application/xml", "<a><b>1</b></a>");

        assertThatThrownBy(() -> response.xmlPath())
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("xmlPath()", DocumentType.XML));
        assertThatThrownBy(() -> response.htmlPath())
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("htmlPath()", DocumentType.HTML));
    }

    @Test
    void path_of_a_response_fails_with_the_message() {
        Response json = response("application/json", "{\"a\":{\"b\":1}}");
        Response xml = response("application/xml", "<a><b>1</b></a>");

        assertThatThrownBy(() -> json.path("a.b"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("a.b", DocumentType.JSON));
        assertThatThrownBy(() -> xml.path("a.b"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("a.b", DocumentType.XML));
    }

    @Test
    void body_expectation_of_a_response_fails_with_the_message() {
        Response response = response("application/json", "{\"a\":{\"b\":1}}");

        assertThatThrownBy(() -> response.then().body("a.b", equalTo(1)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(MissingPathEngine.message("a.b", DocumentType.JSON));
    }

    @Test
    void response_without_paths_works() {
        Response response = response("application/json", "{\"a\":{\"b\":1}}");

        response.then().statusCode(200).body(equalTo("{\"a\":{\"b\":1}}"));
        assertThat(response.asString()).isEqualTo("{\"a\":{\"b\":1}}");
    }

    private static Response response(String contentType, String body) {
        return new ResponseBuilder().setStatusCode(200).setContentType(contentType).setBody(body).build();
    }
}
