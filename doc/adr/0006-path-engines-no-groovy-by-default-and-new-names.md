# 6. Path engines as modules, no Groovy by default, and the new public names

Date: 2026-10-17

## Status

Accepted, for 6.3.0 and 7.0.0. Amends ADR 0001.

## Context

ADR 0001 took Groovy out of core's own code but kept it on every user's classpath: core depends on `json-path` and
`xml-path`, which evaluate GPath as Groovy scripts. ADR 0002 makes path engines modules. This ADR settles what core does
without one, how Groovy stays out of the default dependency tree, and the public names that 6.3.0 and 7.0.0 add, so they
are fixed before any of them ships.

A probe of Jayway against REST Assured's own tests showed that Jayway can't stand in for GPath silently: list spreads
(`store.book.title`), quoted keys, closures and `sum()` or `max()` on a spread fail or differ. So there is no default
JSON path engine that keeps 6.x paths working without Groovy.

## Decision

### Path engines

**No path engine is a default.** `json-path-gpath` and `xml-path-gpath` (today's `json-path` and `xml-path`, renamed,
packages unchanged) evaluate GPath. `json-path-jayway` evaluates JSONPath. XML and HTML have no default engine, and an
XPath module may come later in 7.x.

**A path evaluated with no engine fails with a clear message**: the failed path, that since 7.0.0 the path engine is a
separate dependency, and Maven and Gradle snippets for both JSON engines with the user's real REST Assured version, read
from core's version file. The Maven snippets are written on indented lines, like a pom. The message also gives the
OpenRewrite command and a link to the "Path engines" heading of the ReleaseNotes70 wiki page. XML and HTML point to
`xml-path-gpath`. With only Jayway present, GPath-only syntax fails with a shorter message suggesting `json-path-gpath`.

**The Jayway module parses the body itself** into plain maps and lists, with numbers following `JsonConfig`, and
evaluates with a provider over those maps and lists, an explicitly built configuration and no `SUPPRESS_EXCEPTIONS`.

- The parser is a small strict JSON parser in the module, not json-smart. The phase 0 spike showed that json-smart turns
  any decimal of 18 characters or fewer into a `Double`, so `BIG_DECIMAL` loses the scale (`1.50` becomes `1.5`), and it
  converts a number before REST Assured can check its length. A 101-line parser matched today's output in all four
  `NumberReturnType` modes. The module excludes json-smart, and a provider over plain maps and lists works without it.
- Jayway throws `PathNotFoundException` for every case, so the module tells them apart by message, which is identical on
  2.8.0, 2.9.0 and 2.10.0, and by checking the parent's type. A missing property, a missing parent and a property of a
  `null` all give `null`, as in GPath, so `body("missing", nullValue())` keeps working. A list addressed as an object
  fails with a `[*]` hint and, without GPath, a pointer to `json-path-gpath`. A property of a string or number gives what
  GPath gives, pinned by a characterization test.

**Form authentication and CSRF don't need a path engine.** `FormAuthFilter` and `CsrfTokenFinder` parse the page with
TagSoup's SAX parser into a JDK DOM and walk it for the `form`, `input`, `meta` and `base` elements. TagSoup's
default attributes are turned off, so a form without a `method` doesn't get one, and attribute values such as
`type="HIDDEN"` are compared ignoring case. TagSoup is already
a direct dependency of core. Core imports `org.ccil.cowan.tagsoup` with `resolution:=optional` in OSGi, and the filters
fail with a message naming the bundle when it's missing.

### `jsonPath()`, `xmlPath()` and `htmlPath()` stay, through a holder class

The six path methods of `ResponseBodyExtractionOptions` stay. `rest-assured` declares `json-path-gpath` and
`xml-path-gpath` as `<optional>true</optional>` dependencies, which Maven doesn't pass on to users. Each method:

1. checks with `Class.forName` that the GPath class is present, without initializing it, and otherwise throws the
   missing-engine message;
2. delegates to a package-private holder class. The holder is the only class whose method bodies use GPath classes.
   The API names `JsonPath` and `XmlPath` only as return types.

The check is `Class.forName(name, false, <core's class loader>)`, so it also works with core's optional and dynamic OSGi
imports.

A probe on JDK 17 showed that a class whose methods return a type from an absent optional jar loads and runs, even with
`-Xverify:all`, as long as those methods aren't called. The GPath modules depend only on `rest-assured-common`, never
on core.

### No Groovy by default

