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

package io.restassured.itest.java.wire;

import org.eclipse.jetty.server.HttpConnection;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * How {@link WireServer} answers a request.
 */
@FunctionalInterface
public interface Responder {
    /**
     * The response header that {@link #withRemotePort()} sets
     */
    String REMOTE_PORT_HEADER = "X-Remote-Port";

    void respond(RecordedRequest request, HttpServletResponse response) throws IOException;

    /**
     * @return a responder that answers {@code 200 ok} as {@code text/plain}
     */
    static Responder ok() {
        return text(200, "ok");
    }

    /**
     * @return a responder that answers with the status and a {@code text/plain} UTF-8 body
     */
    static Responder text(int status, String body) {
        return (request, response) -> {
            response.setStatus(status);
            response.setContentType("text/plain;charset=UTF-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        };
    }

    /**
     * @return a responder that redirects to {@code location} with the status, which should be a 3xx
     */
    static Responder redirect(int status, String location) {
        return (request, response) -> {
            response.setStatus(status);
            response.setHeader("Location", location);
            response.setContentLength(0);
        };
    }

    /**
     * @return a responder that sets the header and then answers like this one
     */
    default Responder withHeader(String name, String value) {
        return (request, response) -> {
            response.addHeader(name, value);
            respond(request, response);
        };
    }

    /**
     * @return a responder that sets the client port of the connection in {@link #REMOTE_PORT_HEADER} and then answers
     * like this one, so that a test can tell which connection a request arrived on
     */
    default Responder withRemotePort() {
        return (request, response) -> {
            InetSocketAddress remote = (InetSocketAddress) HttpConnection.getCurrentConnection().getEndPoint().getRemoteAddress();
            response.setHeader(REMOTE_PORT_HEADER, String.valueOf(remote.getPort()));
            respond(request, response);
        };
    }
}
