package com.jayway.restassured.authentication

import groovyx.net.http.AuthConfig
import groovyx.net.http.HTTPBuilder

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/15/10
 * Time: 9:12 AM
 * To change this template use File | Settings | File Templates.
 */
class BasicAuthScheme implements AuthenticationScheme {
  def String userName
  def String password

  @Override def authenticate(HTTPBuilder httpBuilder) {
    return httpBuilder.auth.basic(userName, password)
  }
}
