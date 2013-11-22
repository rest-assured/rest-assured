/*
 * Copyright 2013 the original author or authors.
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
import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.http.client.ClientProtocolException;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.HttpClientConfig.httpClientConfig;
import static com.jayway.restassured.config.RedirectConfig.redirectConfig;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class RedirectITest extends WithJetty {

    @Test
    public void followsRedirectsByDefault() throws Exception {
        given().
                param("url", "/hello").
        expect().
                body("hello", equalTo("Hello Scalatra")).
        when().
                get("/redirect");
    }

    @Test
    public void doesntFollowRedirectsIfExplicitlySpecified() throws Exception {
        given().
                redirects().follow(false).
                param("url", "/hello").
        expect().
                statusCode(302).
                header("Location", is("http://localhost:8080/hello")).
        when().
                get("/redirect");
    }

    @Test
    public void doesntFollowRedirectsIfSpecifiedStaticallyInRedirectConfig() throws Exception {
        RestAssured.config = newConfig().redirect(redirectConfig().followRedirects(false));
        try {
            given().
                    param("url", "/hello").
            expect().
                    statusCode(302).
                    header("Location", is("http://localhost:8080/hello")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

    @Test(expected = ClientProtocolException.class)
    public void throwsClientProtocolExceptionIfMaxNumberOfRedirectAreExceeded() throws Exception {
        RestAssured.config = config().redirect(redirectConfig().followRedirects(true).and().maxRedirects(0));
        try {
            given().
                    param("url", "/hello").
            expect().
                    statusCode(302).
                    header("Location", is("http://localhost:8080/hello")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void definingRedirectConfigInTheDSLOverridesSettingsFromDefaultConfig() throws Exception {
        RestAssured.config = config().redirect(redirectConfig().followRedirects(false).and().maxRedirects(0));
        try {
            given().
                    redirects().follow(true).and().redirects().max(1).
                    param("url", "/hello").
            expect().
                    statusCode(200).
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void cookiesAreReceivedOnWhenServerReturns302() throws Exception {
        given().
                redirects().follow(false).
                param("url", "/hello").
        expect().
                statusCode(302).
                cookie("cookieName", "cookieValue").
                header("location", "http://localhost:8080/hello").
        when().
                get("/redirect-and-set-cookie");
    }

    @Test
    public void cookiesAreIncludedInRedirectsWhenCookiePolicyIsBrowserCompatibility() throws Exception {
        given().
                config(newConfig().httpClient(httpClientConfig().setParam(COOKIE_POLICY, BROWSER_COMPATIBILITY))).
                param("url", "/reflect").
        expect().
                statusCode(200).
                cookie("cookieName", "cookieValue").
        when().
                get("/redirect-and-set-cookie");
    }
}
