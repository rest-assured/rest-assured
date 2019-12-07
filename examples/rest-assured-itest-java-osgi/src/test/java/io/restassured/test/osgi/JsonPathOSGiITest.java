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

import io.restassured.path.json.JsonPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
/**
 * This test aims to prove that json-path is available as a valid OSGi bundle.
 */
public class JsonPathOSGiITest {

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
                        mavenBundle().groupId("org.apache.aries.spifly").artifactId("org.apache.aries.spifly.dynamic.bundle").version("1.2.1"),
                        mavenBundle().groupId("org.hamcrest").artifactId("hamcrest").versionAsInProject(),
                        mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                        mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-json").versionAsInProject().noStart(),
                        mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-xml").versionAsInProject().noStart(),
                        mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy").versionAsInProject(),

                        wrappedBundle(mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api").versionAsInProject()),
                        wrappedBundle(mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient").versionAsInProject()),
                        wrappedBundle(mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpmime").versionAsInProject()),
                        wrappedBundle(mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore").versionAsInProject()),
                        wrappedBundle(mavenBundle().groupId("org.ccil.cowan.tagsoup").artifactId("tagsoup").versionAsInProject()),

                        /* Rest Assured dependencies needed in the Pax Exam container to be able to execute the tests below */
                        mavenBundle().groupId("io.rest-assured").artifactId("json-path").versionAsInProject(),
                        mavenBundle().groupId("io.rest-assured").artifactId("xml-path").versionAsInProject(),
                        mavenBundle().groupId("io.rest-assured").artifactId("rest-assured").versionAsInProject(),
                        mavenBundle().groupId("io.rest-assured").artifactId("rest-assured-common").versionAsInProject()
                };
    }

    @Test
    public void jsonPath() {
        final String JSON = "{\n" +
                "\"lotto\":{\n" +
                " \"lottoId\":5,\n" +
                "}\n" +
                "}";
        JsonPath jsonPath = new JsonPath(JSON).setRootPath("lotto");

        assertThat(jsonPath.getInt("lottoId"), equalTo(5));
    }
}
