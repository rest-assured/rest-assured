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

package io.restassured.internal.http.client;

import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.common.spi.http.HttpClientSpi;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.HttpClientType;
import io.restassured.internal.http.client.HttpClientProviders.BuiltIn;
import io.restassured.internal.http.client.HttpClientProviders.How;
import io.restassured.internal.http.client.HttpClientProviders.Selection;
import io.restassured.internal.support.VersionFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(10)
class HttpClientProvidersTest {
    private static final BuiltIn JDK = new BuiltIn(HttpClientType.JDK, FakeHttpClients.Jdk.class.getName(), "http-client-jdk");
    private static final BuiltIn APACHE5 = new BuiltIn(HttpClientType.APACHE5, FakeHttpClients.Apache5.class.getName(), "http-client-apache5");
    private static final BuiltIn NO_JDK = new BuiltIn(HttpClientType.JDK, "io.restassured.http.jdk.NotOnTheClasspath", "http-client-jdk");
    private static final BuiltIn NO_APACHE5 = new BuiltIn(HttpClientType.APACHE5, "io.restassured.http.apache5.NotOnTheClasspath", "http-client-apache5");
    private static final String BROKEN = "com.example.BrokenHttpClientProvider";
    private static final String PIN = " To pin a client, use httpClientConfig().client(..) or the system property restassured.http.client.";

    @TempDir
    Path directory;

    private final Map<String, String> systemProperties = new HashMap<>();
    private final List<String> logged = new ArrayList<>();
    private final Set<String> hiddenClasses = ConcurrentHashMap.newKeySet();
    private Exception failingServicesFiles;

    @BeforeEach
    void restAssured() {
        write("META-INF/rest-assured/rest-assured.properties", "version=7.0.1\nhttp-spi-revision=" + HttpClientSpi.REVISION + "\n");
    }

    // The order

    @Test
    void without_a_choice_http_client_4_is_used_and_a_debug_message_says_so() {
        Selection selection = providers(JDK, NO_APACHE5).selectFor(httpClientConfig());

        assertThat(selection.isHttpClient4()).isTrue();
        assertThat(selection.how()).isEqualTo(How.DEFAULT);
        assertThat(logged).containsExactly("DEBUG REST Assured uses Apache HttpClient 4.");
    }

    @Test
    void without_a_choice_http_client_apache5_is_used_when_it_is_on_the_classpath() {
        Selection selection = providers(JDK, APACHE5).selectFor(httpClientConfig());

        assertThat(selection.provider()).isInstanceOf(FakeHttpClients.Apache5.class);
        assertThat(selection.how()).isEqualTo(How.CLASSPATH);
        assertThat(logged).containsExactly("INFO REST Assured uses the HTTP client from io.rest-assured:http-client-apache5 (Apache HttpClient 5.5.1), "
                + "found on the classpath." + PIN);
    }

