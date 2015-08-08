![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://travis-ci.org/jayway/rest-assured.svg)](https://travis-ci.org/jayway/rest-assured)![tag](http://img.shields.io/github/tag/jayway/rest-assured.svg)


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 
* 2015-07-22: REST Assured has moved to GitHub. From now on the old <a href="https://code.google.com/p/rest-assured/">Google Code page</a> should not be used anymore. All issues are reported here on GitHub and the documentation is migrated.
* 2015-04-12: REST Assured [2.4.1](http://dl.bintray.com/johanhaleby/generic/rest-assured-2.4.1-dist.zip) is released with bug fixes and improvements. See [change log](https://raw.githubusercontent.com/jayway/rest-assured/master/changelog.txt) for details.
* 2015-01-27: Jakub Czeczotka has written a nice blog post on how to use REST Assured MockMvc, you can read it [here](http://blog.czeczotka.com/2015/01/20/spring-mvc-integration-test-with-rest-assured-and-mockmvc/).

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
Node category = from(xml).get("shopping.category[0]");
```

REST Assured supports the *POST*, *GET*, *PUT*, *DELETE*, *OPTIONS*, *PATCH* and *HEAD* http 
methods and includes specifying and validating e.g. parameters, headers, cookies 
and body easily.


## Documentation

* [Getting started](https://github.com/jayway/rest-assured/wiki/GettingStarted)
* [Downloads](https://github.com/jayway/rest-assured/wiki/Downloads)
* [Usage Guide](https://github.com/jayway/rest-assured/wiki/Usage) (click [here](https://github.com/jayway/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Javadoc](http://www.javadoc.io/doc/com.jayway.restassured/rest-assured/2.4.1)
* [Rest Assured Javadoc](http://static.javadoc.io/com.jayway.restassured/rest-assured/2.4.1/com/jayway/restassured/RestAssured.html)
* [Rest Assured Mock Mvc Javadoc](http://static.javadoc.io/com.jayway.restassured/spring-mock-mvc/2.4.1/com/jayway/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/com.jayway.restassured/xml-path/2.4.1/com/jayway/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/com.jayway.restassured/json-path/2.4.1/com/jayway/restassured/path/json/JsonPath.html)
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
