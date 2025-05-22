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

import io.restassured.config.CsrfConfig
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.filter.cookie.CookieFilter
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.session.SessionFilter
import io.restassured.internal.csrf.CsrfTokenFinder
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification

import static io.restassured.RestAssured.given
import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM

class CsrfFilter implements Filter {

  CsrfConfig csrfConfig

  @Override
  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    if (csrfConfig.isCsrfEnabled()) {
      // CSRF token doesn't need to be sent for GET/HEAD requests.
      if (!requestSpec.method.equalsIgnoreCase("GET") && !requestSpec.method.equalsIgnoreCase("HEAD")) {
        def requestSpecification = given().auth().none().disableCsrf().cookies(requestSpec.getCookies())
        if (csrfConfig.isLoggingEnabled()) {
          def logConfig = csrfConfig.getLogConfig()
          def logDetail = csrfConfig.getLogDetail()
          if (logDetail != LogDetail.STATUS) {
            requestSpecification.filter(new RequestLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream(), logConfig.shouldUrlEncodeRequestUri(), logConfig.blacklistedHeaders()))
          }

          if (logDetail != LogDetail.PARAMS) {
            requestSpecification.filter(new ResponseLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()))
          }
        }


        def pageThatContainsCsrfToken = requestSpecification.get(csrfConfig.getCsrfTokenPath())
        def csrfData = CsrfTokenFinder.findInHtml(csrfConfig, pageThatContainsCsrfToken)
        if (!csrfData) {
          throw new IllegalArgumentException("Couldn't find a the CSRF token in response. Expecting either an input field with name \"${csrfConfig.csrfInputFieldName}\" or a meta tag with name \"${csrfConfig.csrfMetaTagName}\". " +
                  "Response was:\n${pageThatContainsCsrfToken.prettyPrint()}")
        }

        if (csrfConfig.isAutomaticallyApplyCookies()) {
          // Add cookies returned from the GET request to the next request
          requestSpec.cookies(pageThatContainsCsrfToken.cookies())

          // Store cookies in cookie filter if applicable
          def cookieFilter = requestSpec.definedFilters.find { it instanceof CookieFilter } as CookieFilter
          if (cookieFilter != null) {
            cookieFilter.storeCookiesFromResponseIfApplicable(requestSpec.getURI(), pageThatContainsCsrfToken)
          }

          // Store session in session filter if applicable
          def sessionFilter = requestSpec.definedFilters.find { it instanceof SessionFilter } as SessionFilter
          if (sessionFilter != null) {
            sessionFilter.storeSessionIdFromResponse(pageThatContainsCsrfToken)
          }
        }

        if (csrfData.shouldSendTokenAs(FORM)) {
          requestSpec.formParam(csrfData.inputFieldOrHeaderName, csrfData.token)
        } else {
          requestSpec.header(csrfData.inputFieldOrHeaderName, csrfData.token)
        }
      }
    }

    return ctx.next(requestSpec, responseSpec)
  }
}
