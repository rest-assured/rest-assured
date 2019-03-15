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

package io.restassured.module.mockmvc;

import io.restassured.http.Header;
import io.restassured.module.mockmvc.http.HeaderController;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

// @formatter:off
public class ResultHandlerTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.standaloneSetup(new HeaderController());
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    supports_using_result_handlers_using_the_response_dsl() {
        MutableObject<Boolean> mutableObject = new MutableObject<Boolean>(false);

        RestAssuredMockMvc.given().
                header(new Header("headerName", "John Doe")).
        when().
                get("/header").
        then().
                apply(print(), customResultHandler(mutableObject)).
                statusCode(200).
                body("headerName", equalTo("John Doe"));

        assertThat(mutableObject.getValue(), is(true));
    }

    @Test public void
    merges_result_handlers_using_the_dsl() {
        MutableObject<Boolean> mutableObject1 = new MutableObject<Boolean>(false);
        MutableObject<Boolean> mutableObject2 = new MutableObject<Boolean>(false);

        RestAssuredMockMvc.given().
                header(new Header("headerName", "John Doe")).
        when().
                get("/header").
        then().
                apply(customResultHandler(mutableObject1)).
                apply(customResultHandler(mutableObject2)).
                statusCode(200).
                body("headerName", equalTo("John Doe"));

        assertThat(mutableObject1.getValue(), is(true));
        assertThat(mutableObject2.getValue(), is(true));
    }

    @Test public void
    supports_defining_result_handlers_statically() {
        MutableObject<Boolean> mutableObject1 = new MutableObject<Boolean>(false);
        MutableObject<Boolean> mutableObject2 = new MutableObject<Boolean>(false);
        RestAssuredMockMvc.resultHandlers(customResultHandler(mutableObject1), customResultHandler(mutableObject2));

        RestAssuredMockMvc.given().
                header(new Header("headerName", "John Doe")).
        when().
                get("/header").
        then().
                statusCode(200).
                body("headerName", equalTo("John Doe"));

        assertThat(mutableObject1.getValue(), is(true));
        assertThat(mutableObject2.getValue(), is(true));
    }

    @Test public void
    merges_statically_defined_result_handlers_with_dsl_defined_using_response_spec() {
        MutableObject<Boolean> mutableObject1 = new MutableObject<Boolean>(false);
        MutableObject<Boolean> mutableObject2 = new MutableObject<Boolean>(false);
        RestAssuredMockMvc.resultHandlers(customResultHandler(mutableObject1));

        RestAssuredMockMvc.given().
                header(new Header("headerName", "John Doe")).
        when().
                get("/header").
        then().
                apply(customResultHandler(mutableObject2)).
                statusCode(200).
                body("headerName", equalTo("John Doe"));

        assertThat(mutableObject1.getValue(), is(true));
        assertThat(mutableObject2.getValue(), is(true));
    }


// @formatter:on

    private ResultHandler customResultHandler(final MutableObject<Boolean> mutableObject) {
        return new ResultHandler() {
            public void handle(MvcResult result) throws Exception {
                mutableObject.setValue(true);
            }
        };
    }
}
