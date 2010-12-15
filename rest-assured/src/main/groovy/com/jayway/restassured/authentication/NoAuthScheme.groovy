package com.jayway.restassured.authentication

import groovyx.net.http.AuthConfig
import groovyx.net.http.HTTPBuilder

/**
 * Authentication scheme that doesn't do any authentication
 */
class NoAuthScheme implements AuthenticationScheme {
  @Override def authenticate(HTTPBuilder httpBuilder) {
  }
}
