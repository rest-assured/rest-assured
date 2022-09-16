/*
 * Copyright 2019 the original author or authors.
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
package io.restassured.internal.filter

import io.restassured.authentication.FormAuthConfig
import io.restassured.config.CsrfConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.config.SessionConfig
import io.restassured.filter.FilterContext
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.session.SessionFilter
import io.restassured.internal.csrf.CsrfData
import io.restassured.internal.csrf.CsrfTokenFinder
import io.restassured.path.xml.XmlPath
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import io.restassured.specification.RequestSpecification
import io.restassured.spi.AuthFilter

import static io.restassured.RestAssured.given
import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML
import static java.lang.String.format

class FormAuthFilter implements AuthFilter {

  private static final String FIND_INPUT_TAG_WITH_TYPE = "html.depthFirst().grep { it.name() == 'input' && it.@type == '%s' }.collect { it.@name }"
  private static final String FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }"
  private static final String FIND_FORM_ACTION = "html.depthFirst().grep { it.name() == 'form' }.get(0).@action"

  def userName
  def password
  FormAuthConfig formAuthConfig
  SessionConfig sessionConfig
  CsrfConfig csrfConfig

  @Override
  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    String formAction
    String userNameInputField
    String passwordInputField
    CsrfData csrfData
    Map<String, String> cookiesFromLoginPage
    List<Tuple2<String, String>> additionalInputFields = []

    if (formAuthConfig == null) {
      formAuthConfig = new FormAuthConfig()
    }

    if (formAuthConfig.requiresParsingOfLoginPage() || csrfConfig.isCsrfEnabled()) {
      def loginPageResponse
      if (csrfConfig.isCsrfEnabled()) {
        loginPageResponse = given().auth().none().disableCsrf().cookies(requestSpec.getCookies()).get(csrfConfig.getCsrfTokenPath())
        cookiesFromLoginPage = loginPageResponse.cookies()
      } else {
        loginPageResponse = ctx.send(given().spec(requestSpec).auth().none())
        cookiesFromLoginPage = loginPageResponse.cookies()
        if (loginPageResponse.statusCode() == 302) {
          // This means that Rest Assured has not done a redirect automatically.
          // This may happen if status code is 302 and method is not GET (see https://blog.jayway.com/2012/10/17/what-you-may-not-know-about-http-redirects/).
          // Thus we follow the Location header explicitly.
          loginPageResponse = given().auth().none().cookies(cookiesFromLoginPage).get(loginPageResponse.getHeader("Location"))
        }
      }

      def html = new XmlPath(HTML, loginPageResponse.asString())

      formAction = formAuthConfig.hasFormAction() ? formAuthConfig.getFormAction() : {
        String tempFormAction = throwIfException { html.getString(FIND_FORM_ACTION) }
        tempFormAction.startsWith("/") ? tempFormAction : "/" + tempFormAction
      }.call()
      userNameInputField = formAuthConfig.hasUserInputTagName() ? formAuthConfig.getUserInputTagName() : throwIfException {
        html.getString(format(FIND_INPUT_TAG_WITH_TYPE, "text"))
      }
      passwordInputField = formAuthConfig.hasPasswordInputTagName() ? formAuthConfig.getPasswordInputTagName() : throwIfException {
        html.getString(format(FIND_INPUT_TAG_WITH_TYPE, "password"))
      }

      if (csrfConfig.isCsrfEnabled()) {
        csrfData = CsrfTokenFinder.findInHtml(csrfConfig, loginPageResponse)
      }

      if (formAuthConfig.hasAdditionalInputFieldNames()) {
        formAuthConfig.getAdditionalInputFieldNames().each { name ->
          String value = throwIfException {
            html.getString(format(FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME, name))
          }
          additionalInputFields.add(new Tuple2(name, value))
        }
      }

    } else {
      formAction = formAuthConfig.getFormAction()
      userNameInputField = formAuthConfig.getUserInputTagName()
      passwordInputField = formAuthConfig.getPasswordInputTagName()
      additionalInputFields = null
      csrfData = null
      cookiesFromLoginPage = null
    }

    formAction = formAction?.startsWith("/") ? formAction : "/" + formAction

    def loginRequestSpec = given().auth().none().and().disableCsrf().and().formParams(userNameInputField, userName, passwordInputField, password)

    def uri = new URI(requestSpec.getURI())
    String loginUri = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + formAction

    if (cookiesFromLoginPage != null) {
      loginRequestSpec.cookies(cookiesFromLoginPage)
    }

    if (csrfData != null) {
      if (csrfData.shouldSendTokenAs(FORM)) {
        loginRequestSpec.formParam(csrfData.inputFieldOrHeaderName, csrfData.token)
      } else {
        loginRequestSpec.header(csrfData.inputFieldOrHeaderName, csrfData.token)
      }
    }

    if (formAuthConfig?.isLoggingEnabled()) {
      def logConfig = formAuthConfig.getLogConfig()
      def logDetail = formAuthConfig.getLogDetail()
      if (logDetail != LogDetail.STATUS) {
        loginRequestSpec.filter(new RequestLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream(), logConfig.shouldUrlEncodeRequestUri(), logConfig.blacklistedHeaders()))
      }

      if (logDetail != LogDetail.PARAMS) {
        loginRequestSpec.filter(new ResponseLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()))
      }
    }

    additionalInputFields?.forEach { tuple ->
      loginRequestSpec.formParam(tuple.getV1(), tuple.getV2())
    }

    applySessionFilterFromOriginalRequestIfDefined(requestSpec, loginRequestSpec)
    final Response loggedInResponse = loginRequestSpec.post(loginUri)
    // Don't send the detailed cookies because they contain too many detail (such as Path which is a reserved token)
    requestSpec.cookies(loggedInResponse.cookies())
    return ctx.next(requestSpec, responseSpec)
  }

  static void applySessionFilterFromOriginalRequestIfDefined(FilterableRequestSpecification requestSpec, RequestSpecification loginRequestSpec) {
    def filters = requestSpec.getDefinedFilters()
    def sessionFilterInOriginalRequest = filters.find { it.class.isAssignableFrom(SessionFilter.class) }
    if (sessionFilterInOriginalRequest) {
      loginRequestSpec.noFiltersOfType(SessionFilter.class)
      loginRequestSpec.filter(sessionFilterInOriginalRequest)
      def sessionIdName = requestSpec.getConfig().getSessionConfig().sessionIdName()
      def cfg = loginRequestSpec.config ?: new RestAssuredConfig()
      loginRequestSpec.config(cfg.sessionConfig(SessionConfig.sessionConfig().sessionIdName(sessionIdName)))
    }
  }

  static def throwIfException(Closure closure) {
    try {
      closure.call()
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.", e)
    }
  }
}