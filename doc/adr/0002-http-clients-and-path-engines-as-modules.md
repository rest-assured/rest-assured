# 2. HTTP clients and path engines as modules

Date: 2026-10-17

## Status

Proposed, for 6.3.0 and 7.0.0.

## Context

REST Assured 6.x sends every request through Apache HttpClient 4.5, which is no longer developed (#1862, #1696).
HttpClient 4 types appear in 19 public methods (`HttpClientConfig`, `SSLConfig`, `CertAuthScheme`,
`CertificateAuthSettings`, `CookieFilter`, `QueryableRequestSpecification`) and in the `AuthenticationScheme`
interface, and core uses its deprecated API (`DefaultHttpClient`, string parameters, the per-client SSL registry).

Paths are evaluated by GPath, which needs Groovy. Since ADR 0001 core has no Groovy code of its own, but every user
still gets Groovy through `json-path` and `xml-path`. Some users would rather write JSONPath (`$.store.book[*].title`)
with Jayway, which Spring Boot already puts on their classpath.

We want users to choose the HTTP client and the path engine by adding a dependency, without an API break for those who
change nothing.

## Decision

**Each HTTP client and each path engine is a separate module** (design A). `rest-assured` depends on exactly one
client module, the default, and on the GPath modules only as optional dependencies (ADR 0006).

| Artifact | Package | Directory |
|---|---|---|
| `http-client-jdk` | `io.restassured.http.jdk` | `modules/http-client-jdk` |
| `http-client-apache5` | `io.restassured.http.apache5` | `modules/http-client-apache5` |
| `json-path-gpath` | `io.restassured.path.json` (unchanged) | `json-path-gpath` |
| `xml-path-gpath` | `io.restassured.path.xml` (unchanged) | `xml-path-gpath` |
| `json-path-jayway` | `io.restassured.path.jayway` | `modules/json-path-jayway` |

**Names.** Artifacts are named `<what>-<implementation>` under `io.rest-assured`, with no `rest-assured-` prefix and no
`-module` suffix. Existing add-on modules keep their names. In 7.x, `json-path` and `xml-path` are Maven relocation
poms pointing to the `-gpath` artifact of the same version, with a message saying they are removed in 8.0. A relocation
pom, unlike a pom-only alias with a dependency, resolves in Maven without `<type>pom</type>` and in Gradle. In 6.3.x,
`json-path-gpath` and `xml-path-gpath` are relocation poms pointing the other way, so users can switch coordinates
before 7.0.

**The default client is `http-client-jdk`**, over `java.net.http.HttpClient`. `rest-assured` depends on it in `runtime`
scope and loads it by class name. It brings no third-party jars, so it can't clash with the versions Spring Boot
manages, and Spring applications keep their own HTTP client in tests.

**Picking the client follows a fixed, documented order**, like the JSON object mapper order in `ObjectMapping`:

1. An explicit `httpClientConfig().client(..)`, set statically, per request or on a `RestAssuredClient`.
2. The system property `restassured.http.client` (`jdk`, `apache5`, or a third-party client's name).
3. `http-client-apache5`, if it is on the classpath.
4. `http-client-jdk`, if it is on the classpath.
5. None: an error naming the module to add.

A third-party client module found through `ServiceLoader` is used only when step 1 or 2 names it, so a jar arriving
transitively can't switch a test suite silently. `client(..)` takes `HttpClientType.JDK` or `HttpClientType.APACHE5`,
a client's name, or its provider class.

**No HttpClient 5 type in `rest-assured` or `rest-assured-common`.** Users customize a client module's builder through
`httpClientConfig().customizeClient(Class<B> builderType, Consumer<B> customizer)`, for example with
`HttpClientBuilder.class`. The Maven dependency graph then keeps HttpClient 5 out of core, and the generic method is
still type-safe. A customizer for a client that isn't in use fails with a message naming both.

**Picking the JSON path engine follows a fixed order too, GPath first.** An explicit `jsonConfig().pathEngine(..)`,
set globally or on a request or response specification, always wins. Otherwise one engine module means that one, both
mean GPath with one INFO message naming how to switch, and none means the error in ADR 0006. Only REST Assured's
modules count: Jayway's own `com.jayway.jsonpath:json-path` on the classpath changes nothing. No path is routed by its
syntax, because GPath accepts a leading `$` too.

**Discovery runs at the first request that goes over the network**, never when classes load, so MockMvc and
WebTestClient users never trigger it. REST Assured finds its own modules by class name and third-party ones through
`ServiceLoader`. It logs one message, once, through `System.Logger`, naming the chosen client, how it was chosen and
any third-party module found but not used: at DEBUG for the default `http-client-jdk`, at INFO otherwise.

**The SPIs live in `rest-assured-common`**, so the modules depend only on common and the default module doesn't create
a Maven cycle with core:

- The HTTP SPI is public, in `io.restassured.common.spi.http`, marked experimental in 7.0. Third parties can implement
  a client.
- The path SPI is internal, in `io.restassured.internal.common.spi.path`. 7.0 has no third-party path engines.

Both use neutral value types with `equals` and `hashCode`, and never a client library's or Groovy's types. Common
already exports both package trees and core's bundle excludes them.

**Versions are checked before any provider class loads.** Each SPI has a revision number in common. Every REST Assured
artifact that takes part carries a generated `META-INF/rest-assured/<artifact>.properties` file with its version and
the SPI revisions it was built against. Core reads these files, and provider class names from `META-INF/services` as
text, so a mismatch gives a message instead of a `NoClassDefFoundError`:

- `rest-assured-common` is checked first, in the static initializers of `RestAssured`, `RestAssuredMockMvc` and
  `RestAssuredWebTestClient`, by a class that uses only JDK types. A different major or minor version fails and names
  Spring Boot 3's `rest-assured.version` property, because Boot 3's BOM pins common, json-path and xml-path.
- REST Assured's own modules are checked when found. A different major or minor version fails, a different patch
  version warns once. For third-party clients only the SPI revision is compared.
- Under OSGi the resource checks are skipped, and bnd's version ranges on the common package imports do the job.

**Minimum library versions are HttpClient 5 5.2.1 and Jayway 2.8.0**, what Spring Boot 3.2 manages, because Boot's
BOM overrides the versions a module declares. Each module is compiled against its minimum in CI and checks the version
when loaded, by feature first, then from HttpClient 5's `VersionInfo` or the `Bundle-Version` of Jayway's jar. Too old
fails. Unreadable warns once. The phase 0 spike compiled the apache5 adapter against 5.2.1 with every API it needs, and
ran the same classes on 5.6.5. Its TLS setup is deprecated from 5.4 in favor of `TlsSocketStrategy`, which doesn't exist
before 5.4, so the module uses the deprecated API until its minimum reaches 5.4.

**OSGi.** Every module is a bundle that exports only its own packages and declares `Provide-Capability:
osgi.serviceloader`. Core's `Export-Package` excludes the module packages with exact patterns, `!io.restassured.http.jdk.*`,
`!io.restassured.http.apache5.*` and `!io.restassured.path.jayway.*`, since bnd would otherwise copy them into core's
jar. A broader `!io.restassured.http.*` would drop core's own `io.restassured.http` package, so a module keeps all its
classes, internal ones included, under its own package and never under `io.restassured.internal.http`. Core imports `io.restassured.http.jdk`, imports the apache5, GPath and Jayway provider packages and TagSoup
with `resolution:=optional`, and adds `DynamicImport-Package` for the provider packages. For third-party clients
through SPI Fly, core declares the `osgi.serviceloader` requirement and the extender requirement, both with
`resolution:=optional`, so core resolves without SPI Fly.

**`rest-assured-all`** shades `rest-assured-common`, `json-path-gpath` and `xml-path-gpath` with a
`ServicesResourceTransformer`, as it shades common, json-path and xml-path today. It carries Groovy and says so in its
description. No client or Jayway module goes into it, because core isn't in it and couldn't discover them there.

## Consequences

- Adding or removing a dependency is the switch. Users who want HttpClient 5 add `http-client-apache5`, and it wins
  over the JDK module. When it arrives transitively, the INFO message shows that it is used.
- About 20 public HTTP SPI types must stay stable across 7.x, with the SPI revision as the escape hatch.
- All REST Assured artifacts must be on one version. The BOM and the version check make that visible.
- The release has six more artifacts and two relocation poms. Each module needs a parent pom entry, bundle setup,
  `META-INF/services` and version files, a BOM entry, a `dist` assembly, javadoc and a README line.
- There is no legacy HttpClient 4 module in 7.0.0, because #1862 asks for HttpClient 4 to leave every artifact.
- `http-client-apache5` on the classpath also changes Spring's own `RestClient` in tests, which then retries a 503 or 429
  once. REST Assured doesn't touch the application's client, and the docs name the fix.
