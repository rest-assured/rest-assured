# 5. Java 17 stays the baseline for 7.0.0

Date: 2026-10-17

## Status

Accepted, for 6.3.0 and 7.0.0.

## Context

REST Assured 6.x targets Java 17, set with the `maven.compiler.source` and `target` properties and again in the
compiler plugin's configuration (root `pom.xml`), and CI tests only on JDK 17. 7.0.0 could raise the baseline to Java 21 to close the JDK HTTP client with `HttpClient.close()` and use virtual threads.

REST Assured is a test library. Its tests run in the same JVM as the application under test, so the baseline decides
which applications can use it. Spring Framework 7 and Spring Boot 4 are expected to keep Java 17 as their minimum.

## Decision

**Java 17 stays the baseline for 7.0.0.**

- From 6.3.0 the build uses `maven.compiler.release=17` instead of `source` and `target`, in both places, so the
  compiler also checks that no API newer than Java 17 is used.
- From 6.3.0 CI builds and tests on JDK 17, 21 and 25. Version matrices for HttpClient 5 and Jayway run on JDK 17
  only.
- Code that needs a newer JDK API, such as `HttpClient.close()` on Java 21, calls it only when it is available and
  documents what happens on Java 17.

## Consequences

- Every application on Java 17 or later can use 7.0.0.
- On Java 17, JDK HTTP clients can't be closed. A client that isn't shared is left to the garbage collector, and the JDK
  module warns once where that matters (ADR 0003).
- The bytecode level stays 17, so the build change in 6.3.0 is additive for users.
