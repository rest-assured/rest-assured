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

import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.ClientSharing;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * HTTP client providers that send nothing. Their clients answer with {@link #response}, record what they were asked
 * for in {@link #created} and {@link #sent}, and record when they are closed.
 */
public final class FakeHttpClients {
    public static final List<FakeClient> created = new CopyOnWriteArrayList<>();
    public static final List<HttpRequest> sent = new CopyOnWriteArrayList<>();
    public static volatile Answer response = request -> ok("ok");
    public static volatile boolean failOnClose;

    private FakeHttpClients() {
    }

    public static void reset() {
        created.clear();
        sent.clear();
        response = request -> ok("ok");
        failOnClose = false;
    }

    public static HttpResponse ok(String body) {
        byte[] bytes = body.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", "text/plain; charset=UTF-8")), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }

    public interface Answer {
        HttpResponse answer(HttpRequest request) throws IOException;
    }

    public static class FakeProvider implements HttpClientProvider {
        private final String name;
        private final String displayName;
        private final String artifact;
        private final String libraryDescription;
        private final ClientSharing sharing;
        private final Class<?> builderType;

        protected FakeProvider(String name, String displayName, String artifact, String libraryDescription, ClientSharing sharing, Class<?> builderType) {
            this.name = name;
            this.displayName = displayName;
            this.artifact = artifact;
            this.libraryDescription = libraryDescription;
            this.sharing = sharing;
            this.builderType = builderType;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public String artifact() {
            return artifact;
        }

        @Override
        public String libraryDescription() {
            return libraryDescription;
        }

        @Override
        public ClientSharing sharing() {
            return sharing;
        }

        @Override
        public Class<?> builderType() {
            return builderType;
        }

        @Override
        public HttpClientInstance newClient(ClientSettings settings) {
            FakeClient client = new FakeClient(this, settings);
            created.add(client);
            return client;
        }
    }

    public static class FakeClient implements HttpClientInstance {
        public final FakeProvider provider;
        public final ClientSettings settings;
        public volatile boolean closed;
        public final AtomicInteger closes = new AtomicInteger();

        FakeClient(FakeProvider provider, ClientSettings settings) {
            this.provider = provider;
            this.settings = settings;
        }

        @Override
        public HttpResponse send(HttpRequest request) throws IOException {
            sent.add(request);
            return response.answer(request);
        }

        @Override
        public void close() {
            closed = true;
            closes.incrementAndGet();
            if (failOnClose) {
                throw new IllegalStateException("Closing failed");
            }
        }
    }

    // A builder type of the fake Apache HttpClient 5
    public static class Apache5Builder {
    }

    public static class Jdk extends FakeProvider {
        public Jdk() {
            super("jdk", "the JDK HTTP client", "io.rest-assured:http-client-jdk", "Java 17.0.20", ClientSharing.SHARED, java.net.http.HttpClient.Builder.class);
        }
    }

    public static class Apache5 extends FakeProvider {
        public Apache5() {
            super("apache5", "Apache HttpClient 5", "io.rest-assured:http-client-apache5", "Apache HttpClient 5.5.1", ClientSharing.PER_REQUEST, Apache5Builder.class);
        }
    }

    public static class OkHttp extends FakeProvider {
        public OkHttp() {
            super("okhttp", "OkHttp", "com.example:ra-okhttp", null, ClientSharing.PER_REQUEST, null);
        }
    }

    public static class Other extends FakeProvider {
        public Other() {
            super("other", "another client", "com.example:ra-other", "Other 1.0", ClientSharing.SHARED, null);
        }
    }

    // A client of another library whose name can't be read
    public static class Faulty extends FakeProvider {
        public Faulty() {
            super(null, "a faulty client", "com.example:ra-faulty", null, ClientSharing.SHARED, null);
        }

        @Override
        public String name() {
            throw new IllegalStateException("No name");
        }
    }

    // A client of another library that uses the name of one of REST Assured's
    public static class Impostor extends FakeProvider {
        public Impostor() {
            super("jdk", "an impostor", "com.example:ra-impostor", null, ClientSharing.SHARED, null);
        }
    }
}
