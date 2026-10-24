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

package io.restassured.http.apache5;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A server that records each request as it arrived on the wire, with the connection it came on, and answers with a raw
 * response. A connection stays open for the next request unless the response says {@code Connection: close}. A
 * request body is read by its Content-Length or its chunks, after a {@code 100 Continue} if the request expects one.
 */
final class RawServer implements AutoCloseable {
    static final String PASSWORD = "test1234";
    /**
     * The loopback address the servers listen on, as a URI host. Tests that expect the connection or the TLS
     * handshake to fail use it instead of {@code localhost}: when {@code localhost} also resolves to {@code ::1},
     * HttpClient 5 tries that address after the failure and reports its "Connection refused" instead.
     */
    static final String LOOPBACK = uriHost(InetAddress.getLoopbackAddress());

    private final ServerSocket socket;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final List<Socket> connections = new CopyOnWriteArrayList<>();
    private volatile Handler handler = text(request -> response(200, "ok"));

    RawServer() throws IOException {
        this(new ServerSocket(0, 50, InetAddress.getLoopbackAddress()));
    }

    private RawServer(ServerSocket socket) {
        this.socket = socket;
        daemon(this::serve);
    }

    /**
     * @return a server that speaks TLS with the certificate of the PKCS12 key store
     */
    static RawServer tls(String keyStore) throws Exception {
        return new RawServer(sslContext(keyStore).getServerSocketFactory().createServerSocket(0, 50, InetAddress.getLoopbackAddress()));
    }

    static SSLContext sslContext(String keyStore) throws Exception {
        KeyStore keys = KeyStore.getInstance("PKCS12");
        try (InputStream in = RawServer.class.getResourceAsStream("/" + keyStore)) {
            keys.load(in, PASSWORD.toCharArray());
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keys, PASSWORD.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        return context;
    }

    /**
     * @return a response with a Content-Length, which keeps the connection open
     */
    static String response(int status, String body, String... headers) {
        StringBuilder response = new StringBuilder("HTTP/1.1 ").append(status).append(" Whatever\r\n");
        for (String header : headers) {
            response.append(header).append("\r\n");
        }
        return response.append("Content-Length: ").append(body.length()).append("\r\n\r\n").append(body).toString();
    }

    /**
     * Answers each request with the text, or not at all when it's {@code null}, while the connection stays open
     */
    RawServer respond(Function<Request, String> responder) {
        return handle(text(responder));
    }

    /**
     * Answers each request by writing to the connection
     */
    RawServer handle(Handler handler) {
        this.handler = handler;
        return this;
    }

    private static Handler text(Function<Request, String> responder) {
        return (request, out) -> {
            String response = responder.apply(request);
            if (response == null) {
                return true;
            }
            out.write(response.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            return !response.toLowerCase(Locale.ROOT).contains("\r\nconnection: close\r\n");
        };
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

    /**
     * @return the number of connections that have been opened
     */
    int connectionCount() {
        return connections.size();
    }

    /**
     * Waits until the client has closed every connection
     */
    boolean awaitConnectionsClosed(long millis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            if (connections.stream().allMatch(Socket::isClosed)) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    private void serve() {
        while (!socket.isClosed()) {
            try {
                Socket connection = socket.accept();
                connections.add(connection);
                daemon(() -> serve(connection));
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket connection) {
        try (connection) {
            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();
            boolean open = true;
            while (open) {
                String head = readHead(in);
                if (head == null) {
                    return;
                }
                if (head.toLowerCase(Locale.ROOT).contains("\r\nexpect: 100-continue\r\n")) {
                    out.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();
                }
                Request request = new Request(head, readBody(in, head), connection.getPort());
                requests.add(request);
                open = handler.handle(request, out);
            }
        } catch (IOException e) {
            // The client went away
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
        return null;
    }

    private static byte[] readBody(InputStream in, String head) throws IOException {
        String lowerCase = head.toLowerCase(Locale.ROOT);
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

    private static String uriHost(InetAddress address) {
        return address instanceof Inet6Address ? "[" + address.getHostAddress() + "]" : address.getHostAddress();
    }

    static void daemon(Runnable runnable) {
        Thread thread = new Thread(runnable, "raw-server");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        socket.close();
        for (Socket connection : connections) {
            connection.close();
        }
    }

    /**
     * Answers a request on its connection.
     */
    interface Handler {
        /**
         * @return whether the connection stays open for the next request
         */
        boolean handle(Request request, OutputStream out) throws IOException, InterruptedException;
    }

    /**
     * A request as it arrived: the request line and headers as sent, the body without its framing, and the client port
     * of its connection.
     */
    static final class Request {
        final String head;
        final byte[] body;
        final int clientPort;

        Request(String head, byte[] body, int clientPort) {
            this.head = head;
            this.body = body;
            this.clientPort = clientPort;
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
            List<String> values = headers(name);
            return values.isEmpty() ? null : values.get(0);
        }

        List<String> headers(String name) {
            List<String> values = new ArrayList<>();
            for (String line : headerLines()) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).equalsIgnoreCase(name)) {
                    values.add(line.substring(colon + 1).trim());
                }
            }
            return values;
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
