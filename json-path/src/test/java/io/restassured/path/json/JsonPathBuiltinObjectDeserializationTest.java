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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class JsonPathBuiltinObjectDeserializationTest {
    private static final String GREETING = "{ \"greeting\" : { \n" +
            "                \"firstName\" : \"John\", \n" +
            "                \"lastName\" : \"Doe\" \n" +
            "               }\n" +
            "}";

    private static final String GREETINGS = "{ \"greeting\" : [{ \n" +
            "                \"firstName\" : \"John\", \n" +
            "                \"lastName\" : \"Doe\" \n" +
            "                }, { \n" +
            "                \"firstName\" : \"Tom\", \n" +
            "                \"lastName\" : \"Smith\" \n" +
            "               }]\n" +
            "}";

    private final JsonParserType parserType;
    
    public JsonPathBuiltinObjectDeserializationTest(JsonParserType parserType) {
    	this.parserType = parserType;
    }

    @Parameters
    public static JsonParserType[] data() {
        return JsonParserType.values();
    }

    @Test public void
    json_path_supports_buitin_deserializers() {
        final JsonPath jsonPath = new JsonPath(GREETING)
        	.using(new JsonPathConfig().defaultParserType(parserType));

        // When
        final Greeting greeting = jsonPath.getObject("greeting", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
    }
    
    @Test public void
    json_path_supports_buitin_deserializers_with_arrays() {
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
