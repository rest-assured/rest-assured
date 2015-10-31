![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://travis-ci.org/jayway/rest-assured.svg)](https://travis-ci.org/jayway/rest-assured)![tag](http://img.shields.io/github/tag/jayway/rest-assured.svg)


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 
* 2015-10-31: REST Assured [2.7.0](http://dl.bintray.com/johanhaleby/generic/rest-assured-2.7.0-dist.zip) is released with support for using [proxy authentication](https://github.com/jayway/rest-assured/wiki/Usage#proxy-configuration), support for sending a body in a GET request, updates to [FilterContext](http://static.javadoc.io/com.jayway.restassured/rest-assured/2.7.0/com/jayway/restassured/filter/FilterContext.html) which allows future integration with [spring-rest-docs](https://github.com/spring-projects/spring-restdocs), improvements to the [Spring Mock Mvc module](https://github.com/jayway/rest-assured/wiki/Usage#spring-mock-mvc-module) as well as other fixes and improvements. This release is not 100% backward compatible so please see [release notes](https://github.com/jayway/rest-assured/wiki/ReleaseNotes27) for more details.
* 2015-10-09: REST Assured [2.6.0](http://dl.bintray.com/johanhaleby/generic/rest-assured-2.6.0-dist.zip) is released with support for using [mapping functions](https://github.com/jayway/rest-assured/wiki/Usage#headers-1) in header validation, ability to specify if parameters should be [merged or replaced](https://github.com/jayway/rest-assured/wiki/Usage#param-config), better support for [multiparts](https://github.com/jayway/rest-assured/wiki/ReleaseNotes26#other-notable-changes), fixed [problems](https://github.com/jayway/rest-assured/wiki/ReleaseNotes26#non-backward-compatible-changes) with [XML namespaces](https://github.com/jayway/rest-assured/wiki/Usage#xml-namespaces), new [Scala module](https://github.com/jayway/rest-assured/wiki/Usage#scala-support-module) and ability to configure which charset to use per content-type basis (both for [encoding](https://github.com/jayway/rest-assured/wiki/Usage#encoder-config) and [decoding](https://github.com/jayway/rest-assured/wiki/Usage#decoder-config)) as well as other bug fixes and improvements. This release is not 100% backward compatible so please see [release notes](https://github.com/jayway/rest-assured/wiki/ReleaseNotes26) for more details.
* 2015-08-09: REST Assured [2.5.0](http://dl.bintray.com/johanhaleby/generic/rest-assured-2.5.0-dist.zip) is released with support for [multiple failure explanations](https://github.com/jayway/rest-assured/wiki/ReleaseNotes25#highlights), improved [OAuth2](https://github.com/jayway/rest-assured/wiki/Usage#oauth-2) support without the need for Scribe, better multipart configuration with support for setting [default control name and filename](https://github.com/jayway/rest-assured/wiki/ReleaseNotes25#other-notable-changes), better Java 8 support and many improvements to the [RestAssuredMockMvc](https://github.com/jayway/rest-assured/wiki/ReleaseNotes25#spring-mock-mvc-module) module. See [release notes](https://github.com/jayway/rest-assured/wiki/ReleaseNotes25) for details.

[Older News](https://github.com/jayway/rest-assured/wiki/OldNews)


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
    parameters("firstName", "John", "lastName", "Doe").
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
String json = get("/lotto").asString()
List<String> winnderIds = from(json).get("lotto.winners.winnerId");
    
// Example with XmlPath
String xml = post("/shopping").andReturn().body().asString()
Node category = from(xml).get("shop2ping.category[0]");
```

REST Assured supports the *POST*, *GET*, *PUT*, *DELETE*, *OPTIONS*, *PATCH* and *HEAD* http 
methods and includes specifying and validating e.g. parameters, headers, cookies 
and body easily.


## Documentation

* [Getting started](https://github.com/jayway/rest-assured/wiki/GettingStarted)
* [Downloads](https://github.com/jayway/rest-assured/wiki/Downloads)
* [Usage Guide](https://github.com/jayway/rest-assured/wiki/Usage) (click [here](https://github.com/jayway/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/com.jayway.restassured/rest-assured/2.7.0)
* [Rest Assured Javadoc](http://static.javadoc.io/com.jayway.restassured/rest-assured/2.7.0/com/jayway/restassured/RestAssured.html)
* [Rest Assured Mock Mvc Javadoc](http://static.javadoc.io/com.jayway.restassured/spring-mock-mvc/2.7.0/com/jayway/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/com.jayway.restassured/xml-path/2.7.0/com/jayway/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/com.jayway.restassured/json-path/2.7.0/com/jayway/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/jayway/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/jayway/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/jayway/rest-assured/raw/master/changelog.txt)
* REST Assured on [Ohloh](https://www.ohloh.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support

## Sponsored by:
[![JAYWAY](http://www.arctiquator.com/oppenkallkod/assets/images/jayway_logo.png)](http://www.jayway.com/)
