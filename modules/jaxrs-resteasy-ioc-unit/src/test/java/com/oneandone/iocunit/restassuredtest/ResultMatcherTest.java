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
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;
import com.oneandone.iocunit.restassuredtest.http.HeaderResource;

import io.restassured.RestAssured;

// @formatter:off
@RunWith(IocUnitRunner.class)
@SutClasses({HeaderResource.class, GreetingResource.class})
public class ResultMatcherTest {


    @Test
    public void
    unnamed_path_params_works() {
        RestAssured.given().
                when().
                get("/greeting?name={name}", "Johan").
                then().
                // expect(content().contentTypeCompatibleWith(APPLICATION_JSON)).
                statusCode(200).
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }
}
// @formatter:on