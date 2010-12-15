package com.jayway.restassured.authentication

import groovyx.net.http.HTTPBuilder

class OAuthScheme implements AuthenticationScheme {
  def String consumerKey
  def String consumerSecret
  def String accessToken
  def String secretToken

  @Override def authenticate(HTTPBuilder httpBuilder) {
    return httpBuilder.auth.oauth(consumerKey, consumerSecret, accessToken, secretToken)
  }
}
