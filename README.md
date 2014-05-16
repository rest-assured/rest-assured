# REST-assured [![Build Status](https://travis-ci.org/tguzik/rest-assured.svg?branch=test-travis)](https://travis-ci.org/tguzik/rest-assured)

Testing and validating REST services in Java is harder than in dynamic languages 
such as Ruby and Groovy. REST Assured brings the simplicity of using these 
languages into the Java domain.


## News 

* 2014-05-14: REST Assured was be presented at Geecon in Krakow.
* 2014-03-31: REST Assured 2.3.1 is released with support for logging if validation 
  fails, root detach as well as bug fixes and other improvements. See change log for 
  more info on what has changed in this release.
* 2014-02-06: REST Assured will be presented at Geecon in Krakow in May.

[older news](https://code.google.com/p/rest-assured/wiki/OldNews)

## Examples
Here's an example of how to make a GET request and validate the JSON or XML response:

    get("/lotto").then().assertThat().body("lotto.lottoId", equalTo(5));


Get and verify all winner ids:

    get("/lotto").then().assertThat().body("lotto.winners.winnerId", hasItems(23, 54));


Using parameters:

    given().
            param("key1", "value1").
            param("key2", "value2").
    when().
            post("/somewhere").
    then().
            body(containsString("OK"));


Using X-Path (XML only):

    given().
            parameters("firstName", "John", "lastName", "Doe").
    when().
            post("/greetMe").
    then().
            body(hasXPath("/greeting/firstName[text()='John']")).


Need authentication? REST Assured provides several authentication mechanisms:

    given().auth().basic(username, password).when().get("/secured").then().statusCode(200);


Getting and parsing a response body:

    // Example with JsonPath
    String json = get("/lotto").asString()
    List<String> winnderIds = from(json).get("lotto.winners.winnerId");
    
    // Example with XmlPath
    String xml = post("/shopping").andReturn().body().asString()
    Node category = from(xml).get("shopping.category[0]");


REST Assured supports the POST, GET, PUT, DELETE, OPTIONS, PATCH and HEAD http 
methods and includes specifying and validating e.g. parameters, headers, cookies 
and body easily.


## Documentation

* [Getting started](https://code.google.com/p/rest-assured/wiki/GettingStarted)
* [Usage Guide](https://code.google.com/p/rest-assured/wiki/Usage) (click [here](https://code.google.com/p/rest-assured/wiki/Usage_Legacy) for legacy documentation)
* [Rest Assured Javadoc](http://rest-assured.googlecode.com/svn/tags/2.3.1/apidocs/com/jayway/restassured/RestAssured.html)
* [Rest Assured Mock Mvc Javadoc](http://rest-assured.googlecode.com/svn/tags/2.3.1/apidocs/com/jayway/restassured/module/mockmvc/RestAssuredMockMvc.html)
* [XmlPath Javadoc](http://rest-assured.googlecode.com/svn/tags/2.3.1/apidocs/com/jayway/restassured/path/xml/XmlPath.html)
* [JsonPath Javadoc](http://rest-assured.googlecode.com/svn/tags/2.3.1/apidocs/com/jayway/restassured/path/json/JsonPath.html)
* [FAQ](https://code.google.com/p/rest-assured/wiki/FAQ)

## Support and discussion
Join the mailing list at our [Google group](http://groups.google.com/group/rest-assured). 

## Founded by:
[![JAYWAY](http://www.arctiquator.com/oppenkallkod/assets/images/jayway_logo.png)](http://www.jayway.com/)

## Other open source projects:
[![PowerMock](http://powermock.googlecode.com/svn/trunk/src/site/resources/images/logos/powermock.png)](http://www.powermock.org/)
[![Awaitility](http://github.com/jayway/awaitility/raw/master/resources/Awaitility_logo_red_small.png)](http://code.google.com/p/awaitility)

