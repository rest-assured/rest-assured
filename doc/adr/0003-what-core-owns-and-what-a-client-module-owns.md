# 3. What core owns and what a client module owns

Date: 2026-10-17

## Status

Proposed, for 7.0.0.

## Context

ADR 0002 makes the HTTP client a module. A test library must behave the same whichever client runs the request, or a
suite would pass with one module and fail with another. The JDK client and HttpClient 5 differ in what they can do:
the JDK client has no per-request redirect settings, no hostname verifier, restricted headers (`Host`, `Connection`,
`Content-Length`, `Expect`, `Upgrade`), HTTP/2 as its default and retries it controls only through JVM-wide
properties. HttpClient 5 retries a 503 once by default and times out after 3 minutes.

Today every request creates a new HttpClient 4 client that is never shut down. Only `ConnectionConfig` closes idle
connections.

## Decision

**Core owns everything except moving bytes:** URI building and encoding, bodies and multipart, gzip and deflate
decoding, cookies, OAuth 1 signing, logging, filters and exception translation, and from 7.0 redirects and Basic and
Digest login challenges. A client module opens connections, applies TLS and proxy settings, writes the request and
returns the status, headers and body stream. Proxy login stays in the client module, because for HTTPS it happens
inside the client's tunnel setup.

**Timeouts surface as REST Assured's `ConnectionTimeoutException` and `ResponseTimeoutException`**, in
`io.restassured.common.exception`, both extending `java.net.SocketTimeoutException`, with the client's exception as the
cause. `catch (SocketTimeoutException e)` keeps working on the JDK client, whose `HttpTimeoutException` is not one.

**Redirects.** From 7.0.0 core follows redirects itself, with HttpClient 4's rules: 301, 302, 307 and 308 only for GET
and HEAD, 303 as a GET, and today's cross-host header stripping. `RedirectConfig` keeps its meaning. Too many redirects
throws `TooManyRedirectsException`, an `IOException`, where HttpClient 4 throws `ClientProtocolException`. 6.x keeps
HttpClient 4 following redirects.

**Login challenges keep today's rule.** `basic(..)` and `digest(..)` both answer a Basic or a Digest challenge, scoped
to the original host and port, and never to another host after a redirect. There is no `DigestAuthScheme`, because
making the method name pick the scheme would break tests silently.

**A client that can't do something fails loudly**, with an `UnsupportedOperationException` naming the feature, the
client in use, how it was chosen and the fix. A setting a client can only approximate prints a one-time warning that
says how. The JDK client fails this way for NTLM, a custom `HostnameVerifier`, the restricted headers and HTTPS through
a proxy that asks for a Basic login.

**REST Assured never retries a request itself**, and connect, connection-request and response timeouts are infinite
unless configured. The apache5 adapter turns retries, decompression and redirects off. The JDK client's own connect
and stale-connection retries are controlled only by JVM-wide properties, which the docs name. REST Assured never sets
JVM-wide properties.

**Both client modules honor the JVM's proxy settings** (`-Dhttp.proxyHost` and friends) through
`ProxySelector.getDefault()`, as today.

**Client lifetime is defined per module:**

| Situation | JDK module | apache5 module |
|---|---|---|
| Static API, default | Taken from a shared cache, with connections reused. | A new client per request, as today, closed once the body is read or the `asInputStream()` stream is closed. |
| `reuseHttpClientInstance()` | The same shared client. With `keepResponseCookies(true)`, response cookies are kept in a core-owned store held by that config. | One pooled client per `HttpClientConfig`, never closed, as today. Cookies as for JDK. |
| `RestAssuredClient` | Owns its cached clients and cookie store, closed in `close()` on Java 21 and later. | Owns one pooled client per distinct setting, closed in `close()`. |
| `ConnectionConfig.closeIdleConnectionsAfterEachResponse(..)` | The request gets its own unshared client, closed after the response on Java 21 and later. On Java 17 it is left to the garbage collector, with a one-time warning. | Closes the pool's idle connections after each response or stream close, as today. |

**Shared JDK clients are cached by value**, one per distinct TLS, proxy, HTTP version and connect timeout setting, at
most 16, with the least recently used evicted and closed on Java 21 and later. `SSLConfig` records what was asked for
(relaxed validation and protocol, store paths, types and passwords), because `relaxedHTTPSValidation()` builds a new
`SSLContext` on every call and an identity key would create a client per request.

**The JDK adapter** uses HTTP/1.1 unless HTTP/2 is asked for, maps `Expect: 100-continue` to the builder's
`expectContinue(true)`, and implements `relaxedHTTPSValidation()` and `allowAllHostnames()` with an
`X509ExtendedTrustManager`, since a plain `X509TrustManager` still gets the host name checked. A request without a body
uses the builder's `GET()` and `DELETE()`, or `method(name, noBody())` only for other methods, because
`method("GET", noBody())` sends `Content-Length: 0`, which HttpClient 4 doesn't.

## Consequences

- Behavior is the same whichever module runs, apart from the documented JDK client differences: header name case and
  order, the `User-Agent` value, the read timeout covering only the wait for headers, connection reuse and JDK retries.
- Core grows a redirect follower, a challenge handler and a cookie store that HttpClient 4 provided. Each needs
  characterization tests against today's behavior before the switch.
- Connections are reused by default on the JDK client, where today each request opens a new one.
- The first network request of a JVM pays for discovery and the version check.
