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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.TestClient;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.*;

public class HttpClientConfigITest extends WithJetty {

    @Test
    public void anHttpClient4ParameterThatRestAssuredDoesntUseAnyMoreFailsTheRequest() {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().setParam("http.default-headers", List.of("header1: value1")));
        try {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> given().param("url", "/hello").get("/multiHeaderReflect"));

            assertThat(thrown.getMessage(), startsWith("The Apache HttpClient 4 parameter \"http.default-headers\" of the HttpClientConfig "
                    + "isn't used any more"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void aCustomizerOfApacheHttpClient5GetsTheBuilder() {
        HttpClientConfig customized = customizeApache5(builder -> builder.addRequestInterceptorFirst((request, entity, context) -> request.addHeader("X-Intercepted", "yes")));
        RequestSpecification request = given().config(RestAssuredConfig.config().httpClient(customized));
        if (TestClient.isJdk()) {
            assertTheJdkClientRejectsTheCustomizer(() -> request.get("/multiHeaderReflect"));
            return;
        }

        request.get("/multiHeaderReflect").then().header("X-Intercepted", "yes");
    }

    @Test
    public void aCustomizerOfApacheHttpClient5CantTurnOnRedirectsBecauseRestAssuredFollowsThem() {
        HttpClientConfig customized = customizeApache5(builder -> builder.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE));
        RequestSpecification request = given().config(RestAssuredConfig.config().httpClient(customized).redirect(RedirectConfig.redirectConfig().followRedirects(false)))
                .param("url", "/hello");
        if (TestClient.isJdk()) {
            assertTheJdkClientRejectsTheCustomizer(() -> request.get("/redirect"));
            return;
        }

        request.get("/redirect").then().statusCode(302);
    }

    @Test
    public void aCustomizerOfApacheHttpClient5CantTurnOnRetries() throws IOException {
        HttpClientConfig customized = customizeApache5(builder -> builder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ofMilliseconds(1))));
        // Closes each connection without an answer
        AtomicInteger connections = new AtomicInteger();
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread thread = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket connection = server.accept()) {
                        connections.incrementAndGet();
                        connection.getInputStream().read(new byte[1024]);
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();

            Executable request = () -> given().config(RestAssuredConfig.config().httpClient(customized)).get("http://localhost:" + server.getLocalPort() + "/");
            if (TestClient.isJdk()) {
                assertTheJdkClientRejectsTheCustomizer(request);
                return;
            }

            assertThrows(IOException.class, request);
            assertThat(connections.get(), equalTo(1));
        }
    }

    private static HttpClientConfig customizeApache5(Consumer<HttpClientBuilder> customizer) {
        return HttpClientConfig.httpClientConfig().customizeClient(HttpClientBuilder.class, customizer);
    }

    // The JDK client fails a request with a customizer of Apache HttpClient 5, and says why
    private static void assertTheJdkClientRejectsTheCustomizer(Executable request) {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, request);
        assertThat(thrown.getMessage(), equalTo("A customizer for org.apache.hc.client5.http.impl.classic.HttpClientBuilder is configured, but REST Assured "
                + "uses the JDK HTTP client."));
    }

    @Test
    public void theRedirectParamsOfTheHttpClientConfigDontLimitRedirectsBecauseTheRedirectConfigHasPrecedence() throws Exception {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().setParam("http.protocol.handle-redirects", true).and().setParam("http.protocol.max-redirects", 0));
        try {
            given().
                    param("url", "/hello").
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

}
