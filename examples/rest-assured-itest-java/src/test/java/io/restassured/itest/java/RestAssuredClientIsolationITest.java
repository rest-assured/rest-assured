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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.RestAssuredClient;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.authentication.FormAuthConfig.formAuthConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The settings of a {@link RestAssuredClient}, and the client bound to a thread with <code>useClient(..)</code>, don't
 * reach the requests of other clients, other threads or the static API, when they all send at once (#534).
 */
public class RestAssuredClientIsolationITest extends WithJetty {
    private static final int THREADS = 8;
    private static final int REQUESTS = 400;
    private static final Filter FAILING = (request, response, context) -> {
        throw new AssertionError("A static filter ran");
    };

    @Test
    void the_tls_settings_of_a_client_dont_reach_another_client() throws Exception {
        RestAssuredClient relaxed = RestAssuredClient.builder().baseUri("https://localhost").port(8443).relaxedHTTPSValidation().build();
        RestAssuredClient strict = RestAssuredClient.builder().baseUri("https://localhost").port(8443).build();

        assertEachRequest(i -> {
            if (i % 2 == 0) {
                String hello = given(relaxed).get("/hello").path("hello");
                return "Hello Scalatra".equals(hello) ? null : "got " + hello;
            }
            try {
                given(strict).get("/hello");
                return "the strict client got a response";
            } catch (Exception e) {
                // The server's certificate isn't trusted
                return e instanceof SSLHandshakeException && e.getMessage().contains("PKIX path building failed") ? null : "got " + e;
            }
        });
    }

    @Test
    void clients_with_different_configs_and_the_static_api_each_keep_their_own_cookies() throws Exception {
        RestAssured.config = keepingCookies();
        RestAssuredClient one = RestAssuredClient.builder().config(keepingCookies()).build();
        RestAssuredClient two = RestAssuredClient.builder().config(keepingCookies()).build();
        List<Supplier<RequestSpecification>> senders = Arrays.asList(() -> given(one), () -> given(two), RestAssured::given);
        String[] cookies = {"one", "two", "static"};

        assertEachRequest(i -> {
            Supplier<RequestSpecification> sender = senders.get(i % 3);
            // The server sets the cookies of the request
            sender.get().cookie(cookies[i % 3], "1").get("/reflect");

            List<String> kept = sender.get().get("/cookiesWithValues").path("name");
            return kept.equals(List.of(cookies[i % 3])) ? null : "got " + kept;
        });
    }

    @Test
    void clients_built_from_the_same_config_share_the_kept_cookies() throws Exception {
        RestAssuredConfig config = keepingCookies();
        RestAssuredClient one = RestAssuredClient.builder().config(config).build();
        RestAssuredClient two = RestAssuredClient.builder().config(config).build();
        given(one).cookie("one", "1").get("/reflect");
        given(two).cookie("two", "1").get("/reflect");

        assertEachRequest(i -> {
            List<String> kept = given(i % 2 == 0 ? one : two).get("/cookiesWithValues").path("name");
            return Set.copyOf(kept).equals(Set.of("one", "two")) && kept.size() == 2 ? null : "got " + kept;
        });
    }

    @Test
    void form_authentication_and_csrf_send_their_extra_requests_with_the_client_from_many_threads() throws Exception {
        RestAssuredClient client = RestAssuredClient.builder().baseUri("http://127.0.0.1").port(8080).build();
        // A closed port, and a filter that fails the requests sent with the static settings
        RestAssured.port = 1;
        RestAssured.filters(FAILING);

        assertEachRequest(i -> {
            String body;
            if (i % 3 == 0) {
                body = given(client).auth().form("John", "Doe", formAuthConfig()).get("/formAuth").asString();
            } else if (i % 3 == 1) {
                body = given(client).csrf("/formAuthCsrf").auth().form("John", "Doe", formAuthConfig()).get("/formAuthCsrf").asString();
            } else {
                int statusCode = given(client).csrf("/loginPageWithCsrf").post("/loginPageWithCsrf").statusCode();
                return statusCode == 200 ? null : "got status code " + statusCode;
            }
            return body.equals("OK") ? null : "got " + body;
        });
    }

    @Test
    void each_thread_sends_with_its_bound_client_unless_it_names_another_one_or_resets_the_binding() throws Exception {
        RestAssured.requestSpecification = tagged("static");
        RestAssuredClient named = client("named");
        RestAssuredClient[] bound = boundClients();

        onThreadsWithTheirOwnClient(bound, (thread, failures) -> {
            for (int i = 0; i < REQUESTS / THREADS; i++) {
                String expected;
                String url;
                if (i % 3 == 0) {
                    expected = "thread" + thread;
                    url = get("/requestUrl").asString();
                } else if (i % 3 == 1) {
                    expected = "named";
                    url = given(named).get("/requestUrl").asString();
                } else {
                    expected = "static";
                    RestAssured.resetClient();
                    url = get("/requestUrl").asString();
                    RestAssured.useClient(bound[thread]);
                }
                if (!url.equals(requestUrl(expected))) {
                    failures.add(thread + ", " + i + ": expected the client " + expected + " but got " + url);
                }
            }
        });
    }

    @Test
    void a_thread_created_by_a_thread_with_a_bound_client_uses_the_static_settings() throws Exception {
        RestAssured.requestSpecification = tagged("static");

        onThreadsWithTheirOwnClient(boundClients(), (thread, failures) -> {
            // Like the ForkJoinPool that JUnit runs tests in parallel on, whose threads are created by threads that run tests
            ForkJoinPool pool = new ForkJoinPool(2);
            try {
                List<Future<String>> urls = new ArrayList<>();
                for (int i = 0; i < REQUESTS / THREADS; i++) {
                    urls.add(pool.submit(() -> get("/requestUrl").asString()));
                }
                for (Future<String> url : urls) {
                    if (!url.get(60, TimeUnit.SECONDS).equals(requestUrl("static"))) {
                        failures.add(thread + ": a thread of the pool got " + url.get());
                    }
                }
            } finally {
                pool.shutdownNow();
            }
            String url = get("/requestUrl").asString();
            if (!url.equals(requestUrl("thread" + thread))) {
                failures.add(thread + ": the thread got " + url);
            }
        });
    }

    private static RestAssuredConfig keepingCookies() {
        return config().httpClient(httpClientConfig().reuseHttpClientInstance().keepResponseCookies(true));
    }

    private static RestAssuredClient[] boundClients() {
        RestAssuredClient[] clients = new RestAssuredClient[THREADS];
        for (int thread = 0; thread < THREADS; thread++) {
            clients[thread] = client("thread" + thread);
        }
        return clients;
    }

    // A client whose requests say which client sent them
    private static RestAssuredClient client(String name) {
        return RestAssuredClient.builder().requestSpecification(tagged(name)).build();
    }

    private static RequestSpecification tagged(String name) {
        return new RequestSpecBuilder().addQueryParam("client", name).build();
    }

    private static String requestUrl(String client) {
        return "http://localhost:8080/requestUrl?client=" + client;
    }

    private interface ThreadWork {
        void run(int thread, List<String> failures) throws Exception;
    }

    // Runs the work on many threads at once, each with its own client bound. The work adds what went wrong to the failures.
    private static void onThreadsWithTheirOwnClient(RestAssuredClient[] clients, ThreadWork work) throws Exception {
        List<String> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                int thread = t;
                futures.add(executor.submit(() -> {
                    RestAssured.useClient(clients[thread]);
                    try {
                        work.run(thread, failures);
                    } catch (Throwable e) {
                        failures.add(thread + ": " + e);
                    } finally {
                        RestAssured.resetClient();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(failures).isEmpty();
    }

    // Sends the requests from many threads. The function returns null, or what went wrong.
    private static void assertEachRequest(IntFunction<String> sendAndCheck) throws Exception {
        List<String> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                int request = i;
                futures.add(executor.submit(() -> {
                    try {
                        String failure = sendAndCheck.apply(request);
                        if (failure != null) {
                            failures.add(request + ": " + failure);
                        }
                    } catch (Throwable e) {
                        failures.add(request + ": " + e);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(failures).isEmpty();
    }
}
