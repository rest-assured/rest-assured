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

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.OAuthSignature;
import io.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.junit.Ignore;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class OAuthITest {

    @Test public void
    oauth1_url_encoded() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret").
                formParam("works", "true").
        when().
                post("http://term.ie/oauth/example/echo_api.php").
        then().
                body("html.body", equalTo("works=true"));
    }

    @Ignore("Server is down atm") @Test public void
    oauth1_works_with_header_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret").
        when().
                get("http://term.ie/oauth/example/echo_api.php?works=true").
        then().
                body("html.body", equalTo("works=true"));
    }

    @Ignore("Server is down atm") @Test public void
    oauth1_works_with_query_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret", OAuthSignature.QUERY_STRING).
        when().
                get("http://term.ie/oauth/example/echo_api.php?works=true").
        then().
                body("html.body", equalTo("works=true"));
    }

    @Test public void
    oauth2_works_with_preemptive_header_signing() {
        final String accessToken = "accessToken";

        given().
                auth().preemptive().oauth2(accessToken).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        assertThat(requestSpec.getHeaders().getValue("Authorization"), equalTo("Bearer "+accessToken));
                        return new ResponseBuilder().setBody("ok").setStatusCode(200).build();
                    }
                }).
        when().
                get("/somewhere").
        then().
                statusCode(200);
    }

    @Test public void
    oauth2_works_with_non_preemptive_header_signing() {
        final String accessToken = "accessToken";

        given().
                auth().oauth2(accessToken).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        AuthenticationScheme scheme = requestSpec.getAuthenticationScheme();
                        assertThat(scheme, instanceOf(PreemptiveOAuth2HeaderScheme.class));
                        assertThat(((PreemptiveOAuth2HeaderScheme) scheme).getAccessToken(), equalTo(accessToken));
                        return new ResponseBuilder().setBody("ok").setStatusCode(200).build();
                    }
                }).
        when().
                get("/somewhere").
        then().
                statusCode(200);
    }
}
