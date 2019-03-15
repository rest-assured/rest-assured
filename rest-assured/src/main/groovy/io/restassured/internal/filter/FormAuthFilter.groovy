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
import io.restassured.config.RestAssuredConfig
import io.restassured.config.SessionConfig
import io.restassured.filter.FilterContext
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.session.SessionFilter
import io.restassured.path.xml.XmlPath
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import io.restassured.specification.RequestSpecification
import io.restassured.spi.AuthFilter

import static io.restassured.RestAssured.given
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML
import static java.lang.String.format

class FormAuthFilter implements AuthFilter {
  private static
  final String FIND_INPUT_TAG_WITH_TYPE = "html.depthFirst().grep { it.name() == 'input' && it.@type == '%s' }.collect { it.@name }"
  private static
  final String FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }"
  private static
  final String FIND_INPUT_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }.get(0)"
  private static final String FIND_FORM_ACTION = "html.depthFirst().grep { it.name() == 'form' }.get(0).@action"

  def userName
  def password
  def FormAuthConfig formAuthConfig
  def SessionConfig sessionConfig

  @Override
  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    String formAction;
    String userNameInputField;
    String passwordInputField;
    String csrfFieldName;
    String csrfValue;
    List<Tuple2<String, String>> additionalInputFields = []

    if (formAuthConfig == null) {
      formAuthConfig = new FormAuthConfig();
    }

    if (formAuthConfig.requiresParsingOfLoginPage()) {
      def response = ctx.send(given().spec(requestSpec).auth().none())
      def html = new XmlPath(HTML, response.asString())

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

      if (formAuthConfig.hasCsrfFieldName() || formAuthConfig.isAutoDetectCsrfFieldName()) {
        csrfFieldName = formAuthConfig.hasCsrfFieldName() ? formAuthConfig.csrfFieldName : nullIfException {
          html.getString(format(FIND_INPUT_TAG_WITH_TYPE, "hidden"))
        }
        csrfValue = nullIfException { html.getString(format(FIND_INPUT_FIELD_WITH_NAME, csrfFieldName)) }
        if (!csrfValue) {
          throw new IllegalArgumentException("Couldn't find the CSRF input field with name $csrfFieldName in response. Response was:\n${response.prettyPrint()}")
        }
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
      csrfFieldName = null
      csrfValue = null
      additionalInputFields = null
    }

    formAction = formAction?.startsWith("/") ? formAction : "/" + formAction

    def loginRequestSpec = given().auth().none().and().formParams(userNameInputField, userName, passwordInputField, password)

    def uri = new URI(requestSpec.getURI())
    String loginUri = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + formAction

    if (csrfValue && csrfFieldName) {
      if (formAuthConfig.shouldSendCsrfTokenAsFormParam()) {
        loginRequestSpec.formParam(csrfFieldName, csrfValue)
      } else {
        loginRequestSpec.header(csrfFieldName, csrfValue)
      }
    }

    if (formAuthConfig?.isLoggingEnabled()) {
      def logConfig = formAuthConfig.getLogConfig()
      def logDetail = formAuthConfig.getLogDetail()
      if (logDetail != LogDetail.STATUS) {
        loginRequestSpec.filter(new RequestLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream(), logConfig.shouldUrlEncodeRequestUri()));
      }

      if (logDetail != LogDetail.PARAMS) {
        loginRequestSpec.filter(new ResponseLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()));
      }
    }

    additionalInputFields?.forEach { tuple ->
      loginRequestSpec.formParam(tuple.first, tuple.second)
    }

    applySessionFilterFromOriginalRequestIfDefined(requestSpec, loginRequestSpec)
    final Response loginResponse = loginRequestSpec.post(loginUri)
    // Don't send the detailed cookies because they contain too many detail (such as Path which is a reserved token)
    requestSpec.cookies(loginResponse.getCookies());
    return ctx.next(requestSpec, responseSpec);
  }

  static
  def void applySessionFilterFromOriginalRequestIfDefined(FilterableRequestSpecification requestSpec, RequestSpecification loginRequestSpec) {
    def filters = requestSpec.getDefinedFilters()
    def sessionFilterInOriginalRequest = filters.find { it.class.isAssignableFrom(SessionFilter.class) }
    if (sessionFilterInOriginalRequest) {
      loginRequestSpec.noFiltersOfType(SessionFilter.class)
      loginRequestSpec.filter(sessionFilterInOriginalRequest)
      def sessionIdName = requestSpec.getConfig().getSessionConfig().sessionIdName();
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

  static def nullIfException(Closure closure) {
    try {
      closure.call()
    } catch (Exception e) {
      null
    }
  }

}