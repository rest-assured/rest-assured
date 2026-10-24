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

package io.restassured.http.jdk;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A server that records each request as it arrived on the wire and answers with a raw response, one request per
 * connection. A request body is read by its Content-Length or its chunks.
 */
final class RawServer implements AutoCloseable {
    private final ServerSocket socket;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile Function<Request, String> responder = request -> response(200, "ok");
    private volatile boolean readsBodies = true;

    RawServer() throws IOException {
        socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread thread = new Thread(this::serve, "raw-server");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @return a response with a Content-Length and Connection: close
     */
    static String response(int status, String body, String... headers) {
        StringBuilder response = new StringBuilder("HTTP/1.1 ").append(status).append(" Whatever\r\n");
        for (String header : headers) {
            response.append(header).append("\r\n");
        }
        return response.append("Content-Length: ").append(body.length()).append("\r\nConnection: close\r\n\r\n").append(body).toString();
    }

    RawServer respond(Function<Request, String> responder) {
        this.responder = responder;
        return this;
    }

    /**
     * Answers right after the request head, without reading the body, as a server that rejects a request early does
     */
    RawServer answerBeforeTheBody() {
        readsBodies = false;
        return this;
    }

    URI uri(String path) {
        return URI.create("http://localhost:" + port() + path);
    }

    int port() {
        return socket.getLocalPort();
    }

    List<Request> requests() {
        return new ArrayList<>(requests);
    }

    Request onlyRequest() {
        List<Request> received = requests();
        if (received.size() != 1) {
            throw new AssertionError("Expected one request but got " + received);
        }
        return received.get(0);
    }

    private void serve() {
        while (!socket.isClosed()) {
            try (Socket connection = socket.accept()) {
                InputStream in = connection.getInputStream();
                String head = readHead(in);
                if (head == null) {
                    continue;
                }
                Request request = new Request(head, readsBodies ? readBody(in, head) : new byte[0]);
                requests.add(request);
                OutputStream out = connection.getOutputStream();
                String response = responder.apply(request);
                if (response != null) {
                    out.write(response.getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();
                }
            } catch (IOException ignored) {
                // The client went away
            }
        }
    }

    static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int newLines = 0;
        int c;
        while ((c = in.read()) != -1) {
            head.write(c);
            if (c == '\n') {
                if (++newLines == 2) {
                    return head.toString(StandardCharsets.ISO_8859_1);
                }
            } else if (c != '\r') {
                newLines = 0;
            }
        }
        return head.size() == 0 ? null : head.toString(StandardCharsets.ISO_8859_1);
    }

    private static byte[] readBody(InputStream in, String head) throws IOException {
        String lowerCase = head.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("expect: 100-continue")) {
            return new byte[0];
        }
        if (lowerCase.contains("transfer-encoding: chunked")) {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            while (true) {
                String sizeLine = readLine(in).trim();
                if (sizeLine.isEmpty()) {
                    throw new EOFException("The client went away in the middle of a chunked body");
                }
                int size = Integer.parseInt(sizeLine.split(";", 2)[0], 16);
                if (size == 0) {
                    readLine(in);
                    return body.toByteArray();
                }
                body.write(in.readNBytes(size));
                readLine(in);
            }
        }
        int start = lowerCase.indexOf("content-length:");
        if (start == -1) {
            return new byte[0];
        }
        int end = lowerCase.indexOf('\r', start);
        return in.readNBytes(Integer.parseInt(head.substring(start + "content-length:".length(), end).trim()));
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            if (c != '\r') {
                line.append((char) c);
            }
        }
        return line.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    /**
     * A request as it arrived: the request line and headers as sent, and the body without its framing.
     */
    static final class Request {
        final String head;
        final byte[] body;

        Request(String head, byte[] body) {
            this.head = head;
            this.body = body;
        }

        String requestLine() {
            return head.substring(0, head.indexOf("\r\n"));
        }

        /**
         * @return the header lines as sent, without the request line
         */
        List<String> headerLines() {
            List<String> lines = new ArrayList<>(List.of(head.split("\r\n")));
            lines.remove(0);
            return lines;
        }

        String header(String name) {
            for (String line : headerLines()) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).equalsIgnoreCase(name)) {
                    return line.substring(colon + 1).trim();
                }
            }
            return null;
        }

        String bodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return head;
        }
    }
}
