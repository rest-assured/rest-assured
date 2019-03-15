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

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HeaderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Header;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import org.junit.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class HeaderConfigITest extends WithJetty {

    @Test public void
    merges_headers_by_default() {
        List<Header> list =
        given().
                header("header1", "value1").
                header("header1", "value2").
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(2));
    }

    @Test public void
    overwrite_headers_configured_by_default_header_config() {
        List<Header> list =
        given().
                config(RestAssuredConfig.config().headerConfig(HeaderConfig.headerConfig().overwriteHeadersWithName("header1"))).
                header("header1", "value1").
                header("header1", "value2").
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(1));
        assertThat(list.get(0).getValue(), equalTo("value2"));
    }

    @Test public void
    overwrite_headers_defined_at_once_configured_by_default_header_config() {
        List<Header> list =
        given().
                config(RestAssuredConfig.config().headerConfig(HeaderConfig.headerConfig().overwriteHeadersWithName("header1"))).
                header("header1", "value1", "value2").
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(1));
        assertThat(list.get(0).getValue(), equalTo("value2"));
    }

    @Test public void
    overwrite_headers_defined_using_headers_construct_configured_by_default_header_config() {
        List<Header> list =
        given().
                config(RestAssuredConfig.config().headerConfig(HeaderConfig.headerConfig().overwriteHeadersWithName("header1"))).
                headers("header1", "value1", "header3", "value3", "header1", "value2").
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(1));
        assertThat(list.get(0).getValue(), equalTo("value2"));
    }

    @Test public void
    request_spec_merges_headers_by_default() {
        RequestSpecification specification = new RequestSpecBuilder().addHeader("header1", "value2").build();

        List<Header> list =
        given().
                header("header1", "value1").
                spec(specification).
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(2));
    }

    @Test public void
    request_spec_overwrites_headers_when_configured_in_header_config() {
        RequestSpecification specification = new RequestSpecBuilder().addHeader("header1", "value2").build();

        List<Header> list =
        given().
                config(RestAssuredConfig.config().headerConfig(HeaderConfig.headerConfig().overwriteHeadersWithName("header1"))).
                header("header1", "value1").
                spec(specification).
        when().
                get("/multiHeaderReflect").
        then().
                extract().headers().getList("header1");

        assertThat(list, hasSize(1));
        assertThat(list.get(0).getValue(), equalTo("value2"));
    }
}
