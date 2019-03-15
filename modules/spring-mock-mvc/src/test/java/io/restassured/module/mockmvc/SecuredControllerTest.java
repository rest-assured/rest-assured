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

import io.restassured.module.mockmvc.http.SecuredController;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SecuredControllerTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    javax_principal_authentication_works() {
        RestAssuredMockMvc.given().
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
        RestAssuredMockMvc.given().
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
        RestAssuredMockMvc.given().
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
    spring_security_set_authentication_also_set_principal() {
        RestAssuredMockMvc.given().
                standaloneSetup(new SecuredController()).
                auth().authentication(new TestingAuthenticationToken(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()), "")).
                param("name", "Johan").
        when().
                get("/setAuthenticationSetBoth").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    spring_context_holder_is_cleared_after_test() {
        RestAssuredMockMvc.given().
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
            RestAssuredMockMvc.given().
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

    @Test public void
    statically_defined_authentication_works() {
        // Given
        RestAssuredMockMvc.authentication = RestAssuredMockMvc.principal(new Principal() {
                                             public String getName() {
                                                 return "authorized_user";
                                             }
                                        });

        // When
        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new SecuredController()).
                    param("name", "Johan").
            when().
                    get("/principalGreeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    can_override_static_auth_config_with_none() {
        exception.expectMessage("Not authorized");

        // Given
        RestAssuredMockMvc.authentication = RestAssuredMockMvc.principal(new Principal() {
                                             public String getName() {
                                                 return "authorized_user";
                                             }
                                        });

        // When
        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new SecuredController()).
                    auth().none().
                    param("name", "Johan").
            when().
                    get("/principalGreeting");
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    spring_context_holder_is_cleared_after_failed_test_when_auth_is_statically_defined() {
        RestAssuredMockMvc.authentication = RestAssuredMockMvc.principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()));

        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new SecuredController()).
                    param("name", "Johan").
            when().
                    get("/springSecurityGreeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test public void
    statically_defined_auth_has_precedence_over_statically_defined_request_spec() {
        RestAssuredMockMvc.authentication = RestAssuredMockMvc.principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()));
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().setAuth(RestAssuredMockMvc.authentication(new TestingAuthenticationToken("name", "pw"))).build();

        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new SecuredController()).
                    param("name", "Johan").
            when().
                    get("/springSecurityGreeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    statically_defined_defined_request_spec_may_include_auth() {
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().setAuth(RestAssuredMockMvc.principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()))).build();

        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new SecuredController()).
                    param("name", "Johan").
            when().
                    get("/springSecurityGreeting").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    dsl_defined_defined_request_spec_may_include_auth() {
        RestAssuredMockMvc.given().
                spec(new MockMvcRequestSpecBuilder().setAuth(RestAssuredMockMvc.principal(new User("authorized_user", "password", Collections.<GrantedAuthority>emptyList()))).build()).
                standaloneSetup(new SecuredController()).
                param("name", "Johan").
        when().
                get("/springSecurityGreeting").
        then().
                statusCode(200).
                body("content", equalTo("Hello, Johan!"));
    }
}
