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
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.path.MissingPathEngine;
import io.restassured.response.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.net.UnknownHostException;

import static io.restassured.RestAssured.given;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredCoreBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * REST Assured without a path engine: no json-path-gpath, xml-path-gpath, Groovy, SPI Fly or TagSoup bundle. The
 * rest-assured bundle resolves, sends requests, and a path fails with the message that names the dependency to add.
 */
@RunWith(PaxExam.class)
public class NoPathEngineOSGiITest {

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
                        mavenBundle().groupId("org.hamcrest").artifactId("hamcrest").versionAsInProject(),
                        mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpclient").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpmime").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpcore").versionAsInProject()),

                        /* rest-assured and rest-assured-common, the bundles built by this build */
                        restAssuredCoreBundles()
                };
    }

    @Test(expected = UnknownHostException.class)
    public void sendsRequests() {
        given().
                when().
                get("http://dummy").
                then().
                statusCode(200);
    }

    @Test
    public void pathFailsWithTheDependencyToAdd() {
        Response response = new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody("{\"a\":{\"b\":\"c\"}}").build();

        try {
            response.path("a.b");
        } catch (IllegalStateException e) {
            assertEquals(MissingPathEngine.message("a.b", DocumentType.JSON), e.getMessage());
            return;
        }
        fail("Expected path(..) to fail without a path engine");
    }
}
