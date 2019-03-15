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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.expect;
import static io.restassured.parsing.Parser.JSON;
import static org.hamcrest.Matchers.equalTo;

public class DefaultParserITest extends WithJetty {

    @Test
    public void usingStaticDefaultParserParsersAnyContentWhenResponseContentTypeIsDefined() throws Exception {
        RestAssured.defaultParser = JSON;
        try {
            expect().body("message", equalTo("It works")).when().get("/customMimeTypeJsonCompatible");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingDefaultParserParsersAnyContentWhenResponseContentTypeIsDefined() throws Exception {
        expect().defaultParser(JSON).and().body("message", equalTo("It works")).when().get("/customMimeTypeJsonCompatible");
    }

    @Test
    public void usingStaticDefaultParserWhenResponseContentTypeIsUnDefined() throws Exception {
        RestAssured.defaultParser = JSON;
        try {
            expect().body("message", equalTo("It works")).when().get("/noContentTypeJsonCompatible");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingDefaultParserWhenResponseContentTypeIsUnDefined() throws Exception {
        expect().defaultParser(JSON).and().body("message", equalTo("It works")).when().get("/noContentTypeJsonCompatible");
    }
}
