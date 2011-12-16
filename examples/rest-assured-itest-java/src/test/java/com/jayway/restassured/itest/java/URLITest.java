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
import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

public class URLITest extends WithJetty {

    @Test
    public void specifyingFullyQualifiedPathOverridesDefaultValues() throws Exception {
        RestAssured.basePath = "/something";
        RestAssured.baseURI = "http://www.google.com";
        RestAssured.port = 80;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("http://localhost:8080/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Ignore("Find a way to test this (port, because it's 8080, is messing things up)")
    @Test
    public void whenBaseURIEndsWithSlashAndPathBeginsWithSlashThenOneSlashIsRemoved() throws Exception {
        RestAssured.baseURI = "http://localhost/";
        RestAssured.port = 8080;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenBaseURIIncludesPortAndEndsWithSlashAndPathBeginsWithSlashThenOneSlashIsRemoved() throws Exception {
        RestAssured.baseURI = "http://localhost:8080/";
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenBaseURIAndPathDoesntEndsWithSlashThenOneSlashIsInserted() throws Exception {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void baseURIPicksUpSchemeAndPort() throws Exception {
        RestAssured.baseURI = "http://localhost:8080/lotto";

        try {
            expect().body("lotto.lottoId", equalTo(5)).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void baseURIPicksUpSchemeAndPortAndBasePath() throws Exception {
        RestAssured.basePath = "/lotto";
        RestAssured.baseURI = "http://localhost:8080";

        try {
            expect().body("lotto.lottoId", equalTo(5)).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void basicAuthenticationWithBasePath() throws Exception {
        RestAssured.basePath = "/secured/hello";
        try {
            given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void canCallFullyQualifiedUrlsWithoutPortDefined() throws Exception {
        // This test hangs forever unless it works
        get("http://filehost-semc-rss-dev.s3.amazonaws.com/testfile1.txt");
    }
}
