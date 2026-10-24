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

/**
 * The HTTP client module that sends REST Assured's requests with Apache HttpClient 5 (5.2.1 and later), through its
 * classic API. REST Assured uses it when it's on the classpath, unless {@code httpClientConfig().client(..)} or the
 * system property {@code restassured.http.client} chooses another client.
 * <p>
 * REST Assured follows redirects, answers Basic and Digest challenges, keeps cookies and decodes gzip and deflate
 * itself, so the client does none of that, and it retries nothing. It answers a proxy's login request and NTLM
 * challenges. Timeouts are infinite unless {@code httpClientConfig()} sets them, and the pool doesn't limit the number
 * of connections. The connect timeout also covers the TLS handshake of a new connection: HttpClient 5.2.1 to 5.4.4 time
 * the handshake with the socket timeout of the {@code SocketConfig}, which REST Assured sets to the connect timeout, and
 * 5.6.5 with the connect timeout itself. A handshake that takes too long fails with a
 * {@link io.restassured.common.exception.ConnectionTimeoutException} on every version. A proxy with the scheme https is
 * connected to over TLS, with the SSL settings of the request. A plain connection isn't asked to upgrade to TLS, which
 * HttpClient 5.4 and later otherwise do through a proxy. NTLM is deprecated in HttpClient 5 and may be removed in
 * HttpClient 6.
 * <p>
 * <b>Customizers.</b> {@code httpClientConfig().customizeClient(HttpClientBuilder.class, builder -> ..)} gets the builder
 * after REST Assured has set it up. A builder can't be read, so what a customizer sets has these effects:
 * <ul>
 * <li>{@code setConnectionManager(..)} replaces REST Assured's, together with its TLS settings, connect timeout and
 * pool limits. That's how to limit the connections, and {@code connectionRequestTimeout(..)} then limits the wait for
 * one. {@code closeIdleConnectionsAfterEachResponseAfter(..)} doesn't reach such a connection manager. The client shuts
 * its connection manager down when it's closed, which REST Assured does after each response unless
 * {@code httpClientConfig().reuseHttpClientInstance()} keeps the client. So create the connection manager inside the
 * customizer, or create it once, call {@code setConnectionManagerShared(true)} and close it yourself. A limit that
 * holds across requests needs {@code reuseHttpClientInstance()} or such a shared connection manager. A request whose
 * connection manager has been shut down fails with an {@link IllegalStateException} that says so.</li>
 * <li>{@code setDefaultAuthSchemeRegistry(..)} replaces REST Assured's registry. Keep {@code "ntlm"}
 * ({@code NTLMSchemeFactory.INSTANCE}) in it for {@code auth().ntlm(..)}, and {@code "basic"} and {@code "digest"} for
 * the login of a proxy.</li>
 * <li>{@code useSystemProperties()} makes HttpClient read the system properties {@code http.agent}, for the
 * {@code User-Agent}, and {@code http.keepAlive}, for whether connections are kept, and nothing else.</li>
 * <li>Interceptors, {@code setUserAgent}, {@code setDefaultHeaders}, {@code setKeepAliveStrategy},
 * {@code setConnectionReuseStrategy} and {@code evictIdleConnections} apply.</li>
 * <li>Retries, redirects, cookies, decompression, the answer to a server's challenges and the route planner are
 * switched back afterwards, since REST Assured does them or decides them itself.</li>
 * <li>{@code setDefaultRequestConfig}, {@code setDefaultCredentialsProvider} and {@code setProxy} have no effect,
 * since each request has REST Assured's config, credentials and route. Use {@code httpClientConfig()}, {@code auth()}
 * and {@code proxy(..)} instead.</li>
 * </ul>
 */
package io.restassured.http.apache5;
