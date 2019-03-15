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
import io.restassured.config.DecoderConfig;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class UnicodeITest extends WithJetty {

    @Test public void
    pure_body_expectations_work_for_unicode_content() {
        given().
                config(RestAssured.config().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))).
        when().
                get("/utf8-body-json").
        then().
                body(containsString("啊 ☆"));
    }

    @Test public void
    json_body_expectations_work_for_unicode_content() {
        given().
                config(RestAssured.config().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))).
        when().
                get("/utf8-body-json").
        then().
                body("value", equalTo("啊 ☆"));
    }

    @Test public void
    xml_body_expectations_work_for_unicode_content() {
        given().
                config(RestAssured.config().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"))).
        when().
                get("/utf8-body-xml").
        then().
                body("value", equalTo("啊 ☆"));
    }

    @Test public void
    unicode_values_works_in_json_content() {
        given().
                contentType(ContentType.JSON).
                body(new HashMap<String, String>() {{put("title", "äöüß€’");}}).
        when().
                post("/reflect").
        then().
                statusCode(200).
                body("title", equalTo("äöüß€’"));
    }
}
