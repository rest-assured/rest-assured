package com.jayway.restassured.specification

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/16/10
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RequestSpecification {

  RequestSpecification when();

  RequestSpecification given();

  RequestSpecification that();

  ResponseSpecification response();

  RequestSpecification request();

  void get(String path);

  void post(String path);

  RequestSpecification parameters(String parameter, String...parameters);

  RequestSpecification parameters(Map<String, String> parametersMap);

  RequestSpecification and();

  RequestSpecification with();

  RequestSpecification then();

  ResponseSpecification expect();

  AuthenticationSpecification auth();

  AuthenticationSpecification authentication();

  RequestSpecification port(int port);
}