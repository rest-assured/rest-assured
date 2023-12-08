![REST Assured](rest-assured-logo-green.png)

[![Build Status](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/rest-assured/rest-assured/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.rest-assured/rest-assured)
<!-- [![Javadoc](https://javadoc-badge.appspot.com/io.rest-assured/rest-assured.svg)](http://www.javadoc.io/doc/io.rest-assured/rest-assured) -->


Testing and validation of REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News
* 2023-12-08: REST Assured 5.4.0 is released with several bug fixes and a [Kotlin Extension Module for Spring WebTest]([Kotlin#kotlin-extension-module-for-spring-webtest](https://github.com/rest-assured/rest-assured/wiki/Kotlin#kotlin-extension-module-for-spring-webtest)). See [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2023-09-08: REST Assured 5.3.2 is released with several bug fixes, better Spring MockMvc Support, better support for newer versions of Spring Security etc. See [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.
* 2023-06-16: REST Assured 5.3.1 is released with several bug fixes and dependency upgrades. See [change log](https://raw.githubusercontent.com/rest-assured/rest-assured/master/changelog.txt) for more details.

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
* [Javadoc](http://www.javadoc.io/doc/io.rest-assured/rest-assured/5.4.0)
* [Rest Assured Javadoc](http://static.javadoc.io/io.rest-assured/rest-assured/5.4.0/io/restassured/RestAssured.html)
* [Rest AssuredMockMvc Javadoc](http://static.javadoc.io/io.rest-assured/spring-mock-mvc/5.4.0/io/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://static.javadoc.io/io.rest-assured/xml-path/5.4.0/io/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://static.javadoc.io/io.rest-assured/json-path/5.4.0/io/restassured/path/json/JsonPath.html)
* [Release Notes](https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes)
* [FAQ](https://github.com/rest-assured/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Links
* [Change log](https://github.com/rest-assured/rest-assured/raw/master/changelog.txt)
* REST Assured on [openhub](https://www.openhub.net/p/rest-assured)
* [Mailing list](http://groups.google.com/group/rest-assured) for questions and support

<a href="https://www.buymeacoffee.com/johanhaleby" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/arial-blue.png" alt="Buy Me A Coffee" style="height: 42px !important;width: 180px !important;" height="42px" width="180px"></a>
