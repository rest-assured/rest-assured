![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://travis-ci.org/rest-assured/rest-assured.svg)](https://travis-ci.org/rest-assured/rest-assured)![tag](http://img.shields.io/github/tag/jayway/rest-assured.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.jayway.restassured/rest-assured/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.jayway.restassured/rest-assured)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.jayway.restassured/rest-assured/badge.svg)](http://www.javadoc.io/doc/com.jayway.restassured/rest-assured)


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 
* 2016-06-01: [Spring REST Docs](http://projects.spring.io/spring-restdocs/) 1.1.0 is released with support for REST Assured. Read more about it [here](https://spring.io/blog/2016/05/31/spring-rest-docs-1-1-0-release).
* 2016-03-04: REST Assured [2.9.0](http://dl.bintray.com/johanhaleby/generic/rest-assured-2.9.0-dist.zip) is released. This is essentially a maintenance release but it comes with big changes in how keystores and truststores are used. This release is not 100% backward compatible so please see [release notes](https://github.com/jayway/rest-assured/wiki/ReleaseNotes29) for more details.
* 2016-01-20: [Johan Haleby](http://code.haleby.se/) will present REST Assured at the [JDays](http://www.jdays.se/) conference in Gothenburg at 8th of March.

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

REST Assured supports the *POST*, *GET*, *PUT*, *DELETE*, *OPTIONS*, *PATCH* and *HEAD* http 
methods and includes specifying and validating e.g. parameters, headers, cookies 
and body easily.


## Documentation

* [Getting started](https://github.com/rest-assured/rest-assured/wiki/GettingStarted)
* [Downloads](https://github.com/rest-assured/rest-assured/wiki/Downloads)
* [Usage Guide](https://github.com/rest-assured/rest-assured/wiki/Usage) (click [here](https://github.com/rest-assured/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/com.jayway.restassured/rest-assured/2.8.0)
* [Rest Assured Javadoc](http://static.javadoc.io/com.jayway.restassured/rest-assured/2.8.0/com/jayway/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/com.jayway.restassured/spring-mock-mvc/2.8.0/com/jayway/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/com.jayway.restassured/xml-path/2.8.0/com/jayway/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/com.jayway.restassured/json-path/2.8.0/com/jayway/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/rest-assured/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/jayway/rest-assured/raw/master/changelog.txt)
* REST Assured on [Ohloh](https://www.ohloh.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support
