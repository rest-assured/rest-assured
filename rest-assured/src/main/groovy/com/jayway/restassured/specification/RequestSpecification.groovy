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

  ResponseSpecification response();

  RequestSpecification request();

  RequestSpecification parameters(String parameter, String...parameters);

  RequestSpecification parameters(Map<String, String> parametersMap);

  RequestSpecification and();

  RequestSpecification with();

  RequestSpecification then();

  ResponseSpecification expect();

  RequestSpecification contentType(ContentType contentType);

  AuthenticationSpecification auth();

  AuthenticationSpecification authentication();

  RequestSpecification port(int port);

  RequestSpecification headers(Map<String, String> headers);

  RequestSpecification headers(String headerName, String ... headerNameValuePairs);
}