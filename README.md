![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.sml.io/sonatype-central/io.rest-assured/rest-assured/badge.svg)](https://central.sonatype.com/artifact/io.rest-assured/rest-assured)
<!-- [![Javadoc](https://javadoc-badge.appspot.com/io.rest-assured/rest-assured.svg)](http://www.javadoc.io/doc/io.rest-assured/rest-assured) -->


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News
* 2026-11-12: REST Assured 7.0.0-RC1 is released. It's the release candidate for 7.0.0, a new major release. REST Assured now sends requests with the JDK's HTTP client by default, with Apache HttpClient 5 in the new `http-client-apache5` module, and no longer depends on Groovy. You pick a path engine for `body(..)` paths and `jsonPath()`: `json-path-gpath`, `xml-path-gpath` or `json-path-jayway`. Java 17 is required. This release contains several non-backward compatible changes, so please read the [release notes](https://rest-assured.io/release-notes/7-0/) and [which module do I need](https://rest-assured.io/docs/modules/) before upgrading. The `rest-assured-openrewrite` recipe moves most code over. Please try it and report what breaks! See the [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2026-10-23: REST Assured 6.3.0 is released. It prepares for REST Assured 7.0.0, which drops Apache HttpClient 4 and Groovy from the default dependencies. Every API that 7.0.0 removes now has a replacement, the old ones are deprecated, and the new `rest-assured-openrewrite` module has a recipe that moves your code over. Among the replacements are timeouts as `Duration` in `HttpClientConfig`, `SSLContext` support in `SSLConfig` and `followRedirectsForAllMethods(..)` in `RedirectConfig`. Form authentication now also uses the config and proxy of your request for all its requests. See [Preparing for REST Assured 7.0](https://rest-assured.io/docs/upgrading/preparing-for-7-0/), the [release notes](https://rest-assured.io/release-notes/6-3/) and the [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2026-10-16: REST Assured 6.2.0 is released. The `rest-assured` module is now written entirely in Java and no longer depends on Groovy directly. Tests that run in parallel no longer fail because of races in the static configuration, and `auth().certificate(..)` now sends the client certificate to servers that ask for one (mutual TLS). Many failures that used to show up as Groovy exceptions or a `NullPointerException` now give a clear error message, and `scala-support` is upgraded to a Scala version that fixes CVE-2022-36944. This release contains several non-backward compatible changes, so please read the [release notes](https://rest-assured.io/release-notes/6-2/) before upgrading. See also the [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.

[Older News](https://rest-assured.io/news/#older-news)


## Examples
Here's an example of how to make a GET request and validate the JSON or XML response:

```java
get("/lotto").then().assertThat().body("lotto.lottoId", equalTo(5));
```

Get and verify all winner ids:

```java
get("/lotto").then().assertThat().body("lotto.winners.winnerId", hasItems(23, 54));
```

Using parameters:

```java
given().
    param("key1", "value1").
    param("key2", "value2").
when().
    post("/somewhere").
then().
    body(containsString("OK"));
```

Using X-Path (XML only):

```java
given().
    params("firstName", "John", "lastName", "Doe").
when().
    post("/greetMe").
then().
    body(hasXPath("/greeting/firstName[text()='John']")).
```

Need authentication? REST Assured provides several authentication mechanisms:

```java
given().auth().basic(username, password).when().get("/secured").then().statusCode(200);
```

Getting and parsing a response body:

```java
// Example with JsonPath
String json = get("/lotto").asString();
List<String> winnerIds = from(json).get("lotto.winners.winnerId");
    
// Example with XmlPath
String xml = post("/shopping").andReturn().body().asString();
Node category = from(xml).get("shopping.category[0]");
```

REST Assured supports any HTTP method but has explicit support for *POST*, *GET*, *PUT*, *DELETE*, *OPTIONS*, *PATCH* and *HEAD* and includes specifying and validating e.g. parameters, headers, cookies and body easily.

## Modules
Add `rest-assured` to your test dependencies, and the modules for what your tests do: a path engine (`json-path-gpath`, `xml-path-gpath` or `json-path-jayway`) if they use paths, `http-client-apache5` to send the requests with Apache HttpClient 5 instead of the JDK's HTTP client, `json-schema-validator`, or the Spring, Kotlin and Scala modules. See [Which module do I need](https://rest-assured.io/docs/modules/), and import `io.rest-assured:rest-assured-bom` to keep their versions in line.

## Documentation

The documentation is at [rest-assured.io](https://rest-assured.io/docs/).

* [Getting started](https://rest-assured.io/docs/getting-started/)
* [Which module do I need](https://rest-assured.io/docs/modules/)
* [Usage Guide](https://rest-assured.io/docs/usage/) (click [here](https://rest-assured.io/docs/legacy/usage/) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/io.rest-assured/rest-assured/7.0.0-RC1)
* [Rest Assured Javadoc](http://static.javadoc.io/io.rest-assured/rest-assured/7.0.0-RC1/io/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/io.rest-assured/spring-mock-mvc/7.0.0-RC1/io/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/io.rest-assured/xml-path/7.0.0-RC1/io/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/io.rest-assured/json-path/7.0.0-RC1/io/restassured/path/json/JsonPath.html)
* [Release Notes](https://rest-assured.io/release-notes/)
* [FAQ](https://rest-assured.io/docs/faq/)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/rest-assured/rest-assured/raw/master/changelog.txt)
* REST Assured on [openhub](https://www.openhub.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support

<a href="https://www.buymeacoffee.com/johanhaleby" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/arial-blue.png" alt="Buy Me A Coffee" style="height: 42px !important;width: 180px !important;" height="42px" width="180px"></a>
