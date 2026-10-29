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

package io.restassured.test.osgi;

import io.restassured.RestAssuredClient;
import io.restassured.http.jdk.JdkHttpClientProvider;
import io.restassured.internal.http.client.HttpClientProviders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.net.UnknownHostException;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.spiflyBundles;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
/**
 * This test aims to prove that Rest Assured is available as a valid OSGi bundle.
 */
public class RestAssuredOSGiITest {

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

                        /* Rest Assured dependencies needed in the Pax Exam container (the bundles built by this build) to be able to execute the test below */
                        restAssuredBundles()
                };
    }

    @Test(expected = UnknownHostException.class)
    public void restAssured() {
        given().
                when().
                get("http://dummy").
                then().
                statusCode(200);
    }

    @Test(expected = UnknownHostException.class)
    public void restAssuredClient() {
        try (RestAssuredClient client = RestAssuredClient.builder().build()) {
            client.get("http://dummy").then().statusCode(200);
        }
    }

    @Test
    public void the_jdk_http_client_is_the_default_without_the_apache_http_client_5_module() {
        HttpClientProviders.Selection selection = HttpClientProviders.select(config().getHttpClientConfig());

        assertEquals(JdkHttpClientProvider.class, selection.provider().getClass());
        assertEquals(HttpClientProviders.How.DEFAULT, selection.how());
    }
}
