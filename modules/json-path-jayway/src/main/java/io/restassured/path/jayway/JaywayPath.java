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

package io.restassured.path.jayway;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.path.jayway.JaywayEvaluator;
import io.restassured.internal.path.jayway.JaywayLibrary;
import io.restassured.internal.path.jayway.JsonParser;

/**
 * Reads a JSON document with Jayway JsonPath, configured as REST Assured's JSONPath engine is. Use it where you would
 * use {@code jsonPath()} with GPath, for example:
 * <pre>
 * String json = get("/store").asString();
 * List&lt;String&gt; titles = JaywayPath.from(json).read("$.store.book[*].title");
 * </pre>
 * Numbers with a fraction are read as floats or doubles, as with REST Assured's default JsonConfig, and the document is
 * read into plain maps and lists.
 */
public final class JaywayPath {
    private JaywayPath() {
    }

    /**
     * @param json The JSON document
     * @return Jayway's document context for the document, to read paths with
     * @throws com.jayway.jsonpath.InvalidJsonException If the document isn't valid JSON
     */
    public static DocumentContext from(String json) {
        JaywayLibrary.verify();
        return JsonPath.using(JaywayEvaluator.CONFIGURATION).parse(JsonParser.parse(json, NumberReturnType.FLOAT_AND_DOUBLE, JsonParser.DEFAULT_NUMBER_LENGTH_LIMIT));
    }
}
