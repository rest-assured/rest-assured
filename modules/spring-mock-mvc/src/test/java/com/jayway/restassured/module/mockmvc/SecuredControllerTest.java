/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.SecuredController;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.util.NestedServletException;

import java.security.Principal;
import java.util.Collections;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SecuredControllerTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    javax_principal_authentication_works() {
        given().
                standaloneSetup(new SecuredController()).
                auth().principal(new Principal() {
                                     public String getName() {
                                            return "authorized_user";
                                     }
                                }).
                param("name", "Johan").
        when().
                get("/principalGreeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    spring_security_principal_authentication_works() {
        given().
                standaloneSetup(new SecuredController()).
                auth().principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList())).
                param("name", "Johan").
        when().
                get("/springSecurityGreeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    spring_security_authentication_authentication_works() {
        given().
                standaloneSetup(new SecuredController()).
                auth().authentication(new TestingAuthenticationToken(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()), "")).
                param("name", "Johan").
        when().
                get("/springSecurityGreeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    spring_context_holder_is_cleared_after_test() {
        given().
                standaloneSetup(new SecuredController()).
                auth().principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList())).
                param("name", "Johan").
        when().
                get("/springSecurityGreeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test public void
    spring_context_holder_is_cleared_after_failed_test() {
        exception.expect(NestedServletException.class);
        exception.expectMessage("Not authorized");

        try {
            given().
                    standaloneSetup(new SecuredController()).
                    auth().principal(new User("authorized_user2", "password", Collections.<GrantedAuthority>emptyList())).
                    param("name", "Johan").
            when().
                    get("/springSecurityGreeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

}
