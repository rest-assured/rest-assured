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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A proxy that tunnels a CONNECT to the target port. With credentials, it first asks for a Basic login with a 407.
 */
final class TunnelingProxy implements AutoCloseable {
    private final ServerSocket socket;
    private final List<RawServer.Request> connects = new CopyOnWriteArrayList<>();
    private final int targetPort;
    private final String credentials;

    /**
     * @param socket      the socket of the proxy, which may speak TLS
     * @param credentials the Proxy-Authorization header the proxy asks for, or {@code null} if it asks for none
     */
    TunnelingProxy(ServerSocket socket, int targetPort, String credentials) {
        this.socket = socket;
        this.targetPort = targetPort;
        this.credentials = credentials;
        RawServer.daemon(() -> {
            while (!socket.isClosed()) {
                try {
                    Socket connection = socket.accept();
                    RawServer.daemon(() -> tunnel(connection));
                } catch (IOException e) {
                    return;
                }
            }
        });
    }

    static TunnelingProxy plain(int targetPort, String credentials) throws IOException {
        return new TunnelingProxy(new ServerSocket(0, 50, InetAddress.getLoopbackAddress()), targetPort, credentials);
    }

    int port() {
        return socket.getLocalPort();
    }

    List<RawServer.Request> connects() {
        return connects;
    }

    private void tunnel(Socket connection) {
        try (connection) {
            OutputStream out = connection.getOutputStream();
            while (true) {
                String head = RawServer.readHead(connection.getInputStream());
                if (head == null) {
                    return;
                }
                RawServer.Request connect = new RawServer.Request(head, new byte[0], connection.getPort());
                connects.add(connect);
                if (credentials == null || credentials.equals(connect.header("Proxy-Authorization"))) {
                    break;
                }
                out.write(RawServer.response(407, "", "Proxy-Authenticate: Basic realm=\"proxy\"").getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
            }
            out.write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            try (Socket target = new Socket(InetAddress.getLoopbackAddress(), targetPort)) {
                RawServer.daemon(() -> {
                    try {
                        connection.getInputStream().transferTo(target.getOutputStream());
                    } catch (IOException ignored) {
                        // One side closed the tunnel
                    }
                });
                target.getInputStream().transferTo(out);
            }
        } catch (IOException ignored) {
            // One side closed the tunnel
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
