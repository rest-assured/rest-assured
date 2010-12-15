package com.jayway.restassured.builder

import com.jayway.restassured.authentication.BasicAuthScheme
import com.jayway.restassured.authentication.CertAuthScheme
import com.jayway.restassured.authentication.OAuthScheme

class AuthenticationBuilder {
  private RequestBuilder requestBuilder;

  AuthenticationBuilder(RequestBuilder requestBuilder) {
    this.requestBuilder = requestBuilder
  }

  def RequestBuilder basic(String userName, String password) {
    requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
    return requestBuilder
  }

  def RequestBuilder certificate(String certURL, String password) {
    requestBuilder.authenticationScheme = new CertAuthScheme(certURL: certURL, password: password)
    return requestBuilder
  }

  def RequestBuilder oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
    requestBuilder.authenticationScheme = new OAuthScheme(consumerKey: consumerKey, consumerSecret: consumerSecret, accessToken: accessToken, secretToken: secretToken)
    return requestBuilder
  }
}
