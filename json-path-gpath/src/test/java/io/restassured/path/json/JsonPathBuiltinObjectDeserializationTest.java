/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.path.json;

import io.restassured.path.json.config.JsonParserType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.support.Greeting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JsonPathBuiltinObjectDeserializationTest {
    private static final String GREETING = """
        { "greeting" : {
                "firstName" : "John",
                "lastName" : "Doe"
               }
        }""";

    private static final String GREETINGS = """
        { "greeting" : [{
                "firstName" : "John",
                "lastName" : "Doe"
                }, {
                "firstName" : "Tom",
                "lastName" : "Smith"
               }]
        }""";

    @ParameterizedTest
    @EnumSource(JsonParserType.class)
    public void json_path_supports_builtin_deserializers(JsonParserType parserType) {
        final JsonPath jsonPath = new JsonPath(GREETING)
            .using(new JsonPathConfig().defaultParserType(parserType));

        // When
        final Greeting greeting = jsonPath.getObject("greeting", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
    }

    @ParameterizedTest
    @EnumSource(JsonParserType.class)
    public void json_path_supports_builtin_deserializers_with_arrays(JsonParserType parserType) {
        final JsonPath jsonPath = new JsonPath(GREETINGS)
            .using(new JsonPathConfig().defaultParserType(parserType));

        // When
        final Greeting[] greeting = jsonPath.getObject("greeting", Greeting[].class);

        // Then
        assertThat(greeting.length, equalTo(2));
        assertThat(greeting[0].getFirstName(), equalTo("John"));
        assertThat(greeting[0].getLastName(), equalTo("Doe"));
        assertThat(greeting[1].getFirstName(), equalTo("Tom"));
        assertThat(greeting[1].getLastName(), equalTo("Smith"));
    }
}