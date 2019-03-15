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

import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.http.GreetingController;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.restdocs.RestDocumentation;

import static io.restassured.module.mockmvc.config.MockMvcConfig.mockMvcConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

public class RestDocsTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");

    @Test
    public void path_parameters_are_automatically_supported() {
        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController(), documentationConfiguration(restDocumentation)).
                queryParam("name", "John").
        when().
               get("/{path}", "greeting").
        then().
                apply(document("greeting").snippets(
                        pathParameters(
                                parameterWithName("path").description("The path to greeting")),
                        responseFields(
                                fieldWithPath("id").description("The ID of the greeting"),
                                fieldWithPath("content").description("The content of the greeting"))
                )).
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test
    public void can_disable_automatic_spring_rest_docks_mvc_support() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("urlTemplate not found. Did you use RestDocumentationRequestBuilders to build the request?");

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvcConfig.config().mockMvcConfig(mockMvcConfig().dontAutomaticallyApplySpringRestDocsMockMvcSupport())).
                standaloneSetup(new GreetingController(), documentationConfiguration(restDocumentation)).
                queryParam("name", "John").
        when().
               get("/{path}", "greeting").
        then().
                apply(document("greeting").snippets(
                        pathParameters(
                                parameterWithName("path").description("The path to greeting")),
                        responseFields(
                                fieldWithPath("id").description("The ID of the greeting"),
                                fieldWithPath("content").description("The content of the greeting"))
                )).
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }
}
