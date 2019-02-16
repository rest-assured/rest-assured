package io.restassured.test.osgi;

/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.net.UnknownHostException;

import static io.restassured.RestAssured.given;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
/**
 * This test aims to prove that Rest Assured is available as a valid OSGi bundle.
 */
public class RestAssuredOSGiITest {

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[]
                {
                        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.hamcrest", "1.3_1"),
                        junitBundles(),
                        systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                        /* Transitive dependencies needed in the Pax Exam container.
                        Some of these need to be wrapped because they are not available as OSGi bundles */
                        mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                        wrappedBundle(mavenBundle().groupId("org.ccil.cowan.tagsoup").artifactId("tagsoup").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpclient").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpmime").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpcore").versionAsInProject()),
                        wrappedBundle(mavenBundle("javax.xml.bind", "jaxb-api").versionAsInProject()),
                        wrappedBundle(mavenBundle("javax.activation", "activation").version("1.1.1")),
                        wrappedBundle(mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-all").version("2.4.15")),

                        /* Rest Assured dependencie needed in the Pax Exam container to be able to execute the test below */
                        mavenBundle("io.rest-assured", "json-path").versionAsInProject(),
                        mavenBundle("io.rest-assured", "xml-path").versionAsInProject(),
                        mavenBundle("io.rest-assured", "rest-assured").versionAsInProject(),
                        mavenBundle("io.rest-assured", "rest-assured-common").versionAsInProject()
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
}
