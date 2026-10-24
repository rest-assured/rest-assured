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
import io.restassured.response.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.spiflyBundles;
import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Evaluates paths in body assertions and with {@code path(..)} in OSGi, where core finds the path engines in the
 * json-path-gpath and xml-path-gpath bundles by class name. The probe uses no Hamcrest classes itself, since it would see the
 * Hamcrest 1.3 bundle that JUnit needs, which isn't the one REST Assured uses.
 */
@RunWith(PaxExam.class)
public class PathOSGiITest {

    @Configuration
    public static Option[] configure() {
        return new Option[]
                {
                        /* System Properties */
                        systemProperty(EXAM_FAIL_ON_UNRESOLVED_KEY).value("true"),
                        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                        /* Hamcrest & JUnit bundles */
                        restAssuredJunitBundles(),

                        /* Transitive dependencies needed in the Pax Exam container.
                        Some of these need to be wrapped because they are not available as OSGi bundles */
                        spiflyBundles(),
                        mavenBundle().groupId("org.hamcrest").artifactId("hamcrest").versionAsInProject(),
                        mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                        mavenBundle().groupId("org.apache.groovy").artifactId("groovy-json").versionAsInProject().noStart(),
                        mavenBundle().groupId("org.apache.groovy").artifactId("groovy-xml").versionAsInProject().noStart(),
                        mavenBundle().groupId("org.apache.groovy").artifactId("groovy").versionAsInProject(),

                        wrappedBundle(mavenBundle().groupId("org.ccil.cowan.tagsoup").artifactId("tagsoup").versionAsInProject()),
                        mavenBundle("javax.xml.bind", "jaxb-api").versionAsInProject(),
                        wrappedBundle(mavenBundle("javax.activation", "activation").version("1.1.1")),

                        /* Rest Assured dependencies needed in the Pax Exam container (the bundles built by this build) to be able to execute the tests below */
                        restAssuredBundles()
                };
    }

    private static Response response(String contentType, String body) {
        return new ResponseBuilder().setStatusCode(200).setContentType(contentType).setBody(body).build();
    }

    // The JUnit bundle in the container has no assertThrows
    private static void assertFailsToMatch(Response response) {
        try {
            response.then().rootPath("a").body("b", equalToPath("a"));
        } catch (AssertionError e) {
            return;
        }
        fail("Expected the body assertion to fail");
    }

    @Test
    public void jsonPaths() {
        Response response = response("application/json", "{\"a\":{\"b\":\"c\"}}");

        assertEquals("c", response.path("a.b"));
        response.then().rootPath("a").body("b", equalToPath("a.b"));
        assertFailsToMatch(response);
    }

    @Test
    public void xmlPaths() {
        Response response = response("application/xml", "<a><b>c</b></a>");

        assertEquals("c", response.path("a.b"));
        response.then().rootPath("a").body("b", equalToPath("a.b"));
        assertFailsToMatch(response);
    }
}
