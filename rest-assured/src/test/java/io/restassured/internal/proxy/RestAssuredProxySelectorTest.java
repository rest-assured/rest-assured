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

package io.restassured.internal.proxy;

import com.sun.net.httpserver.HttpServer;
import io.restassured.specification.ProxySpecification;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestAssuredProxySelectorTest {

    private static final URI URI = java.net.URI.create("http://example.com/path");

    @Test
    void exposesPropertiesThroughGettersAndSetters() {
        RecordingProxySelector delegate = new RecordingProxySelector(singletonList(Proxy.NO_PROXY));
        ProxySpecification proxySpecification = new ProxySpecification("127.0.0.1", 8888, "http");
        RestAssuredProxySelector selector = new RestAssuredProxySelector();

        assertThat(selector.getDelegatingProxySelector()).isNull();
        assertThat(selector.getProxySpecification()).isNull();

        selector.setDelegatingProxySelector(delegate);
        selector.setProxySpecification(proxySpecification);

        assertThat(selector.getDelegatingProxySelector()).isSameAs(delegate);
        assertThat(selector.getProxySpecification()).isSameAs(proxySpecification);
    }

    @Test
    void selectReturnsHttpProxyFromProxySpecificationWithoutConsultingTheDelegate() {
        RecordingProxySelector delegate = new RecordingProxySelector(singletonList(Proxy.NO_PROXY));
        RestAssuredProxySelector selector = selector(delegate, new ProxySpecification("127.0.0.1", 8888, "https"));

        List<Proxy> proxies = selector.select(URI);

        assertThat(proxies).containsExactly(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)));
        assertThat(proxies).isInstanceOf(ArrayList.class);
        assertThat(delegate.selectedUris).isEmpty();
    }

    @Test
    void selectUsesProxySpecificationEvenWithoutDelegate() {
        RestAssuredProxySelector selector = selector(null, new ProxySpecification("localhost", 8080, "http"));

        assertThat(selector.select(URI)).containsExactly(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8080)));
    }

    @Test
    void selectDelegatesWhenProxySpecificationIsNull() {
        List<Proxy> delegateProxies = singletonList(Proxy.NO_PROXY);
        RecordingProxySelector delegate = new RecordingProxySelector(delegateProxies);
        RestAssuredProxySelector selector = selector(delegate, null);

        List<Proxy> proxies = selector.select(URI);

        assertThat(proxies).isSameAs(delegateProxies);
        assertThat(delegate.selectedUris).containsExactly(URI);
    }

    @Test
    void selectThrowsNullPointerExceptionWhenNeitherProxySpecificationNorDelegateIsDefined() {
        RestAssuredProxySelector selector = new RestAssuredProxySelector();

        assertThatThrownBy(() -> selector.select(URI)).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    void selectThrowsIllegalArgumentExceptionWhenUriIsNull() {
        RestAssuredProxySelector selector = selector(new RecordingProxySelector(singletonList(Proxy.NO_PROXY)), new ProxySpecification("localhost", 8080, "http"));

        assertThatThrownBy(() -> selector.select(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("URI cannot be null");
    }

    @Test
    void connectFailedRethrowsTheCheckedIOExceptionUnwrappedWhenProxySpecificationIsDefined() {
        RecordingProxySelector delegate = new RecordingProxySelector(singletonList(Proxy.NO_PROXY));
        RestAssuredProxySelector selector = selector(delegate, new ProxySpecification("localhost", 8080, "http"));
        IOException ioe = new IOException("connect failed");

        assertThatThrownBy(() -> selector.connectFailed(URI, new InetSocketAddress("localhost", 8080), ioe)).isSameAs(ioe);
        assertThat(delegate.failedUris).isEmpty();
    }

    @Test
    void connectFailedDelegatesWhenProxySpecificationIsNull() {
        RecordingProxySelector delegate = new RecordingProxySelector(singletonList(Proxy.NO_PROXY));
        RestAssuredProxySelector selector = selector(delegate, null);
        IOException ioe = new IOException("connect failed");
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);

        selector.connectFailed(URI, address, ioe);

        assertThat(delegate.failedUris).containsExactly(URI);
        assertThat(delegate.failedAddresses).containsExactly(address);
        assertThat(delegate.failedExceptions).containsExactly(ioe);
    }

    @Test
    void requestWithProxySpecificationIsSentToTheProxy() throws Exception {
        // Exercises the named-argument construction of RestAssuredProxySelector in RequestSpecificationImpl.applyProxySettings
        AtomicReference<String> requestUriSeenByProxy = new AtomicReference<>();
        HttpServer proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/", exchange -> {
            requestUriSeenByProxy.set(exchange.getRequestURI().toString());
            byte[] body = "from proxy".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        proxy.start();
        try {
            String body = given().
                    proxy("127.0.0.1", proxy.getAddress().getPort()).
            when().
                    get("http://rest-assured-proxy-selector-test.invalid/hello").
            then().
                    statusCode(200).
                    extract().asString();

            assertThat(body).isEqualTo("from proxy");
            assertThat(requestUriSeenByProxy.get()).isEqualTo("http://rest-assured-proxy-selector-test.invalid/hello");
        } finally {
            proxy.stop(0);
        }
    }

    private static RestAssuredProxySelector selector(ProxySelector delegate, ProxySpecification proxySpecification) {
        RestAssuredProxySelector selector = new RestAssuredProxySelector();
        selector.setDelegatingProxySelector(delegate);
        selector.setProxySpecification(proxySpecification);
        return selector;
    }

    private static class RecordingProxySelector extends ProxySelector {
        private final List<Proxy> proxies;
        final List<URI> selectedUris = new ArrayList<>();
        final List<URI> failedUris = new ArrayList<>();
        final List<SocketAddress> failedAddresses = new ArrayList<>();
        final List<IOException> failedExceptions = new ArrayList<>();

        RecordingProxySelector(List<Proxy> proxies) {
            this.proxies = proxies;
        }

        @Override
        public List<Proxy> select(URI uri) {
            selectedUris.add(uri);
            return proxies;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            failedUris.add(uri);
            failedAddresses.add(sa);
            failedExceptions.add(ioe);
        }
    }
}
