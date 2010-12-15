package com.jayway.restassured.authentication

import groovyx.net.http.AuthConfig
import groovyx.net.http.HTTPBuilder
/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/15/10
 * Time: 9:10 AM
 * To change this template use File | Settings | File Templates.
 */

public interface AuthenticationScheme {

  def authenticate(HTTPBuilder httpBuilder)

}