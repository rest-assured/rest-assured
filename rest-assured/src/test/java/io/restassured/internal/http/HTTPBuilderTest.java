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

package io.restassured.internal.http;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.restassured.http.ContentType;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpVersion;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HTTPBuilderTest {

    private final AtomicReference<HTTPBuilder.RequestConfigDelegate> executed = new AtomicReference<>();

    private final HTTPBuilder http = new HTTPBuilder("http://localhost:8080/base", true, null, null, null, new DefaultHttpClient()) {
        @Override
        protected Object doRequest(RequestConfigDelegate delegate) {
            executed.set(delegate);
            return "result";
        }
    };

    @Test
    void request_executes_the_request_configured_by_the_configurer() throws Exception {
        Object result = http.request("PUT", ContentType.JSON, true, request -> {
            request.getUri().setPath("/path");
            request.setRequestContentType("text/plain");
            request.setBody(request.getRequestContentType(), "body");
        });

        HTTPBuilder.RequestConfigDelegate delegate = executed.get();
        assertThat(result).isEqualTo("result");
        assertThat(delegate.getRequest().getMethod()).isEqualTo("PUT");
        assertThat(delegate.getUri().toString()).isEqualTo("http://localhost:8080/path");
        assertThat(delegate.getContentType()).isEqualTo(ContentType.JSON);
        assertThat(delegate.getRequestContentType()).isEqualTo("text/plain");
        assertThat(((HttpEntityEnclosingRequest) delegate.getRequest()).getEntity().getContentType().getValue()).isEqualTo("text/plain");
        assertThat(delegate.getResponse()).containsOnlyKeys("success", "failure");
    }

    @Test
    void checked_exceptions_thrown_by_a_groovy_closure_configurer_propagate_unchanged() {
        Throwable uriSyntax = catchThrowable(() -> requestConfiguredByGroovy("throw new java.net.URISyntaxException('a b', 'bad uri')"));
        Throwable io = catchThrowable(() -> requestConfiguredByGroovy("throw new java.io.FileNotFoundException('missing')"));

        assertThat(uriSyntax).isExactlyInstanceOf(URISyntaxException.class).hasMessage("bad uri: a b");
        assertThat(io).isExactlyInstanceOf(FileNotFoundException.class).hasMessage("missing");
        assertThat(executed.get()).isNull();
    }

    @Test
    void groovy_closure_configurer_configures_the_request_through_the_delegate() throws Exception {
        requestConfiguredByGroovy("d.with { uri.path = '/groovy'; setRequestContentType('text/plain'); body = 'text'; " +
                "response.success = { r, c -> 'handled ' + c } as io.restassured.internal.http.HttpResponseHandler }");

        HTTPBuilder.RequestConfigDelegate delegate = executed.get();
        assertThat(delegate.getUri().toString()).isEqualTo("http://localhost:8080/groovy");
        assertThat(((HttpEntityEnclosingRequest) delegate.getRequest()).getEntity().getContentType().getValue()).isEqualTo("text/plain");
        assertThat(delegate.findResponseHandler(200).handle(null, "content")).isEqualTo("handled content");
    }

    @Test
    void checked_exceptions_thrown_by_a_groovy_closure_response_handler_propagate_unchanged() {
        HttpResponseHandler handler = (HttpResponseHandler) new GroovyShell().evaluate(
                "{ r, c -> throw new java.io.FileNotFoundException('missing') } as io.restassured.internal.http.HttpResponseHandler");

        assertThat(catchThrowable(() -> handler.handle(null, null))).isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void finds_the_handler_for_the_status_code_before_the_handler_for_its_status() throws Exception {
        http.request("GET", ContentType.ANY, false, request -> request.getResponse().put("404", (response, content) -> "404 handler"));
        HTTPBuilder.RequestConfigDelegate delegate = executed.get();

        assertThat(delegate.findResponseHandler(404).handle(null, null)).isEqualTo("404 handler");
        assertThat(delegate.findResponseHandler(500)).isSameAs(delegate.getResponse().get("failure"));
        assertThat(delegate.findResponseHandler(200)).isSameAs(delegate.getResponse().get("success"));
    }

    @Test
    void default_success_handler_buffers_streaming_content_and_default_failure_handler_throws() throws Exception {
        http.request("GET", ContentType.ANY, false, request -> {
        });
        HTTPBuilder.RequestConfigDelegate delegate = executed.get();
        HttpResponseDecorator response = new HttpResponseDecorator(new BasicHttpResponse(HttpVersion.HTTP_1_1, 404, "Not Found"), null);

        Object buffered = delegate.findResponseHandler(200).handle(response, new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));
        Throwable failure = catchThrowable(() -> delegate.findResponseHandler(404).handle(response, null));

        assertThat(buffered).isInstanceOf(ByteArrayInputStream.class);
        assertThat(new String(((InputStream) buffered).readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("abc");
        assertThat(failure).isExactlyInstanceOf(HttpResponseException.class);
        assertThat(((HttpResponseException) failure).getStatusCode()).isEqualTo(404);
    }

    private void requestConfiguredByGroovy(String configurerBody) {
        Binding binding = new Binding();
        binding.setVariable("http", http);
        new GroovyShell(binding).evaluate("http.request('PUT', '*/*', true) { d -> " + configurerBody + " }");
    }
}
