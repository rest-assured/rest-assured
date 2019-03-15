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

import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

public class GzipITest extends WithJetty {

    /**
     * Asserts that <a href="https://github.com/rest-assured/rest-assured/issues/814">issue 814</a> is resolved
     */
    @Test public void
    returns_empty_body_when_content_encoding_is_gzip_but_body_is_empty() {
        when().
                get("/gzip-empty-body").
                then().
                statusCode(200).
                body(emptyString());
    }

    @Test public void
    returns_json_body_when_content_encoding_is_gzip_and_body_is_json() {
        when().
                get("/gzip-json").
                then().
                statusCode(200).
                body("hello", equalTo("Hello Scalatra"));
    }
}