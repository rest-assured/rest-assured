/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class URLEncodingITest extends WithJetty {

    @Test
    public void urlEncodingDisabledStatically() {
        try {
            RestAssured.baseURI = "https://jira.atlassian.com";
            RestAssured.port = 443;
            RestAssured.urlEncodingEnabled = false;
            final String query = "project%20=%20BAM%20AND%20issuetype%20=%20Bug";
            expect().
                    body("issues", not(nullValue())).
                    when().
                    get("/rest/api/2.0.alpha1/search?jql={q}", query);
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    @Ignore("2.0.alpha1 no longer working")
    public void urlEncodingDisabledUsingGiven() {
        final String body = given().urlEncodingEnabled(false).get("https://jira.atlassian.com:443/rest/api/2.0.alpha1/search?jql=project%20=%20BAM%20AND%20issuetype%20=%20Bug").asString();
        assertThat(body, containsString("issues"));
    }

    @Test
    @Ignore("2.0.alpha1 no longer working")
    public void urlEncodingDisabledUsingRequestSpecBuilder() {
        final RequestSpecification specification = new RequestSpecBuilder().setUrlEncodingEnabled(false).build();

        final String body = given().specification(specification).get("https://jira.atlassian.com:443/rest/api/2.0.alpha1/search?jql=project%20=%20BAM%20AND%20issuetype%20=%20Bug").asString();
        assertThat(body, containsString("issues"));
    }

    @Test
    @Ignore("2.0.alpha1 no longer working")
    public void doubleUrlEncodingShouldFailRequest() {
        final String body = given().urlEncodingEnabled(true).get("https://jira.atlassian.com:443/rest/api/2.0.alpha1/search?jql=project%20=%20BAM%20AND%20issuetype%20=%20Bug").asString();

        assertThat(body, containsString("Error in the JQL Query"));
    }

    @Test
    public void doesntDoubleEncodeParamsWhenDefiningUrlEncodingToFalseStatically() throws Exception {
        RestAssured.urlEncodingEnabled = false;
        try {
            given().
                    formParam("formParam", "form%20param").
            then().
                    body(equalTo("query param path param form param")).
            when().
                    post("/{pathParam}/manyParams?queryParam=query%20param", "path%20param");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void doesntDoubleEncodeParamsWhenDefiningUrlEncodingToFalseNonStatically() throws Exception {
        given().
                urlEncodingEnabled(false).
                formParam("formParam", "form%20param").
        then().
                body(equalTo("query param path param form param")).
        when().
                post("/{pathParam}/manyParams?queryParam=query%20param", "path%20param");
    }
}