**No Groovy in `rest-assured`'s default dependency tree in 7.0, not even transitively.** Only `json-path-gpath`,
`xml-path-gpath` and `rest-assured-all` carry Groovy. A `maven-enforcer-plugin` `bannedDependencies` rule fails the
build if `org.apache.groovy` or `org.codehaus.groovy` enters the compile or runtime scope of a small consumer module that
depends only on `rest-assured`, and of `rest-assured-common`, `http-client-jdk`, spring-commons, spring-mock-mvc,
spring-web-test-client, kotlin-extensions, scala-support and scala-extensions. The `NoGroovyBytecodeTest`s in core and
common stay.

The Spring MockMvc and WebTestClient modules keep depending on `rest-assured`, with unchanged main dependencies. They
bring only the JDK module and no Groovy, and their `commons-logging` exclusions become dead but harmless.

This amends ADR 0001, which says Groovy reaches core through `json-path` and `xml-path`. From 7.0 it reaches a user only
through a GPath module they add.

### What core takes over from json-path and xml-path

**The object mapper factory types and `NumberReturnType` move to `rest-assured-common` in 6.3.0**, because
`ObjectMapperConfig` and `JsonConfig` name them in public signatures and core can't drop json-path without them. The old
types stay, deprecated, in the GPath modules until 8.0, and each old factory interface extends its new one. Getters keep
their names: in 6.x they return the old types, wrapping a new-type value, and in 7.0 they return the new types.
`NumberReturnType` is an enum, so the old and new enums map by name instead, the new one keeps `isFloatOrDouble()`, and
`JsonConfig` gets constructor overloads as well as setter overloads. `JsonPathConfig.DEFAULT_NUMBER_LENGTH_LIMIT` is not
deprecated, because the GPath module still needs it, and a test checks it equals `JsonConfig`'s copy. Common gets
optional compile dependencies on the mapper libraries the factory interfaces name, which become optional OSGi imports. Jackson
1's factories aren't copied. Their setters are deprecated instead, and Jackson 1 leaves in 7.0.

**The JSON writer in 7.0.** Today a `Map` or `Collection` request body that reaches the JSON encoder, for example form
parameters sent with a JSON content type, is written by Groovy's `new JsonBuilder(model).toString()`. In 7.0 a writer in
core produces the same output for the JDK types JsonBuilder handles: `null`, strings and other `CharSequence`s,
numbers, booleans, characters, enums by name, `UUID`, `URI` and `URL` as strings, `Date` and `Calendar` in JsonBuilder's
GMT format, arrays, collections, maps and `Optional`. Any other value, such as a nested bean, is handed to the configured
JSON object mapper, the way `body(Object)` already serializes a bean. With no JSON object mapper on the classpath, that
value fails with the same error `body(Object)` gives today. A characterization test with a nested bean, a
`Date` and an enum pins the difference, and the changelog names it.

**Pretty printing** for logging and `prettyPrint()` moves to a JSON token reformatter and a small DOM printer in core
that follows today's `XmlNodePrinter` rules, with HTML read by TagSoup. Characterization tests must show identical
output.

### New public names

These names are fixed by this ADR. Signatures named here are part of the decision. Method lists marked "shape" are
checked against the existing behavior in the PR that adds them, and any change comes back for approval.

**Shipping in 6.3.0** (additions next to the deprecated HttpClient 4 members, frozen from then on):

