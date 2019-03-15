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
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

public class BomITest extends WithJetty {

    @Test
    public void canParseXmlWithBom() throws Exception {
        RestAssured.expect().body("project.target.@name", equalTo("build")).when().get("/xmlWithBom");
    }

    @Test
    @Ignore
    public void xmlWithHeaderAndFooter() throws Exception {
        RestAssured.expect().log().body().body("some", equalTo("thing")).when().get("/xmlWithHeaderAndFooter");
    }
}
