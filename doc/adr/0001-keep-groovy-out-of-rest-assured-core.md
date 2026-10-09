# 1. Keep Groovy out of rest-assured core

Date: 2026-10-09

## Status

Accepted. Implemented in #1914 to #1932, for 7.0.0.

## Context

For 7.0.0 we want to make the assertion library and the HTTP client pluggable. Both are hard to do while the
`rest-assured` module is written in Groovy. Before this decision, core had about 8,000 lines of Groovy in 62 files, including
the center of the request/response stack (`RequestSpecificationImpl`, `ResponseSpecificationImpl`,
`SpecificationMerger`), the closures passed to the forked `HTTPBuilder`, the authentication schemes and several
matchers. `rest-assured-common` had about 460 lines of Groovy.

Groovy made the code hard to change safely:

- Overloads are picked at runtime from the argument types, so the method that runs isn't the one the code seems to
  call. One port found that `multiPart(String, Object)` sent a `File` to the `File` overload only because Groovy
  checked the type at runtime.
- Groovy truth, `==`, implicit `toString()` of maps and arrays (`[a:1]` instead of `{a=1}`) and implicit String
  coercion all leak into user-visible output.
- Checked exceptions are thrown without being declared, and some calls only fail at runtime, for example named-argument
  constructors or a method that doesn't exist. Several such latent errors were found during the port, such as
  `keyStore(KeyStore)` on a request specification, which always threw `MissingMethodException`.
- Java code can't call into Groovy closures or Groovy-only types without depending on Groovy itself.

GPath is the one feature that really needs Groovy. `json-path` and `xml-path` compile and run each path expression as
a Groovy script, and core uses them for `body("a.b", ..)`, `path(..)` and the form authentication and CSRF filters.

## Decision

A module carries Groovy only if it evaluates GPath.

- `json-path` and `xml-path` keep Groovy.
- `rest-assured-common` and `rest-assured` have no Groovy sources, no direct Groovy dependency and no gmavenplus.
  Groovy reaches core only through `json-path` and `xml-path`.
- A `NoGroovyBytecodeTest` in each of those two modules scans the compiled main classes and fails if any of them refers
  to `groovy/`, `org/codehaus/groovy/` or `org/apache/groovy/`.
- Core recognizes a GString by its class name (`GroovyTypes`), walking the superclass chain, so it handles GStrings from
  Groovy and Kotlin users without a dependency on Groovy.

The port preserves behavior:

- Every rewritten class got characterization tests first, run against the Groovy code, and the Java port had to pass
  the same tests.
- A quirk that Java can express was kept and listed as an issue candidate rather than fixed in the port.
- A latent runtime error, such as a `MissingMethodException` or an "ambiguous method overloading" error, was fixed, with
  a test and a changelog entry.
- Where Groovy semantics are user-visible, small internal helpers reproduce them: `GroovyStyleToString` for `toString()`
  of maps, collections and arrays, `GroovyStringConversion` for `as String`, `GroovyStyleEquality` for `==` and
  `GroovyStyleHashCode` for `@Canonical` hash codes. Each one is tested against Groovy itself.
- `HTTPBuilder` stays forked. Its `Closure` parameters became Java functional interfaces. Replacing it is part of the
  pluggable HTTP client work.
- The public API stayed the same, with one exception: public classes that were written in Groovy no longer implement
  `groovy.lang.GroovyObject`.

The work was split into six phases, each merged on its own when CI was green: `rest-assured-common`, response parsing,
`HTTPBuilder`, the leaf classes, the request/response stack (in six layers, from the response options to
`SpecificationMerger`), and finally the build.

## Consequences

- Core can be read and changed as plain Java. Overload choice, equality and exceptions follow the Java rules, and the
  compiler catches what used to fail at runtime.
- Users still get the same Groovy jars on the classpath, through `json-path` and `xml-path`. Dropping Groovy entirely
  depends on making the assertion library pluggable, which is a later 7.0.0 step.
- Several error messages and exception types changed. Most of them used to be an obscure Groovy exception, such as
  `MissingMethodException` or `GroovyRuntimeException`, and are now an `IllegalArgumentException` with a message that
  says what's wrong. Each one is listed in the changelog as a non-backward compatible change.
- A Groovy `Closure` returned from a custom `ObjectMapper` is no longer run as a request body builder. It fails with an
  `IllegalArgumentException`.
- The Groovy-style helpers keep Groovy's behavior where users could see it. They can be removed when that behavior is
  allowed to change, for example in a major version.
- About 20 of core's tests use Groovy types, for example to check GString handling. They compile only because
  `json-path` brings in Groovy. If `json-path` ever stops depending on Groovy, core needs a test-scoped Groovy
  dependency.
- The quirks that were kept were collected during the port, and the ones worth fixing were filed as issues.
