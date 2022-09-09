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
import io.restassured.internal.csrf.CsrfInputFieldFinder
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification

import static io.restassured.RestAssured.given
import static io.restassured.internal.filter.FormAuthFilter.FORM_AUTH_COMPLETED_CONTEXT_KEY

class CsrfFilter implements Filter {

  CsrfConfig csrfConfig

  @Override
  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    if (csrfConfig.isCsrfEnabled()) {
      def formAuthFilterPresent = requestSpec.definedFilters.any { it instanceof FormAuthFilter }
      boolean formAuthFilterPresentAndIfSoIsItCompleted = !formAuthFilterPresent ? true : formAuthFilterPresent && ctx.hasValue(FORM_AUTH_COMPLETED_CONTEXT_KEY, true)

      // We need to apply CSRF _after_ FormAuthFilter is completed (if it is used), otherwise the request will get the wrong cookie.
      // Also, a CSRF token doesn't need to be sent on GET/HEAD requests.
      if (formAuthFilterPresentAndIfSoIsItCompleted && !requestSpec.method.equalsIgnoreCase("GET") && !requestSpec.method.equalsIgnoreCase("HEAD")) {
        def pageThatContainsCsrfToken = given().auth().none().cookies(requestSpec.getCookies()).get(csrfConfig.getCsrfTokenPath())
        def csrfInputField = CsrfInputFieldFinder.findInHtml(csrfConfig, pageThatContainsCsrfToken)

        if (csrfConfig.shouldSendCsrfTokenAsFormParam()) {
          requestSpec.formParam(csrfInputField.name, csrfInputField.value)
        } else {
          requestSpec.header(csrfInputField.name, csrfInputField.value)
        }
      }
    }

    return ctx.next(requestSpec, responseSpec)
  }
}
