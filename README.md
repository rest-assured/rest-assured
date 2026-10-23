![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.sml.io/sonatype-central/io.rest-assured/rest-assured/badge.svg)](https://central.sonatype.com/artifact/io.rest-assured/rest-assured)
<!-- [![Javadoc](https://javadoc-badge.appspot.com/io.rest-assured/rest-assured.svg)](http://www.javadoc.io/doc/io.rest-assured/rest-assured) -->


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News
* 2026-10-23: REST Assured 6.3.0 is released. It prepares for REST Assured 7.0.0, which drops Apache HttpClient 4 and Groovy from the default dependencies. Every API that 7.0.0 removes now has a replacement, the old ones are deprecated, and the new `rest-assured-openrewrite` module has a recipe that moves your code over. Among the replacements are timeouts as `Duration` in `HttpClientConfig`, `SSLContext` support in `SSLConfig` and `followRedirectsForAllMethods(..)` in `RedirectConfig`. Form authentication now also uses the config and proxy of your request for all its requests. See [Preparing for REST Assured 7.0](https://github.com/rest-assured/rest-assured/wiki/PreparingFor70), the [release notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes63) and the [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2026-10-16: REST Assured 6.2.0 is released. The `rest-assured` module is now written entirely in Java and no longer depends on Groovy directly. Tests that run in parallel no longer fail because of races in the static configuration, and `auth().certificate(..)` now sends the client certificate to servers that ask for one (mutual TLS). Many failures that used to show up as Groovy exceptions or a `NullPointerException` now give a clear error message, and `scala-support` is upgraded to a Scala version that fixes CVE-2022-36944. This release contains several non-backward compatible changes, so please read the [release notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes62) before upgrading. See also the [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2026-10-09: REST Assured 6.1.0 is released. It adds support for the HTTP QUERY method, JSON Schema draft-06, draft-07, 2019-09 and 2020-12, and `java.time` values as parameters. Authorization and Cookie headers are no longer sent along when a redirect goes to a different host. There are also many bug fixes, several of them from the community. See [release notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes61) and [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.

[Older News](https://github.com/rest-assured/rest-assured/wiki/OldNews)


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


## Documentation

* [Getting started](https://github.com/rest-assured/rest-assured/wiki/GettingStarted)
* [Usage Guide](https://github.com/rest-assured/rest-assured/wiki/Usage) (click [here](https://github.com/rest-assured/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/io.rest-assured/rest-assured/6.2.0)
* [Rest Assured Javadoc](http://static.javadoc.io/io.rest-assured/rest-assured/6.2.0/io/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/io.rest-assured/spring-mock-mvc/6.2.0/io/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/io.rest-assured/xml-path/6.2.0/io/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/io.rest-assured/json-path/6.2.0/io/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/rest-assured/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/rest-assured/rest-assured/raw/master/changelog.txt)
* REST Assured on [openhub](https://www.openhub.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support

<a href="https://www.buymeacoffee.com/johanhaleby" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/arial-blue.png" alt="Buy Me A Coffee" style="height: 42px !important;width: 180px !important;" height="42px" width="180px"></a>
