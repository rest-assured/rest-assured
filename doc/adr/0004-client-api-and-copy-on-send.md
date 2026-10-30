# 4. A non-static client API, and sending on copies

Date: 2026-10-17

## Status

Accepted, for 7.0.0. Amended 2026-10-30: a client is picked with `given(client)` or `useClient(..)` and has no DSL
methods of its own.

## Context

REST Assured's configuration lives in 12 public static fields on `RestAssured`, and every static `given()` copies them
into new specifications. Tests that run in parallel and need different base URIs or configs can't share that state
safely (#534).

Sending a request also changes the specifications it was sent from. The request specification gets REST Assured's
internal filters, the method, path, authentication, config and HTTP client, and the response specification gets
logging settings and a link to the request. A built specification used from several threads then races (#945), and
after one send through `given(built, resp)`, a later `given().spec(built).filter(f)` sends before `f` runs.
`RequestSpecBuilder` also copies the static specification in its constructor, so static headers end up twice (#1977).

## Decision

**Sending works on copies of both the request and the response specification**, so neither changes after a send.
Nothing is merged in from the static settings during the copy, so the request on the wire is the same as today. The
specifications the user holds no longer receive internal filters, the method, path, config or logging settings, and a
built response specification no longer gets the request linked into it.

**A `RestAssuredClient` holds its own fixed defaults** and doesn't read the `RestAssured` static fields.
`RestAssured.client()` creates one from their current values, and a builder creates one from scratch. A client holds
what `createTestSpecification()` reads today, its own parser registry, its HTTP clients and cookie store, and is
`AutoCloseable`.

**A client is picked, not called.** Plain `given()` stays the way in, since that style is REST Assured's main
strength, so a client has no `given()`, `when()`, `get(..)` or other DSL methods. `given(client)` names the client for
one request. `useClient(client)` binds a client to the current thread, and `given()`, `when()`, `expect()`,
`given(req, resp)` and the static send methods such as `get(..)` use it until `resetClient()` or `reset()` unbinds it.
`useClient(builder)` builds the client and closes it when it's unbound, while a client passed in stays open, since
other threads may share it. A client named in `given(client)` wins, then the bound client, then the static settings.
`client()`, `config()`, `filters()` and the other static getters and setters stay about the static settings. The
binding is a `ThreadLocal` that isn't inheritable, because a ForkJoinPool worker that JUnit runs tests on could inherit
another test's client. A request on another thread, such as Awaitility's polling thread, uses the static settings
unless it names the client.

**The static API stays.** The static methods delegate to a default client that reads the static fields at each call, so
their behavior doesn't change, and the fields aren't deprecated in 7.0.

**`given(req, resp)` merges with the static settings**, like `given().spec(req)`, instead of wrapping the built
specification, and `RequestSpecBuilder` stops copying the static specification (#1977).

**MockMvc and WebTestClient get `RestAssuredMockMvc.client(..)` and `RestAssuredWebTestClient.client(..)`** on the same
pattern, and Kotlin and Scala get `Given(client) { }` style extensions.

The API's shape is checked with the maintainer after the first `RestAssuredClient` PR.

## Consequences

- One client and one built specification can be used from many threads with different base URIs.
- Code that inspects a specification after sending it, for example `SpecificationQuerier.query(spec).getURI()` or
  `getMethod()`, now sees what was set before the send. This is a non-backward compatible change in the changelog.
- `given(req, resp)` and builders no longer duplicate static headers. Code that relied on the duplication changes
  behavior, also listed in the changelog.
- A new, less clunky configuration DSL can be built on `RestAssuredClient` later in 7.x as an additive change. A fully
  immutable DSL is an 8.0 candidate: it would break silently, since `spec.header(..)` without using the return value
  still compiles.
