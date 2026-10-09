/*
 * Copyright 2026 the original author or authors.
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
package io.restassured.internal;

import io.restassured.authentication.ExplicitNoAuthScheme;
import io.restassured.specification.AuthenticationSpecification;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreemptiveAuthSpecImplTest {

    @Test
    void basic_replaces_previous_authentication_with_a_basic_authorization_header() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().auth().basic("other", "other").header("Authorization", "previous");

        RequestSpecification returned = new PreemptiveAuthSpecImpl(spec).basic("user", "pass");

        assertThat(returned).isSameAs(spec);
        assertThat(spec.getHeaders().getValues("Authorization")).containsExactly("Basic dXNlcjpwYXNz");
        assertThat(spec.getAuthenticationScheme()).isExactlyInstanceOf(ExplicitNoAuthScheme.class);
    }

    @Test
    void oauth2_replaces_previous_authentication_with_a_bearer_authorization_header() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().auth().basic("other", "other").header("Authorization", "previous");

        RequestSpecification returned = new PreemptiveAuthSpecImpl(spec).oauth2("token");

        assertThat(returned).isSameAs(spec);
        assertThat(spec.getHeaders().getValues("Authorization")).containsExactly("Bearer token");
        assertThat(spec.getAuthenticationScheme()).isExactlyInstanceOf(ExplicitNoAuthScheme.class);
    }

    @Test
    void basic_returns_the_request_specification_and_oauth2_what_header_returns() {
        RequestSpecification spec = mock(RequestSpecification.class);
        AuthenticationSpecification auth = mock(AuthenticationSpecification.class);
        RequestSpecification afterNone = mock(RequestSpecification.class);
        RequestSpecification afterHeader = mock(RequestSpecification.class);
        when(spec.auth()).thenReturn(auth);
        when(auth.none()).thenReturn(afterNone);
        when(afterNone.header("Authorization", "Basic dXNlcjpwYXNz")).thenReturn(afterHeader);
        when(afterNone.header("Authorization", "Bearer token")).thenReturn(afterHeader);

        assertThat(new PreemptiveAuthSpecImpl(spec).basic("user", "pass")).isSameAs(spec);
        assertThat(new PreemptiveAuthSpecImpl(spec).oauth2("token")).isSameAs(afterHeader);
    }

    @Test
    void arguments_must_not_be_null() {
        PreemptiveAuthSpecImpl preemptive = new PreemptiveAuthSpecImpl(given());

        assertThatThrownBy(() -> preemptive.basic(null, "pass")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("userName cannot be null");
        assertThatThrownBy(() -> preemptive.basic("user", null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("password cannot be null");
        assertThatThrownBy(() -> preemptive.oauth2(null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("accessToken cannot be null");
    }
}
