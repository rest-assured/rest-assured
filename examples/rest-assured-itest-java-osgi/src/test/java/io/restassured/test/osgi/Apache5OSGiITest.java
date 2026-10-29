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
import io.restassured.internal.http.client.HttpClientProviders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.apache5Bundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredCoreBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.restAssuredJunitBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.slf4jBundles;
import static io.restassured.test.osgi.options.RestAssuredPaxExamOptions.spiflyBundles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * REST Assured with http-client-apache5 in OSGi: the module bundle, HttpClient 5, HttpCore 5 and httpcore5-h2 wrapped,
 * and SLF4J 2, which needs SPI Fly. Core finds the module by class name, and sends requests with it. The wrapped bundles
 * import org.slf4j, which Pax Exam's own pax-logging-api exports too (1.7), so they would also resolve here without the
 * SLF4J bundles.
 */
@RunWith(PaxExam.class)
public class Apache5OSGiITest {

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

                        /* rest-assured, rest-assured-common, http-client-jdk and http-client-apache5, the bundles built by this build */
                        restAssuredCoreBundles(),
                        apache5Bundles(),
                        slf4jBundles()
                };
    }

    @Test
    public void the_apache_http_client_5_module_is_found_on_the_classpath() {
        HttpClientProviders.Selection selection = HttpClientProviders.select(config().getHttpClientConfig());

        assertEquals(Apache5HttpClientProvider.class, selection.provider().getClass());
        assertEquals(HttpClientProviders.How.CLASSPATH, selection.how());
    }

    @Test
    public void the_version_of_http_client_5_is_read_from_its_bundle() {
        String description = new Apache5HttpClientProvider().libraryDescription();

        assertTrue(description, description.startsWith("Apache HttpClient 5."));
    }

    @Test
    public void sends_requests() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> respond(server, "hello"));

            String body = given().when().get("http://localhost:" + server.getLocalPort() + "/greeting").then().statusCode(200).extract().asString();

            String head = request.get(10, TimeUnit.SECONDS);
            assertEquals("hello", body);
            assertTrue(head, head.startsWith("GET /greeting HTTP/1.1\r\n"));
            assertTrue(head, head.contains("\r\nUser-Agent: Apache-HttpClient/"));
        }
    }

    // Answers one request with the body and closes the connection. Returns the request head.
    private static String respond(ServerSocket server, String body) {
        try (Socket socket = server.accept()) {
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            while (!head.toString(StandardCharsets.ISO_8859_1).endsWith("\r\n\r\n")) {
                int b = in.read();
                if (b == -1) {
                    break;
                }
                head.write(b);
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream out = socket.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Length: " + bytes.length
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(bytes);
            out.flush();
            return head.toString(StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
