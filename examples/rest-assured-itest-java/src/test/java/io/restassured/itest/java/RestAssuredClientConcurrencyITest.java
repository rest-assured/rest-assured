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
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RestAssuredClient}s used from many threads at once, named with <code>given(client)</code> or bound to the threads
 * with <code>useClient(..)</code>, with different base URIs, while the static fields change (#534).
 */
public class RestAssuredClientConcurrencyITest extends WithJetty {
    private static final int THREADS = 8;
    private static final int REQUESTS = 400;
    private static final Filter FAILING = (request, response, context) -> {
        throw new AssertionError("A static filter ran");
    };
    // Fails a request that has the header of the static request specification
    private static final Filter NO_STATIC_HEADER = (request, response, context) -> {
        if (request.getHeaders().hasHeaderWithName("X-Static")) {
            throw new AssertionError("The static request specification was used");
        }
        return context.next(request, response);
    };

    private final List<RestAssuredClient> clients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        clients.forEach(RestAssuredClient::close);
    }

    @Test
    void clients_with_different_base_uris_dont_see_each_other_or_the_static_fields() throws Throwable {
        RestAssuredClient[] api = {
                client(RestAssuredClient.builder().baseUri("http://localhost:8080")),
                client(RestAssuredClient.builder().baseUri("http://127.0.0.1:8080")),
                client(RestAssuredClient.builder().baseUri("http://localhost").port(8080).basePath("/"))};
        String[] expectedUrls = {"http://localhost:8080/requestUrl?n=", "http://127.0.0.1:8080/requestUrl?n=", "http://localhost:8080/requestUrl?n="};

        whileTheStaticFieldsChange(() -> assertEachRequest(i -> {
            String url = given(api[i % 3]).get("/requestUrl?n=" + i).asString();
            return url.equals(expectedUrls[i % 3] + i) ? null : "got " + url;
        }));
    }

    @Test
    void each_thread_can_bind_a_client_of_its_own_and_send_with_the_static_methods() throws Throwable {
        String[] baseUris = {"http://localhost:8080", "http://127.0.0.1:8080"};
        List<String> failures = new CopyOnWriteArrayList<>();

        whileTheStaticFieldsChange(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    String baseUri = baseUris[t % 2];
                    futures.add(executor.submit(() -> {
                        RestAssured.useClient(RestAssuredClient.builder().baseUri(baseUri).filters(NO_STATIC_HEADER));
                        try {
                            for (int i = 0; i < REQUESTS / THREADS; i++) {
                                String url = get("/requestUrl?n=" + i).asString();
                                if (!url.equals(baseUri + "/requestUrl?n=" + i)) {
                                    failures.add(baseUri + ", " + i + ": got " + url);
                                }
                            }
                        } catch (Throwable e) {
                            failures.add(baseUri + ": " + e);
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
        });

        assertThat(failures).isEmpty();
    }

    @Test
    void one_client_and_one_built_specification_can_send_to_different_base_uris_from_many_threads() throws Exception {
        RestAssuredClient api = client(RestAssuredClient.builder());
        RequestSpecification specification = new RequestSpecBuilder().addHeader("X-Spec", "1").addQueryParam("q", "1").build();

        assertEachRequest(i -> {
            String baseUri = i % 2 == 0 ? "http://localhost:8080" : "http://127.0.0.1:8080";
            String url = given(api).spec(specification).baseUri(baseUri).get("/requestUrl").asString();
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(given(api).spec(specification).baseUri(baseUri).get("/headersWithValues").jsonPath().getMap(""));
            if (!url.equals(baseUri + "/requestUrl?q=1")) {
                return "got " + url;
            }
            return headers.get("X-Spec").equals(List.of("1")) && headers.get("Host").equals(List.of(baseUri.substring("http://".length())))
                    ? null : "got headers " + headers;
        });
    }

    @Test
    void one_client_bound_to_many_threads_can_send_one_built_request_and_response_specification() throws Exception {
        RestAssuredClient api = client(RestAssuredClient.builder());
        RequestSpecification request = new RequestSpecBuilder().build();
        ResponseSpecification response = new ResponseSpecBuilder().expectStatusCode(200).build();

        assertEachRequest(i -> {
            RestAssured.useClient(api);
            try {
                Response sent = given(request, response).get("/{firstName}/{lastName}", "first" + i, "last" + i);
                String fullName = sent.path("fullName");
                return ("first" + i + " last" + i).equals(fullName) ? null : "got " + fullName;
            } finally {
                RestAssured.resetClient();
            }
        });
    }

    private RestAssuredClient client(RestAssuredClient.Builder builder) {
        RestAssuredClient client = builder.filters(NO_STATIC_HEADER).build();
        clients.add(client);
        return client;
    }

    private static void whileTheStaticFieldsChange(Executable requests) throws Throwable {
        AtomicBoolean done = new AtomicBoolean();
        Thread changingStatics = new Thread(() -> {
            while (!done.get()) {
                RestAssured.baseURI = "http://wrong.invalid";
                RestAssured.port = 1;
                RestAssured.basePath = "/wrong";
                RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Static", "1").build();
                RestAssured.filters(FAILING);
                RestAssured.reset();
            }
        });
        changingStatics.start();
        try {
            requests.execute();
        } finally {
            done.set(true);
            changingStatics.join();
        }
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
