![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://travis-ci.org/rest-assured/rest-assured.svg)](https://travis-ci.org/rest-assured/rest-assured)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/io.rest-assured/rest-assured/badge.svg)](http://www.javadoc.io/doc/io.rest-assured/rest-assured)


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 
* 2016-09-21: REST Assured [3.0.1](http://dl.bintray.com/johanhaleby/generic/rest-assured-3.0.1-dist.zip) is released. This is a maintence release with several bug fixes. See [change log](https://raw.githubusercontent.com/jayway/rest-assured/master/changelog.txt) for details.
* 2016-06-03: REST Assured [3.0.0](http://dl.bintray.com/johanhaleby/generic/rest-assured-3.0.0-dist.zip) is released. This is a new major release with lots of updates and new features such as the ability to use [any HTTP method](https://github.com/rest-assured/rest-assured/wiki/Usage#invoking-http-resources), all HTTP methods now supports multipart uploads, improved error messages, improved JsonPath etc. **Note** The package name has changed to `io.restassured` and the groupId has changed to `io.rest-assured`. Please see [release notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes30) and [getting started guide](https://github.com/rest-assured/rest-assured/wiki/GettingStarted) for more details.

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
String json = get("/lotto").asString()
List<String> winnderIds = from(json).get("lotto.winners.winnerId");
    
// Example with XmlPath
String xml = post("/shopping").andReturn().body().asString()
Node category = from(xml).get("shopping.category[0]");
```

REST Assured supports any HTTP method but has explicit support for *POST*, *GET*, *PUT*, *DELETE*, *OPTIONS*, *PATCH* and *HEAD* and includes specifying and validating e.g. parameters, headers, cookies and body easily.


## Documentation

* [Getting started](https://github.com/rest-assured/rest-assured/wiki/GettingStarted)
* [Downloads](https://github.com/rest-assured/rest-assured/wiki/Downloads)
* [Usage Guide](https://github.com/rest-assured/rest-assured/wiki/Usage) (click [here](https://github.com/rest-assured/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/io.rest-assured/rest-assured/3.0.0)
* [Rest Assured Javadoc](http://static.javadoc.io/io.rest-assured/rest-assured/3.0.0/io/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/io.rest-assured/spring-mock-mvc/3.0.0/io/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/io.rest-assured/xml-path/3.0.0/io/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/io.rest-assured/json-path/3.0.0/io/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/rest-assured/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/rest-assured/rest-assured/raw/master/changelog.txt)
* REST Assured on [Ohloh](https://www.ohloh.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support
