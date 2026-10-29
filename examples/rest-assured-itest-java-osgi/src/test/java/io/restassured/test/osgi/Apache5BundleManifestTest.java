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

import io.restassured.http.apache5.Apache5HttpClientProvider;
import org.junit.Test;

import java.util.List;

import static io.restassured.test.osgi.RestAssuredBundleManifestTest.clauses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The OSGi headers of the http-client-apache5 bundle. HttpClient 5's jars aren't bundles and carry no package versions,
 * so whatever wraps them gives their packages no version, and the bundle imports them without a range. The bundle uses
 * no SLF4J itself.
 */
public class Apache5BundleManifestTest {

    @Test
    public void only_the_package_of_the_provider_is_exported() throws Exception {
        assertThat(clauses(header("Export-Package"))).hasSize(1).allMatch(clause -> clause.startsWith("io.restassured.http.apache5;"));
    }

    @Test
    public void the_provider_is_a_service_loader_capability() throws Exception {
        assertThat(clauses(header("Provide-Capability")))
                .containsExactly("osgi.serviceloader;osgi.serviceloader=\"io.restassured.common.spi.http.HttpClientProvider\"");
    }

    @Test
    public void http_client_5_is_imported_without_a_version_range_and_slf4j_isnt_imported() throws Exception {
        List<String> imports = clauses(header("Import-Package"));

        assertThat(imports).filteredOn(clause -> clause.startsWith("org.apache.hc."))
                .contains("org.apache.hc.client5.http.impl.classic", "org.apache.hc.core5.http", "org.apache.hc.core5.http2")
                .allSatisfy(clause -> assertThat(clause).doesNotContain(";"));
        assertThat(imports).noneMatch(clause -> clause.startsWith("org.slf4j"));
    }

    private static String header(String name) throws Exception {
        return RestAssuredBundleManifestTest.header(Apache5HttpClientProvider.class, name);
    }
}
