package com.jayway.restassured.itest.java;

import com.jayway.restassured.authentication.AuthenticationScheme;
import com.jayway.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.authentication.OAuthSignature.QUERY_STRING;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class OAuthITest {

    @Test public void
    oauth1_works_with_header_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret").
        when().
                get("http://term.ie/oauth/example/echo_api.php?works=true").
        then().
                body("html.body", equalTo("works=true"));
    }

    @Test public void
    oauth1_works_with_query_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret", QUERY_STRING).
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
