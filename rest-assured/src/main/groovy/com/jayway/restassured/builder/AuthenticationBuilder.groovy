package com.jayway.restassured.builder

import com.jayway.restassured.authentication.BasicAuthScheme

class AuthenticationBuilder {
    def RequestBuilder requestBuilder;

   def RequestBuilder basic(String userName, String password) {
     requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
     return requestBuilder
   }
}
