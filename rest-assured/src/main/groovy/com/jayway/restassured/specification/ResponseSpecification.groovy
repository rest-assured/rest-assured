package com.jayway.restassured.specification

import groovyx.net.http.ContentType
import org.hamcrest.Matcher

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/16/10
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ResponseSpecification extends RequestSender {
  ResponseSpecification content(Matcher<?> matcher, Matcher<?>...additionalMatchers);

  ResponseSpecification content(String key, Matcher<?> matcher, Object...additionalKeyMatcherPairs);

  ResponseSpecification statusCode(Matcher<Integer> expectedStatusCode);

  ResponseSpecification statusCode(int expectedStatusCode);

  ResponseSpecification statusLine(Matcher<String> expectedStatusLine);

  ResponseSpecification headers(Map<String, Object> expectedHeaders);

  ResponseSpecification headers(String firstExpectedHeaderName, Object...expectedHeaders);

  ResponseSpecification header(String headerName, Matcher<String> expectedValueMatcher);

  ResponseSpecification header(String headerName, String expectedValue);

  ResponseSpecification contentType(ContentType contentType);

  ResponseSpecification statusLine(String expectedStatusLine);

  ResponseSpecification body(Matcher<?> matcher, Matcher<?>...additionalMatchers);

  ResponseSpecification body(String key, Matcher<?> matcher, Object...additionalKeyMatcherPairs);

  ResponseSpecification when();

  RequestSpecification given();

  ResponseSpecification that();

  RequestSpecification request();

  ResponseSpecification response();

  ResponseSpecification and();

  RequestSpecification with();

  ResponseSpecification then();

  ResponseSpecification expect();
}