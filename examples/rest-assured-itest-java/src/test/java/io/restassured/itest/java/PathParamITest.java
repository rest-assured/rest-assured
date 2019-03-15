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

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.itest.java.support.RequestPathFromLogExtractor;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PathParamITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void supportsPassingPathParamsToRequestSpec() throws Exception {
        expect().body("fullName", equalTo("John Doe")).when().get("/{firstName}/{lastName}", "John", "Doe");
    }

    @Test
    public void possibleToGetOriginalRequestPathForUnnamedPathParamsFromRequestSpec() throws Exception {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUserDefinedPath(), equalTo("/{firstName}/{lastName}"));
                    assertThat(requestSpec.getURI(), equalTo("http://localhost:8080/John/Doe"));
                    assertThat(requestSpec.getDerivedPath(), equalTo("/John/Doe"));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John", "Doe").
        then().
                body("fullName", equalTo("John Doe"));
    }

    @Test
    public void possibleToGetOriginalRequestPathForNamedPathParamsUsingRequestSpec() throws Exception {
        given().
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUserDefinedPath(), equalTo("/{firstName}/{lastName}"));
                    assertThat(requestSpec.getDerivedPath(), equalTo("/John/Doe"));
                    assertThat(requestSpec.getURI(), equalTo("http://localhost:8080/John/Doe"));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}").
        then().
                body("fullName", equalTo("John Doe"));
    }

    @Test
    public void supportsPassingPathParamsAsMapToRequestSpec() throws Exception {
        final Map<String, Object> params = new HashMap<>();
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
        final Map<String, String> params = new HashMap<>();
        params.put("firstName", "John: å");
        params.put("lastName", "Doe");

        expect().body("fullName", equalTo("John: å Doe")).when().get("/{firstName}/{lastName}", params);
    }

    @Test
    public void doesntUrlEncodePathParamsInMapWhenUrlEncodingIsDisabled() throws Exception {
        RestAssured.urlEncodingEnabled = false;

        try {
            final Map<String, String> params = new HashMap<>();
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
        final String fullName = JsonPath.from(response).getString("fullName");
        assertThat(fullName, equalTo("John Doe"));
    }

    @Test
    public void supportsPassingPathParamsAsMapToGet() throws Exception {
        final Map<String, String> params = new HashMap<>();
        params.put("firstName", "John=me");
        params.put("lastName", "Doe");

        final String response = get("/{firstName}/{lastName}", params).asString();
        final String fullName = JsonPath.from(response).getString("fullName");
        assertThat(fullName, equalTo("John=me Doe"));
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedUnnamedPathParamsAreGreaterThanDefinedPathParams() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 3. Redundant path parameters are: Real Doe");

        get("/{firstName}/{lastName}", "John", "Doe", "Real Doe");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedNamedPathParamsAreEqualButDifferentToPlaceholders() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Path parameters were not correctly defined. Redundant path parameters are: x=first, y=second. Undefined path parameters are: firstName, lastName.");

        given().pathParam("x", "first").pathParam("y", "second").get("/{firstName}/{lastName}");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedNamedPathParamsAreDefinedButNoPlaceholdersAreDefined() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 0, was 2. Redundant path parameters are: x=first, y=second.");

        given().pathParam("x", "first").pathParam("y", "second").get("/x");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedNamedPathParamsIsGreaterThanDefinedPlaceholders() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 3. Redundant path parameters are: x=first, y=second. Undefined path parameters are: firstName.");

        given().pathParam("x", "first").pathParam("lastName", "Doe").pathParam("y", "second").get("/{firstName}/{lastName}");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedNamedAndUnnamedPathParamsIsGreaterThanDefinedPlaceholders() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 6. Redundant path parameters are: x=first, y=second and Doe, Last.");

        given().pathParam("x", "first").pathParam("y", "second").get("/{firstName}/{lastName}", "John", "Middle", "Doe", "Last");
    }

    @Test
    public void throwsIAEWhenNumberOfSuppliedPathParamsAreLowerThanDefinedPathParams() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 1. Undefined path parameters are: lastName");

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
        final Map<String, String> params = new HashMap<>();
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
        final Map<String, Object> params = new HashMap<>();
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
        final Map<String, Object> params = new HashMap<>();
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
    public void passingInTwoManyPathParamsWithGivenThrowsIAE() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 3.");

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
        exception.expectMessage("Invalid number of path parameters. Expected 2, was 1. Undefined path parameters are: lastName");

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
        exception.expectMessage("Invalid number of path parameters. Expected 1, was 2. Redundant path parameters are: ikk.");

        expect().statusCode(200).when().get("http://www.google.se/search?q={query}&hl=en", "query", "ikk");
    }

    @Test
    public void throwsIllegalArgumentExceptionWhenTooFewPathParametersAreUsed() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid number of path parameters. Expected 1, was 0.");

        expect().statusCode(200).when().get("http://www.google.se/search?q={query}&hl=en");
    }

    @Test
    public void mixingUnnamedPathParametersAndQueryParametersWorks() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(RestAssuredConfig.config().logConfig(new LogConfig(captor, true))).
                log().all().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        get("/{channelName}/item-import/rss/import?source={url}", "games", "http://myurl.com");

        // Then
        assertThat(RequestPathFromLogExtractor.loggedRequestPathIn(writer), equalTo("http://localhost:8080/games/item-import/rss/import?source=http%3A%2F%2Fmyurl.com"));
    }

    @Test
    public void urlEncodesUnnamedPathParametersThatContainsCurlyBracesAndEquals() throws Exception {
        // When
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(RestAssuredConfig.config().logConfig(new LogConfig(captor, true))).
                log().all().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        get("/feed?canonicalName={trackingName}&platform=ed4", "{trackingName='trackingname1'}");

        // Then
        assertThat(RequestPathFromLogExtractor.loggedRequestPathIn(writer), equalTo("http://localhost:8080/feed?canonicalName=%7BtrackingName%3D%27trackingname1%27%7D&platform=ed4"));
    }

    @Test
    public void urlEncodesNamedPathParametersThatContainsCurlyBracesAndEquals() throws Exception {
        // When
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(RestAssuredConfig.config().logConfig(new LogConfig(captor, true))).
                pathParam("trackingName", "{trackingName='trackingname1'}").
                pathParam("platform", "platform").
                log().all().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        get("/feed?canonicalName={trackingName}&{platform}=ed4");

        // Then
        assertThat(RequestPathFromLogExtractor.loggedRequestPathIn(writer), equalTo("http://localhost:8080/feed?canonicalName=%7BtrackingName%3D%27trackingname1%27%7D&platform=ed4"));
    }

    @Test
    public void unnamedPathParametersCanBeAppendedBeforeSubPath() throws Exception {
        get("/{path}.json", "something").then().assertThat().statusCode(is(200)).and().body("value", equalTo("something"));
    }

    @Test
    public void namedPathParametersCanBeAppendedBeforeSubPath() throws Exception {
        given().pathParam("path", "something").when().get("/{path}.json").then().assertThat().statusCode(is(200)).and().body("value", equalTo("something"));
    }

    @Test
    public void unnamedPathParametersCanBeAppendedAfterSubPath() throws Exception {
        get("/something.{format}", "json").then().assertThat().statusCode(is(200)).and().body("value", equalTo("something"));
    }

    @Test
    public void namedPathParametersCanBeAppendedAfterSubPath() throws Exception {
        given().pathParam("format", "json").when().get("/something.{format}").then().assertThat().statusCode(is(200)).and().body("value", equalTo("something"));
    }

    @Test
    public void namedPathParametersWorksWithUnicodeParameterValues() throws Exception {
        given().
                pathParam("param1Value", "Hello").
                pathParam("param2Value", "Hello\u0085").
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody(requestSpec.getURI()).build();
                    }
                }).
        when().
                get("/reflect?param1={param1Value}&param2={param2Value}").
        then().
                statusCode(is(200)).
                body(equalTo("http://localhost:8080/reflect?param1=Hello&param2=Hello%C2%85"));
    }

    @Test
    public void unnamedPathParametersWorksWithUnicodeParameterValues() throws Exception {
        given().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody(requestSpec.getURI()).build();
                    }
                }).
        when().
                get("/reflect?param1={param1Value}&param2={param2Value}", "Hello", "Hello\u0085").
        then().
                statusCode(is(200)).
                body(equalTo("http://localhost:8080/reflect?param1=Hello&param2=Hello%C2%85"));
    }

    @Test
    public void unnamedPathParametersWorksWhenThereAreMultipleTemplatesBetweenEachSlash() throws Exception {
        String param1Value =  "Hello";
        String param2Value =  "Hello2";

        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).setStatusLine("HTTP/1.1 200 OK").setBody(requestSpec.getURI()).build()).
        when().
                get("param1={param1Value}&param2={param2Value}", param1Value, param2Value).
        then().
                body(equalTo("http://localhost:8080/param1%3DHello%26param2%3DHello2"));
    }

    @Test
    public void namedPathParametersWorksWhenThereAreMultipleTemplatesBetweenEachSlash() throws Exception {
        String param1Value =  "Hello";
        String param2Value =  "Hello2";

        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).setStatusLine("HTTP/1.1 200 OK").setBody(requestSpec.getURI()).build()).
                pathParam("param1Value", param1Value).
                pathParam("param2Value", param2Value).
        when().
                get("param1={param1Value}&param2={param2Value}").
        then().
                body(equalTo("http://localhost:8080/param1%3DHello%26param2%3DHello2"));
    }

    @Test public void
    canSetNamedPathParameterDefinedAsFirstPathParamInPathAndConjWithAnUnnamedPathParam() {
        given().
                pathParam("firstName", "John").
        when().
                get("/{firstName}/{lastName}", "Doe").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    canSetNamedPathParameterDefinedAsLastPathParamInPathAndConjWithAnUnnamedPathParam() {
        given().
                pathParam("lastName", "Doe").
        when().
                get("/{firstName}/{lastName}", "John").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    named_path_parameters_have_precedence_over_unnamed_path_params() {
        given().
                pathParam("middleName", "The Beast").
        when().
                get("/{firstName}/{middleName}/{lastName}", "John", "Doe").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("middleName", equalTo("The Beast")).
                body("lastName", equalTo("Doe"));
    }

    @Test public void
    can_specify_space_only_named_path_params() {
        given().
                pathParam("firstName", "John").
                pathParam("lastName", " ").
        when().
                get("/{firstName}/{lastName}").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo(" "));
    }

    @Test public void
    can_specify_space_only_unnamed_path_params() {
        when().
                get("/{firstName}/{lastName}", "John", " ").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo(" "));
    }

    @Test public void
    can_specify_empty_named_path_params() {
        given().
                pathParam("firstName", "John").
                pathParam("lastName", "").
        when().
                get("/{firstName}/{lastName}").
        then().
                statusCode(404); // A resource matching only {firstName} is not defined
    }

    @Test public void
    can_specify_empty_unnamed_path_params() {
        when().
                get("/{firstName}/{lastName}", "John", "").
        then().
                statusCode(404); // A resource matching only {firstName} is not defined
    }

    @Test public void
    returns_nice_error_message_when_several_unnamed_path_parameter_are_be_null() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unnamed path parameter cannot be null (path parameters at indices 0,2 are null)");

        get("/{firstName}/{middleName}", null, "something", null);
    }

    @Test public void
    returns_nice_error_message_when_single_unnamed_path_parameter_is_null() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unnamed path parameter cannot be null (path parameter at index 0 is null");

        get("/{firstName}/{middleName}", (Object) null);
    }

    @Test public void
    can_use_path_parameters_value_shorter_than_the_template_name_when_using_multiple_templates_in_a_subresource() {
        when().
                get("/matrix;{abcde}={value}", "John", "Doe").
        then().
                statusCode(200).
                body("John", equalTo("Doe"));
    }

    @Test public void
    can_use_path_parameters_value_longer_than_the_template_name_when_using_multiple_templates_in_a_subresource() {
        when().
                get("/matrix;{abcde}={value}", "JohnJohn", "Doe").
        then().
                statusCode(200).
                body("JohnJohn", equalTo("Doe"));
    }
}
