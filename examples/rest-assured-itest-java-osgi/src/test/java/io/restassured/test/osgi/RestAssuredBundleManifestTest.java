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

import io.restassured.RestAssured;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The OSGi headers of the rest-assured bundle for the HTTP client modules, which are found by class name, and for the
 * HTTP clients of other libraries, which are found with ServiceLoader and SPI Fly. All of them are optional, so that
 * the bundle resolves without them (see {@link NoPathEngineOSGiITest}).
 */
public class RestAssuredBundleManifestTest {

    @Test
    public void the_http_client_modules_and_json_path_jayway_are_optional_imports_of_this_version_up_to_the_next_minor() throws Exception {
        List<String> imports = clauses(header("Import-Package"));
        // Such as [7.0,7.1) for 7.0.0.SNAPSHOT
        String[] version = header("Bundle-Version").split("\\.");
        String range = "version=\"[" + version[0] + "." + version[1] + "," + version[0] + "." + (Integer.parseInt(version[1]) + 1) + ")\"";

        assertThat(imports).anySatisfy(clause -> assertThat(clause).startsWith("io.restassured.http.jdk;").contains("resolution:=optional").contains(range));
        assertThat(imports).anySatisfy(clause -> assertThat(clause).startsWith("io.restassured.http.apache5;").contains("resolution:=optional").contains(range));
        assertThat(imports).anySatisfy(clause -> assertThat(clause).startsWith("io.restassured.internal.path.jayway;").contains("resolution:=optional").contains(range));
    }

    @Test
    public void the_http_client_modules_are_dynamic_imports_and_not_exported() throws Exception {
        assertThat(clauses(header("DynamicImport-Package"))).contains("io.restassured.http.jdk", "io.restassured.http.apache5");
        assertThat(clauses(header("Export-Package"))).noneMatch(clause -> clause.startsWith("io.restassured.http.jdk") || clause.startsWith("io.restassured.http.apache5"));
    }

    @Test
    public void the_service_loader_requirements_are_optional() throws Exception {
        assertThat(clauses(header("Require-Capability"))).contains(
                "osgi.extender;filter:=\"(osgi.extender=osgi.serviceloader.processor)\";resolution:=optional",
                "osgi.serviceloader;filter:=\"(osgi.serviceloader=io.restassured.common.spi.http.HttpClientProvider)\";cardinality:=multiple;resolution:=optional");
    }

    private static String header(String name) throws Exception {
        File jar = new File(RestAssured.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile jarFile = new JarFile(jar)) {
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            String value = attributes.getValue(name);
            assertThat(value).as(name + " of " + jar).isNotNull();
            return value;
        }
    }

    // The clauses of a header, which are separated by commas outside of quotes
    private static List<String> clauses(String header) {
        List<String> clauses = new ArrayList<>();
        StringBuilder clause = new StringBuilder();
        boolean quoted = false;
        for (char c : header.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            }
            if (c == ',' && !quoted) {
                clauses.add(clause.toString().trim());
                clause.setLength(0);
            } else {
                clause.append(c);
            }
        }
        clauses.add(clause.toString().trim());
        return clauses;
    }
}
