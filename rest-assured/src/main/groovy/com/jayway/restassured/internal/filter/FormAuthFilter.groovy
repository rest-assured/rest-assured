/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.internal.filter

import com.jayway.restassured.authentication.FormAuthConfig
import com.jayway.restassured.filter.FilterContext
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.FilterableRequestSpecification
import com.jayway.restassured.specification.FilterableResponseSpecification
import com.jayway.restassured.spi.AuthFilter
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.path.xml.XmlPath.CompatibilityMode.HTML
import static java.lang.String.format

class FormAuthFilter implements AuthFilter {
  private static final String FIND_INPUT_TAG = "html.depthFirst().grep { it.name() == 'INPUT' && it.@type == '%s' }.collect { it.@name }"
  private static final String FIND_FORM_ACTION = "html.depthFirst().grep { it.name() == 'FORM' }.get(0).@action"

  def userName
  def password
  def FormAuthConfig config

  @Override
  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    final String formAction;
    final String userNameInputForm;
    final String passwordInputForm;
    if(config == null) {
      def responseBody = ctx.send(given().spec(requestSpec).auth().none()).asString()
      def html = new XmlPath(HTML, responseBody);
      String tempFormAction = throwIfException { html.getString(FIND_FORM_ACTION) }
      formAction = tempFormAction.startsWith("/") ? tempFormAction : "/"+ tempFormAction
      userNameInputForm = throwIfException { html.getString(format(FIND_INPUT_TAG, "text")) }
      passwordInputForm = throwIfException { html.getString(format(FIND_INPUT_TAG, "password")) }
    } else {
      formAction = config.getFormAction()
      userNameInputForm = config.getUserInputTagName()
      passwordInputForm = config.getPasswordInputTagName()
    }
    final Response loginResponse = given().port(requestSpec.getPort()).with().auth().none().and().with().params(userNameInputForm, userName, passwordInputForm, password).then().post(formAction)
    requestSpec.cookies(loginResponse.getDetailedCookies());
    return ctx.next(requestSpec, responseSpec);
  }


  def throwIfException(Closure closure) {
    try {
      closure.call()
    } catch(Exception e) {
      throw new IllegalArgumentException("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.", e)
    }
  }
}