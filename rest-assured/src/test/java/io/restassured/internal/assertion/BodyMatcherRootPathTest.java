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
package io.restassured.internal.assertion;

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.ContentParser;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.response.Response;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static org.assertj.core.api.Assertions.assertThat;

class BodyMatcherRootPathTest {

    private final ResponseParserRegistrar rpr = new ResponseParserRegistrar();
    private final RestAssuredConfig config = RestAssuredConfig.newConfig();

    private Map<String, Object> validate(BodyMatcher bodyMatcher, String json) {
        Response response = new ResponseBuilder().setStatusCode(200).setBody(json).setContentType(ContentType.JSON).build();
        return bodyMatcher.validate(response, new ContentParser().parse(response, rpr, config, true), config);
    }

    private BodyMatcher bodyMatcher(String rootPath, String key, Object expected) {
        BodyMatcher bodyMatcher = new BodyMatcher();
        bodyMatcher.setRootPath(rootPath);
        bodyMatcher.setKey(key);
        bodyMatcher.setMatcher(new IsEqual<>(expected));
        bodyMatcher.setRpr(rpr);
        return bodyMatcher;
    }

    @Test
    void joins_the_root_path_and_the_key_when_validating() {
        Map<String, Object> result = validate(bodyMatcher("a", "b", 2), "{\"a\":{\"b\":1}}");

        assertThat(result).containsEntry("success", false)
                .containsEntry("errorMessage", "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n");
    }

    @Test
    void applies_the_arguments_in_order_after_joining() {
        BodyMatcher bodyMatcher = bodyMatcher("%s", "%s.c", 2);
        bodyMatcher.setArguments(List.of(withArgs("a"), withArgs("b")));

        Map<String, Object> result = validate(bodyMatcher, "{\"a\":{\"b\":{\"c\":1}}}");

        assertThat(result).containsEntry("success", false)
                .containsEntry("errorMessage", "JSON path a.b.c doesn't match.\nExpected: <2>\n  Actual: <1>\n");
    }

    @Test
    void the_message_for_a_body_that_isnt_a_document_has_the_joined_path() {
        Map<String, Object> result = validate(bodyMatcher("a", "b", 2), "\"text\"");

        assertThat(result).containsEntry("success", false)
                .containsEntry("errorMessage", "Cannot assert that path \"a.b\" matches <2> because the response body equal to \"text\".");
    }
}
