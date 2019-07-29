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

package com.oneandone.iocunit.restassuredtest;

import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.ParserResource;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.parsing.Parser;

@RunWith(IocUnitRunner.class)
@SutClasses(ParserResource.class)
public class ParserTest {

    @Test
    public void
    using_static_parser_its_possible_to_parse_unknown_content_types() {
        RestAssured.responseSpecification = new ResponseSpecBuilder().registerParser("some/thing", Parser.JSON).build();

        RestAssured.given().
                param("param", "my param").
                when().
                get("/parserWithUnknownContentType").
                then().
                statusCode(200).
                contentType(equalTo("some/thing")).  // TODO: contentType(equalTo("some/thing;charset=ISO-8859-1")).
                body("param", equalTo("my param"));
    }

    @Test
    public void
    using_non_static_parser_its_possible_to_parse_unknown_content_types() {
        RestAssured.given().
                param("param", "my param").
                when().
                get("/parserWithUnknownContentType").
                then().
                parser("some/thing", Parser.JSON).
                statusCode(200).
                contentType(equalTo("some/thing")). // TODO: contentType(equalTo("some/thing;charset=ISO-8859-1")).
                body("param", equalTo("my param"));
    }
}
