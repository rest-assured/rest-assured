package com.jayway.restassured.specification

import com.jayway.restassured.authentication.BasicAuthScheme
import com.jayway.restassured.authentication.CertAuthScheme
import com.jayway.restassured.authentication.OAuthScheme
import com.jayway.restassured.internal.RequestSpecificationImpl

class AuthenticationSpecification {
  private RequestSpecification requestBuilder;

  AuthenticationSpecification(RequestSpecification requestBuilder) {
    this.requestBuilder = requestBuilder
  }

  def RequestSpecification basic(String userName, String password) {
    requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
    return requestBuilder
  }

  def RequestSpecification certificate(String certURL, String password) {
    requestBuilder.authenticationScheme = new CertAuthScheme(certURL: certURL, password: password)
    return requestBuilder
  }

  def RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
    requestBuilder.authenticationScheme = new OAuthScheme(consumerKey: consumerKey, consumerSecret: consumerSecret, accessToken: accessToken, secretToken: secretToken)
    return requestBuilder
  }
}
