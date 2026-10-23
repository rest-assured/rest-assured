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

package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.restassured.RestAssured.withArgs;
import static io.restassured.module.webtestclient.matcher.RestAssuredWebTestClientMatchers.equalToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

/**
 * Pins how paths are built from root paths and arguments and evaluated through WebTestClient, with GPath as the path engine.
 */
class WebTestClientBodyPathCharacterizationTest {

    @RestController
    static class PathController {
        @GetMapping(value = "/json", produces = APPLICATION_JSON_VALUE)
        String json() {
            return "{\"store\":{\"book\":[{\"title\":\"A\"},{\"title\":\"B\"}],\"bicycle\":{\"color\":\"red\"}},\"self\":\"red\",\"$schema\":\"s\"}";
        }

        @GetMapping(value = "/xml", produces = APPLICATION_XML_VALUE)
        String xml() {
            return "<shop><category name=\"c\"><item><name>n1</name></item><item><name>n2</name></item></category></shop>";
        }
    }

    private static WebTestClientResponse get(String path) {
        return RestAssuredWebTestClient.given().standaloneSetup(new PathController()).when().get(path);
    }

    private static ValidatableWebTestClientResponse json() {
        return get("/json").then();
    }

    @Test
    void root_path_and_key_are_joined() {
        json().rootPath("store.bicycle").body("color", equalTo("red"))
                .rootPath("store.book").body("[1].title", equalTo("B")).body("title", contains("A", "B"))
                .rootPath("store.bicycle.").body(".color", equalTo("red"));
    }

    @Test
    void arguments_are_applied_after_root_path_and_key_are_joined() {
        json().rootPath("store.%s").body("[%d].title", withArgs("book", 1), equalTo("B"))
                .rootPath("store.book[%d]").body(withArgs(0), hasKey("title"));
    }

    @Test
    void response_aware_matchers_evaluate_their_path_without_the_root_path() {
        json().rootPath("store.bicycle").body("color", equalToPath("self"))
                .body("color", response -> equalTo(response.path("store.bicycle.color")));
    }

    @Test
    void failure_message_shows_the_joined_path() {
        assertThatThrownBy(() -> json().rootPath("store.book").body("[%d].title", withArgs(0), equalTo("B")))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nJSON path store.book[0].title doesn't match.\nExpected: B\n  Actual: A\n");
    }

    @Test
    void leading_dollar() {
        json().body("$", hasKey("store")).body("$.store", hasKey("book")).body("$schema", equalTo("s"));
    }

    @Test
    void path_and_extract_path() {
        assertThat((String) get("/json").path("store.book[%s].title", "1")).isEqualTo("B");
        assertThat((String) json().extract().path("store.bicycle.color")).isEqualTo("red");
        assertThat((String) get("/xml").path("shop.category.item[0].name")).isEqualTo("n1");
    }

    @Test
    void xml_paths_with_root_paths_and_arguments() {
        get("/xml").then().rootPath("shop.category").body("item[%d].name", withArgs(1), equalTo("n2")).body("@name", equalTo("c"));
    }
}
