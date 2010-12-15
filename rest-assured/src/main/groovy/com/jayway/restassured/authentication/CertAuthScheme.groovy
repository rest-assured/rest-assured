package com.jayway.restassured.authentication

import groovyx.net.http.HTTPBuilder

class CertAuthScheme implements AuthenticationScheme {
  def String certURL
  def String password

  @Override def authenticate(HTTPBuilder httpBuilder) {
    return httpBuilder.auth.certificate(certURL, password)
  }
}
