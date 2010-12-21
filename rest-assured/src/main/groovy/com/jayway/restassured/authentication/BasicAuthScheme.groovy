package com.jayway.restassured.authentication

import groovyx.net.http.HTTPBuilder

class BasicAuthScheme implements AuthenticationScheme {
  def String userName
  def String password

  @Override def authenticate(HTTPBuilder httpBuilder) {
    return httpBuilder.auth.basic(userName, password)
  }
}
