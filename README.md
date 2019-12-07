![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://travis-ci.org/rest-assured/rest-assured.svg)](https://travis-ci.org/rest-assured/rest-assured)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured)
[![Javadoc](https://javadoc-badge.appspot.com/io.rest-assured/rest-assured.svg)](http://www.javadoc.io/doc/io.rest-assured/rest-assured)


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 
* 2019-10-02: REST Assured [4.1.2](http://dl.bintray.com/johanhaleby/generic/rest-assured-4.1.2-dist.zip) is released. It adds support for Java 13 as well as fixing some issues with the [kotlin extension module](https://github.com/rest-assured/rest-assured/wiki/Usage#kotlin-extension-module). Please see [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for details.
* 2019-09-06: Johan elaborates on some of the benefits of using the new [Kotlin API](https://github.com/rest-assured/rest-assured/wiki/Usage#kotlin-extension-module) in [this](http://code.haleby.se/2019/09/06/rest-assured-in-kotlin/) blog post. 
* 2019-09-06: REST Assured [4.1.1](http://dl.bintray.com/johanhaleby/generic/rest-assured-4.1.1-dist.zip) is released. This is a very small release containing updates to jaxb-osgi and jaxb-apo dependencies which could lead to classpath inconsistencies if using newer versions of jaxb in Java 9+. Please see [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for details.

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
* [Javadoc](http://www.javadoc.io/doc/io.rest-assured/rest-assured/4.1.2)
* [Rest Assured Javadoc](http://static.javadoc.io/io.rest-assured/rest-assured/4.1.2/io/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/io.rest-assured/spring-mock-mvc/4.1.0/io/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/io.rest-assured/xml-path/4.1.2/io/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/io.rest-assured/json-path/4.1.2/io/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/rest-assured/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/rest-assured/rest-assured/raw/master/changelog.txt)
* REST Assured on [openhub](https://www.openhub.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support
