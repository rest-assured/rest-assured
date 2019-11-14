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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.libraries.JUnitBundlesOption;

import java.util.UUID;

import static io.restassured.path.xml.XmlPath.from;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * This test aims to prove that xml-path is available as a valid OSGi bundle.
 */
@RunWith(PaxExam.class)
public class XmlPathOSGiITest {

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

                        wrappedBundle(mavenBundle().groupId("org.ccil.cowan.tagsoup").artifactId("tagsoup").versionAsInProject()),
                        wrappedBundle(mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api").versionAsInProject()),
                        wrappedBundle(mavenBundle("javax.activation", "activation").version("1.1.1")),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpclient").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpmime").versionAsInProject()),
                        wrappedBundle(mavenBundle("org.apache.httpcomponents", "httpcore").versionAsInProject()),

                        /* Rest Assured dependencies needed in the Pax Exam container to be able to execute the tests below */
                        mavenBundle("io.rest-assured", "json-path").versionAsInProject(),
                        mavenBundle("io.rest-assured", "xml-path").versionAsInProject(),
                        mavenBundle("io.rest-assured", "rest-assured").versionAsInProject(),
                        mavenBundle("io.rest-assured", "rest-assured-common").versionAsInProject()
                };
    }

    @Test
    public void getUUIDParsesAStringResultToUUID() {
        final String UUID_XML = "<some>\n" +
                "  <thing id=\"1\">db24eeeb-7fe5-41d3-8f06-986b793ecc91</thing>\n" +
                "  <thing id=\"2\">d69ded28-d75c-460f-9cbe-1412c60ed4cc</thing>\n" +
                "</some>";

        final UUID uuid = from(UUID_XML).getUUID("some.thing[0]");

        assertThat(uuid, Matchers.equalTo(UUID.fromString("db24eeeb-7fe5-41d3-8f06-986b793ecc91")));
    }
}
