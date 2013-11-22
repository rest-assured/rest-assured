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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PathParamITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void supportsPassingPathParamsToRequestSpec() throws Exception {
        expect().body("fullName", equalTo("John Doe")).when().get("/{firstName}/{lastName}", "John", "Doe");
    }

    @Test
    public void supportsPassingPathParamsAsMapToRequestSpec() throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("firstName", "John");
        params.put("lastName", 42);
        expect().body("fullName", equalTo("John 42")).when().get("/{firstName}/{lastName}", params);
    }

    @Test
    public void supportsPassingIntPathParamsToRequestSpec() throws Exception {
        expect().body("fullName", equalTo("John 42")).when().get("/{firstName}/{lastName}", "John", 42);
    }

    @Test
    public void urlEncodesPathParams() throws Exception {
        expect().body("fullName", equalTo("John:() Doe")).when().get("/{firstName}/{lastName}", "John:()", "Doe");
    }

    @Test
    public void doesntUrlEncodesPathParamsWhenUrlEncodingIsDisabled() throws Exception {
        RestAssured.urlEncodingEnabled = false;
        final String encoded = URLEncoder.encode("John:()", "UTF-8");
        try {
            expect().body("fullName", equalTo("John:() Doe")).when().get("/{firstName}/{lastName}", encoded, "Doe");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void urlEncodesPathParamsInMap() throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("firstName", "John: å");
        params.put("lastName", "Doe");

        expect().body("fullName", equalTo("John: å Doe")).when().get("/{firstName}/{lastName}", params);
    }

    @Test
    public void doesntUrlEncodePathParamsInMapWhenUrlEncodingIsDisabled() throws Exception {
        RestAssured.urlEncodingEnabled = false;

        try {
            final Map<String, String> params = new HashMap<String, String>();
            params.put("firstName", "John%20å");
            params.put("lastName", "Doe");

            expect().body("fullName", equalTo("John å Doe")).when().get("/{firstName}/{lastName}", params);
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void supportsPassingPathParamsToGet() throws Exception {
        final String response = get("/{firstName}/{lastName}", "John", "Doe").asString();
        final String fullName = from(response).getString("fullName");
        assertThat(fullName, equalTo("John Doe"));
    }

    @Test
    public void supportsPassingPathParamsAsMapToGet() throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("firstName", "John=me");
        params.put("lastName", "Doe");

        final String response = get("/{firstName}/{lastName}", params).asString();
        final String fullName = from(response).getString("fullName");
        assertThat(fullName, equalTo("John=me Doe"));
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedPathParamsAreGreaterThanDefinedPathParams() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Illegal number of path parameters. Expected 2, was 3.");

        get("/{firstName}/{lastName}", "John", "Doe", "Real Doe");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedPathParamsAreLowerThanDefinedPathParams() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("You specified too few path parameters in the request.");

        get("/{firstName}/{lastName}", "John");
    }

    @Test
    public void supportsPassingPathParamWithGiven() throws Exception {
        given().
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsPassingPathParamWithIntWithGiven() throws Exception {
        given().
                pathParam("firstName", "John").
                pathParam("lastName", 42).
        expect().
                body("fullName", equalTo("John 42")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsPassingPathParamsWithGiven() throws Exception {
        given().
                pathParams("firstName", "John", "lastName", "Doe").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsPassingPathParamsWithIntWithGiven() throws Exception {
        given().
                pathParams("firstName", "John", "lastName", 42).
        expect().
                body("fullName", equalTo("John 42")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsPassingPathParamsWithMapWithGiven() throws Exception {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("firstName", "John");
        params.put("lastName", "Doe");

        given().
                pathParams(params).
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsPassingPathParamsWithIntWithMapWhenGiven() throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("firstName", "John");
        params.put("lastName", 42);

        given().
                pathParams(params).
        expect().
                body("fullName", equalTo("John 42")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void mergesPathParamsMapWithNonMapWhenGiven() throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("firstName", "John");

        given().
                pathParams(params).
                pathParam("lastName", "Doe").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void usingBothGivenAndRequestPathParamsThrowsIAE() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("You cannot specify both named and unnamed path params at the same time");

        given().
                pathParam("lastName", "Doe").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}", "John");
    }

    @Test
    public void passingInTwoManyPathParamsWithGivenThrowsIAE() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Illegal number of path parameters. Expected 2, was 3.");

        given().
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
                pathParam("thirdName", "Not defined").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void passingInTooFewNamedPathParamsWithGivenThrowsIAE() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("You specified too few path parameters to the request, failed to find path parameter with name 'lastName'.");

        given().
                pathParam("firstName", "John").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");
    }

    @Test
    public void canUsePathParamsWithNonStandardChars() throws Exception {
        final String nonStandardChars = "\\$£@\"){¤$";

        expect().
                body("fullName", equalTo("\\$£@\"){¤$ Last")).
        when().
                get("/{firstName}/{lastName}", nonStandardChars, "Last");

    }

    @Test
    public void passingInSinglePathParamsThatHaveBeenDefinedMultipleTimesWorks() throws Exception {
        given().
                pathParam("firstName", "John").
        expect().
                body("fullName", equalTo("John John")).
        when().
                get("/{firstName}/{firstName}");
    }

    @Test
    public void unnamedQueryParametersWorks() throws Exception {
        expect().statusCode(200).when().get("http://www.google.se/search?q={query}&hl=en", "query");
    }

    @Test
    public void throwsIllegalArgumentExceptionWhenTooManyPathParametersAreUsed() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Illegal number of path parameters. Expected 1, was 2.");

        expect().statusCode(200).when().get("http://www.google.se/search?q={query}&hl=en", "query", "ikk");
    }

    @Test
    public void throwsIllegalArgumentExceptionWhenTooFewPathParametersAreUsed() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Illegal number of path parameters. Expected 1, was 0.");

        expect().statusCode(200).when().get("http://www.google.se/search?q={query}&hl=en");
    }
}