| Name | Where |
|---|---|
| `GsonObjectMapperFactory`, `Jackson2ObjectMapperFactory`, `Jackson3ObjectMapperFactory`, `JohnzonObjectMapperFactory`, `JsonbObjectMapperFactory`, `JAXBObjectMapperFactory`, `JakartaEEObjectMapperFactory` | `io.restassured.common.mapper.factory`, same simple names as today |
| `DefaultGsonObjectMapperFactory`, `DefaultJackson2ObjectMapperFactory`, `DefaultJackson3ObjectMapperFactory`, `DefaultJohnzonObjectMapperFactory`, `DefaultYassonObjectMapperFactory`, `DefaultJAXBObjectMapperFactory`, `DefaultJakartaEEObjectMapperFactory` | same |
| `NumberReturnType` (`FLOAT_AND_DOUBLE`, `BIG_DECIMAL`, `DOUBLE`, `BIG_INTEGER`, as today) | `io.restassured.common.config.NumberReturnType`, a top-level enum |
| `JsonConfig.DEFAULT_NUMBER_LENGTH_LIMIT` (1000, as `JsonPathConfig`'s) | `io.restassured.config.JsonConfig`. Core only uses the constant for its own defaults, so it gets its own copy instead of moving it to common |
| `AuthenticationContext`. Shape: `header(String, String)`, `basic(String, String)`, `ntlm(String, String, String, String)`, `oauth(..)` and `oauth2(..)` as `AuthConfig` has them today, and `certificate(..)` taking the JSSE types below | `io.restassured.authentication` |
| `AuthenticationScheme.authenticate(AuthenticationContext)`, a default method that throws in 6.x and is the abstract method in 7.0 | `io.restassured.authentication.AuthenticationScheme` |
| `ContextAuthenticationScheme`, the sub-interface whose default `authenticate(HTTPBuilder)` builds the context and calls the new method. Deprecated in 7.0 | `io.restassured.authentication` |
| `MultipartMode` (`STRICT`, `BROWSER_COMPATIBLE`, `RFC6532`) | `io.restassured.config` |
| `HttpClientConfig.multipartMode(MultipartMode)` and `multipartMode()` | replaces `httpMultipartMode(..)`. A new name, because the getter can't keep its name with a different return type in 6.3.0 |
| `SSLConfig.sslContext(SSLContext)`, `getSSLContext()`, `hostnameVerifier(HostnameVerifier)`, `getHostnameVerifier()` | `io.restassured.config.SSLConfig`. `allowAllHostnames()` and `strictHostnames()` stay |
| `CertificateAuthSettings.sslContext(SSLContext)`, `getSSLContext()`, `hostnameVerifier(HostnameVerifier)`, `getHostnameVerifier()` | `io.restassured.authentication.CertificateAuthSettings` |
| `CertAuthScheme.getSslContext()`, `setSslContext(SSLContext)`, `getHostnameVerifier()`, `setHostnameVerifier(HostnameVerifier)` | `io.restassured.authentication.CertAuthScheme`, bean style like its other accessors |
| `CookieFilter.getCookies()` returning `io.restassured.http.Cookies`, a snapshot of the filter's store, and `CookieFilter.addCookie(io.restassured.http.Cookie)` | `io.restassured.filter.cookie.CookieFilter` |
| `HttpClientConfig.connectTimeout(Duration)`, `readTimeout(Duration)`, `connectionRequestTimeout(Duration)`, with getters of the same names returning `Duration`, `null` when unset | `io.restassured.config.HttpClientConfig` |
| `HttpClientConfig.keepResponseCookies(boolean)` and `keepsResponseCookies()` | same |
| `HttpClientConfig.cookieDatePatterns(String...)` and `cookieDatePatterns()` returning `List<String>`, with today's two patterns as the default | same |
| `HttpClientConfig.expectContinue(boolean)` and `expectsContinue()` | same |
| `RedirectConfig.followRedirectsForAllMethods(boolean)` and `followsRedirectsForAllMethods()` | `io.restassured.config.RedirectConfig` |
| `json-path-gpath` and `xml-path-gpath`, as relocation poms to `json-path` and `xml-path` 6.3.x | Maven coordinates under `io.rest-assured` |
| `rest-assured-openrewrite`, and the recipe `io.restassured.MigrateRestAssured6Deprecations` | Maven coordinates, and the recipe name |

**Shipping in 7.0.0:**

| Name | Where |
|---|---|
| `http-client-jdk`, `http-client-apache5`, `json-path-jayway`, and `json-path-gpath`, `xml-path-gpath` as jars | Maven coordinates (ADR 0002) |
| `HttpClientType` (`JDK`, `APACHE5`) | `io.restassured.config.HttpClientType` |
| `HttpClientConfig.client(HttpClientType)`, `client(String)` and `client(Class<? extends ..>)`, bounded by the HTTP SPI's provider type | `io.restassured.config.HttpClientConfig` |
| `HttpClientConfig.customizeClient(Class<B> builderType, Consumer<B> customizer)` | same. No HttpClient 5 type in core or common |
| The system property `restassured.http.client`, with the values `jdk` and `apache5` | |
| `JsonPathEngine` (`GPATH`, `JAYWAY`) and `JsonConfig.pathEngine(JsonPathEngine)` | `io.restassured.config`. JSON only, so an XPath module later gets its own enum on `XmlConfig` |
| `ResponseSpecBuilder.setConfig(RestAssuredConfig)` | `io.restassured.builder.ResponseSpecBuilder` |
| `JaywayPath.from(String)`, returning Jayway's `DocumentContext` configured like the module | `io.restassured.path.jayway.JaywayPath` |
| `RestAssuredClient`, its builder, and `RestAssured.client()` | `io.restassured` |
| `RestAssuredMockMvc.client(..)` and `RestAssuredWebTestClient.client(..)` | the existing classes |
| `ConnectionTimeoutException`, `ResponseTimeoutException` (both extending `java.net.SocketTimeoutException`) and `TooManyRedirectsException` (an `IOException`) | `io.restassured.common.exception` |
| The HTTP SPI package `io.restassured.common.spi.http`, and the internal path SPI package `io.restassured.internal.common.spi.path` | `rest-assured-common` (ADR 0002) |
| The recipe `io.restassured.UpgradeToRestAssured7` | `rest-assured-openrewrite` |
| The version files `META-INF/rest-assured/<artifact>.properties` | every participating artifact (ADR 0002) |

The HTTP SPI's own type names are settled in the PR that moves the internal transport interface to common, and come
back for approval there, since the SPI is experimental in 7.0.

### Deprecations, removals and the release train

**Only 6.3.0 ships before 7.0.** It adds the replacements above and deprecates the old members, with no internal
refactoring. The `6.x` branch is cut from the 6.3.0 tag and takes security and bug fixes only, and master becomes
7.0.0-SNAPSHOT. In 6.x, `json-path` and `xml-path` still come with `rest-assured`.

**The HttpClient-4-typed members get replacements in 6.3.0 where one is possible, are deprecated there, and are removed
in 7.0.0.** That covers the 19 methods in `HttpClientConfig`, `SSLConfig`, `CertAuthScheme`, `CertificateAuthSettings`,
`CookieFilter` and `QueryableRequestSpecification`, `HttpClientFactory`, and `authenticate(HTTPBuilder)`.

**The string-parameter methods** (`HttpClientConfig(Map)`, `params()`, `setParam`, `withParams`, `setParams`,
`addParams`) are deprecated in 6.3.0, kept in 7.x and removed in 8.0. From 7.0 core translates these keys when the value
is a String, number, boolean or list of Strings, and any other key or value fails, naming the replacement:

| Key | Replacement |
|---|---|
| `http.connection.timeout` | `connectTimeout(Duration)` |
| `http.socket.timeout` | `readTimeout(Duration)` |
| `http.conn-manager.timeout` | `connectionRequestTimeout(Duration)` |
| `http.protocol.cookie-policy` | `keepResponseCookies(boolean)`: `ignoreCookies` means false, every other policy true |
| `http.protocol.cookie-datepatterns` | `cookieDatePatterns(String...)` |
| `http.useragent` | a `User-Agent` header on the request specification |
| `http.protocol.expect-continue` | `expectContinue(boolean)` |
| `http.protocol.handle-redirects`, `max-redirects`, `allow-circular-redirects`, `reject-relative-redirect` | `RedirectConfig`. The parameter wins, as today |

`http.default-headers` and `http.default-proxy` hold HttpClient 4 values and aren't translated.

### OpenRewrite

A new module, `rest-assured-openrewrite`, holds the migration recipes. No other artifact depends on it.

- 6.3.0 ships `io.restassured.MigrateRestAssured6Deprecations`. It moves code to the 6.3.0 replacements so it still
  compiles on 6.3.0: the moved factory and `NumberReturnType` types, the HttpClient 4 methods with a one-to-one
  replacement, and the string keys above.
- 7.0.0 ships `io.restassured.UpgradeToRestAssured7`. It runs the first recipe, then changes Maven and Gradle builds:
  the version, `json-path` and `xml-path` renamed to the `-gpath` names, and `json-path-gpath` added in test scope where
  code uses paths or calls `jsonPath()`.

Recipes are declarative YAML where possible and small Java recipes where not. Custom HttpClient 4 factories and
authentication schemes, and the JDK client's differences, stay manual and are in the upgrade notes.

### Documentation

A new ReleaseNotes70 wiki page opens with "Upgrading to 7.0": the missing-engine snippets, the OpenRewrite commands and
the `Response` reflection note, under a "Path engines" heading that the error message links to. The wiki Usage page
covers every user-visible 7.0 change, with "Path engines" near the top. The README gets a news item, and every breaking
change gets a changelog entry starting "Non-backward compatible change: ". Wiki changes are delivered as git patches
against `rest-assured.wiki.git`.

## Consequences

- `rest-assured` alone brings no Groovy and no path engine. Every user who asserts on a path adds one dependency, which
  the first failing path names, and which the OpenRewrite recipe can add.
- Users upgrading from 6.x who add `json-path-gpath` keep their paths, `jsonPath()` and `xmlPath()` unchanged.
- Code that calls `jsonPath()`, `xmlPath()` or `htmlPath()` doesn't compile without the GPath module ("cannot access
  JsonPath"). Reflection over all of `Response`'s methods throws `NoClassDefFoundError` without it, so Mockito mocks of
  `Response` and Groovy or Spock dynamic calls need GPath on the test classpath. The upgrade notes say so.
- Passing json-path's `JsonPathConfig.NumberReturnType` to core's config stops compiling in 7.0, and so does storing a
  factory getter's result in a json-path-typed variable. 6.3.0 adds the replacements, and the recipe applies them.
- Core's own tests that use Groovy types depend on the GPath modules in test scope, as ADR 0001 foresaw.
- The names above can't change after the release that ships them without a deprecation cycle.
