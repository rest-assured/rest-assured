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
import io.restassured.module.mockmvc.config.MockMvcConfig;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MainConfiguration.class)
@WebAppConfiguration
public class AutoSpringSecurityConfigurerITest {

    @Autowired
    private WebApplicationContext context;

    @Test public void
    spring_security_configurer_is_automatically_applied_when_spring_security_test_is_in_classpath_when_using_the_dsl() {
        RestAssuredMockMvc.given().
                webAppContextSetup(context).
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
    spring_security_configurer_is_automatically_applied_when_spring_security_test_is_in_classpath_when_using_the_dsl_and_post_processor_is_applied_before_web_app_context_setup() {
        RestAssuredMockMvc.given().
                postProcessors(httpBasic("username", "password")).
                webAppContextSetup(context).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    spring_security_configurer_is_automatically_applied_when_spring_security_test_is_in_classpath_when_using_a_specifications_applied_before_web_app_context_setup() {
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().setPostProcessors(httpBasic("username", "password")).build();

        RestAssuredMockMvc.given().
                spec(specification).
                webAppContextSetup(context).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));
    }

    @Test public void
    spring_security_configurer_is_automatically_applied_when_spring_security_test_is_in_classpath_when_using_a_specifications_applied_after_web_app_context_setup() {
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().setPostProcessors(httpBasic("username", "password")).build();

        RestAssuredMockMvc.given().
                webAppContextSetup(context).
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
    spring_security_configurer_is_automatically_applied_when_spring_security_test_is_in_classpath_when_using_static_configuration() {
        RestAssuredMockMvc.webAppContextSetup(context);

        try {
            RestAssuredMockMvc.given().
                    postProcessors(httpBasic("username", "password")).
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

    @Test(expected = NestedServletException.class) public void
    doesnt_add_spring_security_configurer_automatically_when_mock_mvc_config_is_configured_not_to() {
        RestAssuredMockMvc.given().
                webAppContextSetup(context).
                config(RestAssuredMockMvc.config().mockMvcConfig(MockMvcConfig.mockMvcConfig().dontAutomaticallyApplySpringSecurityMockMvcConfigurer())).
                postProcessors(httpBasic("username", "password")).
                param("name", "Johan").
        when().
                get("/secured/greeting");
    }

    @Test public void
    doesnt_add_spring_security_configurer_automatically_when_a_spring_security_configurer_has_been_manually_applied() {
        final AtomicBoolean filterUsed = new AtomicBoolean(false);

        RestAssuredMockMvc.given().
                webAppContextSetup(context, springSecurity(), springSecurity(new Filter() {
                    public void init(FilterConfig filterConfig) throws ServletException {
                    }

                    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                        filterUsed.set(true);
                        chain.doFilter(request, response);
                    }

                    public void destroy() {
                    }
                })).
                postProcessors(httpBasic("username", "password")).
                param("name", "Johan").
        when().
                get("/secured/greeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!")).
                expect(authenticated().withUsername("username"));

        assertThat(filterUsed.get(), is(true));
    }
}
