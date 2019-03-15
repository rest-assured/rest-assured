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

package io.restassured.examples.springmvc.controller;

import io.restassured.examples.springmvc.config.MainConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
public class MockMvcSecurityITest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

     @Before
     public void setup() {
         mvc = MockMvcBuilders
                 .webAppContextSetup(context)
                 .apply(springSecurity())
                 .build();
     }

    @Test public void
    basic_auth_request_post_processor_works() throws Exception {
        RestAssuredMockMvc.given().
                mockMvc(mvc).
                auth().with(httpBasic("username", "password")).
                param("name", "Johan").
         when().
                get("/secured/greeting").
         then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
   }

    @Test public void
    can_specify_request_post_processor_statically_for_authentication() throws Exception {
        RestAssuredMockMvc.authentication = RestAssuredMockMvc.with(httpBasic("username", "password"));

        try {
            RestAssuredMockMvc.given().
                    mockMvc(mvc).
                    param("name", "Johan").
             when().
                    get("/secured/greeting").
             then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!")).
                    expect(authenticated().withUsername("username"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    can_specify_authentication_request_post_processor_using_spec_builder() throws Exception {
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().setAuth(RestAssuredMockMvc.with(httpBasic("username", "password"))).build();

        RestAssuredMockMvc.given().
                mockMvc(mvc).
                spec(specification).
                param("name", "Johan").
         when().
                get("/secured/greeting").
         then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    basic_auth_request_post_processor_works_with_explicit_user() throws Exception {
        RestAssuredMockMvc.given().
                mockMvc(mvc).
                auth().with(httpBasic("username", "password"), user("username").password("password")).
                param("name", "Johan").
         when().
                get("/secured/greeting").
         then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    can_specify_user_for_controllers_not_protected_by_basic_auth() throws Exception {
        RestAssuredMockMvc.given().
                mockMvc(mvc).
                auth().with(user("authorized_user").password("password")).
                param("name", "Johan").
         when().
                get("/user/greeting").
         then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("authorized_user"));
    }

    @WithMockUser(username = "authorized_user")
    @Test public void
    can_use_spring_security_mock_annotations() throws Exception {
        RestAssuredMockMvc.given().
                mockMvc(mvc).
                param("name", "Johan").
         when().
                get("/user/greeting").
         then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("authorized_user"));
   }

    @Test public void
    can_authenticate_using_dsl_post_processors() throws Exception {
        RestAssuredMockMvc.given().
                mockMvc(mvc).
                postProcessors(httpBasic("username", "password")).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    can_authenticate_using_static_post_processors() throws Exception {
        RestAssuredMockMvc.postProcessors(httpBasic("username", "password"));

        try {
            RestAssuredMockMvc.given().
                    mockMvc(mvc).
                    param("name", "Johan").
            when().
                    get("/secured/greeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!")).
                    expect(authenticated().withUsername("username"));
        } finally {
            RestAssuredMockMvc.reset();
        }

        assertThat(RestAssuredMockMvc.postProcessors(), hasSize(0));
    }

    @Test public void
    can_authenticate_using_post_processors_in_spec() throws Exception {
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().setPostProcessors(httpBasic("username", "password")).build();

        RestAssuredMockMvc.given().
                mockMvc(mvc).
                spec(specification).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

}
