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


package io.restassured.test.osgi;

import io.restassured.builder.ResponseBuilder;
import io.restassured.path.jayway.JaywayPath;
import io.restassured.response.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.util.List;

import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.jaywayBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredCoreBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.spiflyBundles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Evaluates JSONPath with json-path-jayway in OSGi, without GPath, Groovy or TagSoup. SPI Fly is there for slf4j-api 2,
 * which Jayway uses. Core finds the Jayway
 * path provider by class name. A filter with a literal reaches json-smart, which Jayway's filter compiler uses. The probe
 * uses no Hamcrest classes itself, since it would see the Hamcrest 1.3 bundle that JUnit needs.
 */
@RunWith(PaxExam.class)
public class JaywayOSGiITest {
    private static final String STORE = "{\"store\":{\"book\":[{\"title\":\"t0\",\"tag\":\"a\"},{\"title\":\"t1\",\"tag\":\"b\"}]}}";

    @Configuration
    public static Option[] configure() {
        return new Option[]
                {
                        /* System Properties */
                        systemProperty(EXAM_FAIL_ON_UNRESOLVED_KEY).value("true"),
                        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                        /* Hamcrest & JUnit bundles */
                        restAssuredJunitBundles(),

                        /* Transitive dependencies needed in the Pax Exam container, without the optional ones */
                        spiflyBundles(),
                        mavenBundle().groupId("org.hamcrest").artifactId("hamcrest").versionAsInProject(),
                        mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpclient").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpmime").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpcore").versionAsInProject()),

                        /* rest-assured, rest-assured-common and json-path-jayway, the bundles built by this build, and Jayway */
                        restAssuredCoreBundles(),
                        jaywayBundles()
                };
    }

    private static Response response() {
        return new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody(STORE).build();
    }

    @Test
    public void paths() {
        Response response = response();

        assertEquals("t1", response.path("$.store.book[1].title"));
        assertEquals(List.of("t0", "t1"), response.path("store.book[*].title"));
        response.then().body("store.book[0].title", equalToPath("$.store.book[0].title"));
        try {
            response.then().body("store.book[0].title", equalToPath("store.book[1].title"));
            fail("Expected the body assertion to fail");
        } catch (AssertionError expected) {
            // The JUnit bundle in the container has no assertThrows
        }
    }

    @Test
    public void filtersWithLiteralsUseJsonSmart() {
        assertEquals(List.of("t0"), response().path("store.book[?(@.tag in ['a','x'])].title"));
    }

    @Test
    public void aGPathSpreadFailsWithTheGPathHint() {
        try {
            response().path("store.book.title");
            fail("Expected a GPath spread to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("JSONPath (Jayway) can't evaluate store.book.title: store.book is a list, so use store.book[*].title."));
            assertTrue(e.getMessage(), e.getMessage().contains("add io.rest-assured:json-path-gpath:"));
        }
    }

    @Test
    public void jaywayPath() {
        assertEquals("t0", JaywayPath.from(STORE).read("$.store.book[0].title"));
    }
}