    @Test
    void the_message_names_the_clients_of_other_libraries_that_are_found_but_not_used() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION);
        thirdParty(FakeHttpClients.Other.class, "com.example:ra-other", HttpClientSpi.REVISION);

        providers(JDK, APACHE5).selectFor(httpClientConfig());

        assertThat(logged).containsExactly("INFO REST Assured uses the HTTP client from io.rest-assured:http-client-apache5 (Apache HttpClient 5.5.1), "
                + "found on the classpath. Also found, not used: okhttp (com.example:ra-okhttp), other (com.example:ra-other)." + PIN);
    }

    @Test
    void a_client_of_another_library_is_used_only_when_it_is_chosen() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION);
        HttpClientProviders providers = providers(JDK, NO_APACHE5);

        assertThat(providers.selectFor(httpClientConfig()).isHttpClient4()).isTrue();
        assertThat(providers.selectFor(httpClientConfig().client("okhttp")).provider()).isInstanceOf(FakeHttpClients.OkHttp.class);
        assertThat(logged).containsExactly("DEBUG REST Assured uses Apache HttpClient 4.",
                "INFO REST Assured uses the HTTP client from com.example:ra-okhttp, chosen with httpClientConfig().client(\"okhttp\").");
    }

    @Test
    void the_config_wins_over_the_system_property_which_wins_over_the_classpath() {
        HttpClientProviders providers = providers(JDK, APACHE5);
        systemProperties.put("restassured.http.client", "jdk");

        Selection byConfig = providers.selectFor(httpClientConfig().client(HttpClientType.APACHE5));
        Selection byProperty = providers.selectFor(httpClientConfig());

        assertThat(byConfig.provider()).isInstanceOf(FakeHttpClients.Apache5.class);
        assertThat(byConfig.chosenBy()).isEqualTo("httpClientConfig().client(APACHE5)");
        assertThat(byProperty.provider()).isInstanceOf(FakeHttpClients.Jdk.class);
        assertThat(byProperty.how()).isEqualTo(How.SYSTEM_PROPERTY);
        assertThat(logged).containsExactly(
                "INFO REST Assured uses the HTTP client from io.rest-assured:http-client-apache5 (Apache HttpClient 5.5.1), chosen with httpClientConfig().client(APACHE5).",
                "INFO REST Assured uses the HTTP client from io.rest-assured:http-client-jdk (Java 17.0.20), chosen with the system property restassured.http.client=jdk.");
    }

    @Test
    void the_system_property_is_read_for_each_request() {
        HttpClientProviders providers = providers(JDK, APACHE5);

        systemProperties.put("restassured.http.client", " JDK ");
        assertThat(providers.selectFor(httpClientConfig()).provider()).isInstanceOf(FakeHttpClients.Jdk.class);
        systemProperties.put("restassured.http.client", "apache5");
        assertThat(providers.selectFor(httpClientConfig()).provider()).isInstanceOf(FakeHttpClients.Apache5.class);
        systemProperties.remove("restassured.http.client");
        assertThat(providers.selectFor(httpClientConfig()).how()).isEqualTo(How.CLASSPATH);
    }

    @Test
    void a_client_is_chosen_by_type_name_or_class() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION);
        HttpClientProviders providers = providers(JDK, NO_APACHE5);

        assertThat(providers.selectFor(httpClientConfig().client(HttpClientType.JDK)).provider()).isInstanceOf(FakeHttpClients.Jdk.class);
        assertThat(providers.selectFor(httpClientConfig().client("Jdk")).provider()).isInstanceOf(FakeHttpClients.Jdk.class);
        assertThat(providers.selectFor(httpClientConfig().client("okhttp")).provider()).isInstanceOf(FakeHttpClients.OkHttp.class);
        Selection byClass = providers.selectFor(httpClientConfig().client(FakeHttpClients.Other.class));
        assertThat(byClass.provider()).isInstanceOf(FakeHttpClients.Other.class);
        assertThat(byClass.chosenBy()).isEqualTo("httpClientConfig().client(" + FakeHttpClients.Other.class.getName() + ".class)");
    }

    @Test
    void the_choice_is_made_once_and_the_message_is_logged_once_for_each_client() {
        HttpClientProviders providers = providers(JDK, APACHE5);
        HttpClientConfig config = httpClientConfig().client(HttpClientType.JDK);

        Selection first = providers.selectFor(config);
        Selection second = providers.selectFor(httpClientConfig().client(HttpClientType.JDK));
        providers.selectFor(httpClientConfig().client("jdk"));

        assertThat(second).isSameAs(first);
        assertThat(logged).hasSize(1);
    }

    // Failures

    @Test
    void a_chosen_client_module_that_isnt_on_the_classpath_fails() {
        HttpClientProviders providers = providers(NO_JDK, NO_APACHE5);

        assertThatThrownBy(() -> providers.selectFor(httpClientConfig().client(HttpClientType.APACHE5)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("httpClientConfig().client(APACHE5) needs io.rest-assured:http-client-apache5 on the classpath.");
        systemProperties.put("restassured.http.client", "jdk");
        assertThatThrownBy(() -> providers.selectFor(httpClientConfig()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("The system property restassured.http.client=jdk needs io.rest-assured:http-client-jdk on the classpath.");
    }

    @Test
    void an_unknown_name_fails_with_the_names_of_the_clients() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION);

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client("netty")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("httpClientConfig().client(\"netty\") names no HTTP client on the classpath. REST Assured's clients are jdk and apache5, "
                        + "and the other clients on the classpath are okhttp.");
    }

    @Test
    void the_name_of_a_client_of_another_library_is_matched_ignoring_case() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION);

        assertThat(providers(JDK, APACHE5).selectFor(httpClientConfig().client("OkHttp")).provider()).isInstanceOf(FakeHttpClients.OkHttp.class);
    }

    @Test
    void a_provider_whose_class_fails_to_load_is_a_problem_that_doesnt_stop_the_others() {
        thirdParty(BROKEN, "com.example:ra-broken", HttpClientSpi.REVISION);
        thirdParty(FakeHttpClients.Faulty.class.getName(), "com.example:ra-faulty", HttpClientSpi.REVISION);
        thirdParty(FakeHttpClients.Other.class.getName(), "com.example:ra-other", HttpClientSpi.REVISION);
        HttpClientProviders providers = providers(JDK, APACHE5);

        assertThat(providers.selectFor(httpClientConfig().client("other")).provider()).isInstanceOf(FakeHttpClients.Other.class);
        assertThatThrownBy(() -> providers.selectFor(httpClientConfig().client("broken")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("httpClientConfig().client(\"broken\") names no HTTP client on the classpath. REST Assured's clients are jdk and apache5, "
                        + "and the other clients on the classpath are other. "
                        + "REST Assured can't load an HTTP client provider: java.lang.NoClassDefFoundError: com/example/MissingLibrary. "
                        + "REST Assured can't load an HTTP client provider: java.lang.IllegalStateException: No name.");
    }

    @Test
    void a_class_loader_that_cant_list_the_services_files_doesnt_hang_the_search() {
        failingServicesFiles = new IOException("Can't list");

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client("other")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("and no other client is on the classpath. " + HttpClientProvider.class.getName() + ": Error locating configuration files");
    }

    @Test
    void the_message_of_the_client_in_use_doesnt_fail_the_request_when_the_other_clients_cant_be_found() {
        failingServicesFiles = new IllegalStateException("Can't list");

        assertThat(providers(JDK, APACHE5).selectFor(httpClientConfig()).provider()).isInstanceOf(FakeHttpClients.Apache5.class);
        assertThat(logged).containsExactly("INFO REST Assured uses the HTTP client from io.rest-assured:http-client-apache5 (Apache HttpClient 5.5.1), "
                + "found on the classpath." + PIN);
    }

    @Test
    void a_client_chosen_by_class_is_checked_for_the_revision_of_the_http_spi_outside_osgi() {
        // The test class path has no META-INF/rest-assured/http-client file for OkHttp
        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client(FakeHttpClients.OkHttp.class)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("The HTTP client provider " + FakeHttpClients.OkHttp.class.getName() + " doesn't say which HTTP SPI revision");
        assertThat(osgiProviders(JDK, APACHE5).selectFor(httpClientConfig().client(FakeHttpClients.OkHttp.class)).provider())
                .isInstanceOf(FakeHttpClients.OkHttp.class);
    }

    @Test
    void a_client_module_chosen_by_class_is_checked_for_its_version() {
        write("META-INF/rest-assured/http-client-jdk.properties", "version=7.1.0\nhttp-spi-revision=" + HttpClientSpi.REVISION + "\n");

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client(FakeHttpClients.Jdk.class)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("io.rest-assured:http-client-jdk 7.1.0 doesn't match REST Assured 7.0.1.");
    }

    @Test
    void in_osgi_a_client_module_that_is_installed_later_is_found() {
        hiddenClasses.add(FakeHttpClients.Apache5.class.getName());
        HttpClientProviders providers = osgiProviders(JDK, APACHE5);
        assertThat(providers.selectFor(httpClientConfig()).isHttpClient4()).isTrue();

        hiddenClasses.clear();

        assertThat(providers.selectFor(httpClientConfig()).provider()).isInstanceOf(FakeHttpClients.Apache5.class);
    }

    @Test
    void outside_osgi_the_absence_of_a_client_module_is_kept() {
        hiddenClasses.add(FakeHttpClients.Apache5.class.getName());
        HttpClientProviders providers = providers(JDK, APACHE5);
        assertThat(providers.selectFor(httpClientConfig()).isHttpClient4()).isTrue();

        hiddenClasses.clear();

        assertThat(providers.selectFor(httpClientConfig()).isHttpClient4()).isTrue();
    }

    @Test
    void a_client_module_of_another_minor_version_fails() {
        write("META-INF/rest-assured/http-client-jdk.properties", "version=7.1.0\nhttp-spi-revision=" + HttpClientSpi.REVISION + "\n");

        assertThatThrownBy(() -> providers(JDK, NO_APACHE5).selectFor(httpClientConfig().client(HttpClientType.JDK)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("io.rest-assured:http-client-jdk 7.1.0 doesn't match REST Assured 7.0.1. Use one version for all REST Assured artifacts, "
                        + "for example by importing io.rest-assured:rest-assured-bom.");
    }

    @Test
    void a_client_module_of_another_patch_version_is_used() {
        write("META-INF/rest-assured/http-client-apache5.properties", "version=7.0.2\nhttp-spi-revision=" + HttpClientSpi.REVISION + "\n");

        assertThat(providers(JDK, APACHE5).selectFor(httpClientConfig()).provider()).isInstanceOf(FakeHttpClients.Apache5.class);
    }

    @Test
    void a_client_module_built_for_another_revision_of_the_http_spi_fails() {
        write("META-INF/rest-assured/http-client-apache5.properties", "version=7.0.2\nhttp-spi-revision=" + (HttpClientSpi.REVISION + 1) + "\n");

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("io.rest-assured:http-client-apache5 7.0.2 doesn't match REST Assured 7.0.1.");
    }

    @Test
    void a_client_of_another_library_built_for_an_older_revision_of_the_http_spi_fails() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION - 1);

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client("okhttp")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("httpClientConfig().client(\"okhttp\") names no HTTP client on the classpath. REST Assured's clients are jdk and apache5, "
                        + "and no other client is on the classpath. "
                        + FakeHttpClients.OkHttp.class.getName() + " (com.example:ra-okhttp 1.2.0) was built for REST Assured's HTTP SPI revision "
                        + (HttpClientSpi.REVISION - 1) + ", but this REST Assured has revision " + HttpClientSpi.REVISION + ". Use a newer version of it.");
    }

    @Test
    void a_client_of_another_library_built_for_a_newer_revision_of_the_http_spi_fails() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION + 1);

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client("okhttp")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("Use a newer version of REST Assured, or an older version of it.");
    }

    @Test
    void a_client_of_another_library_that_doesnt_say_which_revision_it_was_built_for_fails() {
        write("META-INF/services/" + HttpClientProvider.class.getName(), FakeHttpClients.OkHttp.class.getName() + "\n");

        assertThatThrownBy(() -> providers(JDK, APACHE5).selectFor(httpClientConfig().client("okhttp")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith(" The HTTP client provider " + FakeHttpClients.OkHttp.class.getName() + " doesn't say which HTTP SPI revision it was built for, "
                        + "so REST Assured can't use it. Its jar needs META-INF/rest-assured/http-client/" + FakeHttpClients.OkHttp.class.getName()
                        + ".properties with the property http-spi-revision.");
    }

    @Test
    void a_rejected_client_of_another_library_doesnt_stop_others_from_being_used_or_listed() {
        thirdParty(FakeHttpClients.OkHttp.class, "com.example:ra-okhttp", HttpClientSpi.REVISION + 1);
        thirdParty(FakeHttpClients.Other.class, "com.example:ra-other", HttpClientSpi.REVISION);
        HttpClientProviders providers = providers(JDK, APACHE5);

        assertThat(providers.selectFor(httpClientConfig().client("other")).provider()).isInstanceOf(FakeHttpClients.Other.class);
        providers.selectFor(httpClientConfig());

        assertThat(logged).last().asString().contains("Also found, not used: other (com.example:ra-other).");
    }

    @Test
    void a_client_of_another_library_with_the_name_of_one_of_rest_assureds_is_ignored_with_a_warning() {
        thirdParty(FakeHttpClients.Impostor.class, "com.example:ra-impostor", HttpClientSpi.REVISION);
        HttpClientProviders providers = providers(NO_JDK, APACHE5);

        assertThatThrownBy(() -> providers.selectFor(httpClientConfig().client("jdk"))).hasMessageContaining("needs io.rest-assured:http-client-jdk");
        providers.selectFor(httpClientConfig());

        assertThat(logged).contains("WARNING " + FakeHttpClients.Impostor.class.getName() + " (com.example:ra-impostor) names its HTTP client jdk, "
                + "which is the name of one of REST Assured's clients, so REST Assured ignores it.");
    }

    @Test
    void the_revision_of_the_http_spi_is_the_one_in_the_version_file_of_rest_assured() {
        assertThat(VersionFiles.load(VersionFiles.class.getClassLoader(), "META-INF/rest-assured/rest-assured.properties").getProperty("http-spi-revision"))
                .isEqualTo(String.valueOf(HttpClientSpi.REVISION));
    }

    private HttpClientProviders providers(BuiltIn... builtIns) {
        return new HttpClientProviders(classLoader(), List.of(builtIns), systemProperties::get, (level, message) -> logged.add(level + " " + message));
    }

    private HttpClientProviders osgiProviders(BuiltIn... builtIns) {
        return new HttpClientProviders(classLoader(), List.of(builtIns), systemProperties::get, (level, message) -> logged.add(level + " " + message), true);
    }

    private void thirdParty(Class<? extends HttpClientProvider> providerClass, String artifact, int revision) {
        thirdParty(providerClass.getName(), artifact, revision);
    }

    private void thirdParty(String className, String artifact, int revision) {
        write("META-INF/services/" + HttpClientProvider.class.getName(), "# A comment\n" + className + " # another\n");
        write("META-INF/rest-assured/http-client/" + className + ".properties",
                "artifact=" + artifact + "\nversion=1.2.0\nhttp-spi-revision=" + revision + "\n");
    }

    // Classes from the test class path, except the hidden ones and BROKEN, and resources only from the directory
    private ClassLoader classLoader() {
        try {
            URLClassLoader resources = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null);
            return new ClassLoader(getClass().getClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (hiddenClasses.contains(name)) {
                        throw new ClassNotFoundException(name);
                    } else if (name.equals(BROKEN)) {
                        // As for a class whose superclass is in a library that isn't on the classpath
                        throw new NoClassDefFoundError("com/example/MissingLibrary");
                    }
                    return super.loadClass(name, resolve);
                }

                @Override
                public URL getResource(String name) {
                    return resources.getResource(name);
                }

                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    if (failingServicesFiles instanceof IOException && name.startsWith("META-INF/services/")) {
                        throw (IOException) failingServicesFiles;
                    } else if (failingServicesFiles instanceof RuntimeException && name.startsWith("META-INF/services/")) {
                        throw (RuntimeException) failingServicesFiles;
                    }
                    return resources.getResources(name);
                }
            };
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void write(String resource, String content) {
        try {
            Path file = directory.resolve(resource);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
