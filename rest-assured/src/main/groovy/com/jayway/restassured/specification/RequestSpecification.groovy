package com.jayway.restassured.specification

import groovyx.net.http.ContentType

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/16/10
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RequestSpecification extends RequestSender {

  RequestSpecification when();

  RequestSpecification given();

  RequestSpecification that();

  RequestSpecification body(String body);

  RequestSpecification content(String body);

  RequestSpecification body(byte[] body);

  RequestSpecification content(byte[] body);

  ResponseSpecification response();

  RequestSpecification request();

  RequestSpecification cookies(String cookieName, String...cookieNameValuePairs);

  RequestSpecification cookies(Map<String, String> cookies);

  RequestSpecification cookie(String key, String value);

  RequestSpecification parameters(String parameter, String...parameters);

  RequestSpecification parameters(Map<String, String> parametersMap);

  RequestSpecification and();

  RequestSpecification with();

  ResponseSpecification then();

  ResponseSpecification expect();

  RequestSpecification contentType(ContentType contentType);

  AuthenticationSpecification auth();

  AuthenticationSpecification authentication();

  RequestSpecification port(int port);

  RequestSpecification headers(Map<String, String> headers);

  RequestSpecification header(String key, String value);

  RequestSpecification headers(String headerName, String ... headerNameValuePairs);
}