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


package io.restassured.internal

import io.restassured.authentication.ExplicitNoAuthScheme
import io.restassured.config.RestAssuredConfig
import io.restassured.config.SessionConfig
import io.restassured.http.Cookies
import io.restassured.spi.AuthFilter

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class SpecificationMerger {

  /**
   * Merge this builder with settings from another specification. Note that the supplied specification
   * can overwrite data in the current specification. The following settings are overwritten:
   * <ul>
   *     <li>Content type</li>
   *     <li>Root path</
   *     <li>Status code</li>
   *     <li>Status line</li>
   *     <li>Fallback parser</li>
   *     <li>Expected response time</li>
   * </ul>
   * The following settings are merged:
   * <ul>
   *     <li>Response body expectations</li>
   *     <li>Cookies</li>
   *     <li>Headers</li>
   *     <li>Response parser settings</li>
   * </ul>
   */
  def static void merge(ResponseSpecificationImpl thisOne, ResponseSpecificationImpl with) {
    notNull thisOne, "Specification to merge"
    notNull with, "Specification to merge with"

    thisOne.contentType = with.contentType
    thisOne.rpr.defaultParser = with.rpr.defaultParser
    thisOne.rpr.additional.putAll(with.rpr.additional)
    thisOne.bodyMatchers << with.bodyMatchers
    thisOne.bodyRootPath = with.bodyRootPath
    thisOne.cookieAssertions.addAll(with.cookieAssertions)
    thisOne.expectedStatusCode = with.expectedStatusCode
    thisOne.expectedStatusLine = with.expectedStatusLine
    thisOne.expectedResponseTime = with.expectedResponseTime
    thisOne.headerAssertions.addAll(with.headerAssertions)
  }

  /**
   * Merge this builder with settings from another specification. Note that the supplied specification
   * can overwrite data in the current specification. The following settings are overwritten:
   * <ul>
   *     <li>Port</li>
   *     <li>Authentication scheme</
   *     <li>Content type</li>
   *     <li>Request body</li>
   *     <li>Keystore</li>
   *     <li>URL Encoding enabled/disabled</li>
   *     <li>Config</li>
   *     <li>Proxy Specification</li>
   * </ul>
   * The following settings are merged:
   * <ul>
   *     <li>Parameters</li>
   *     <li>Query Parameters</li>
   *     <li>Form Parameters</li>
   *     <li>Path parameters</li>
   *     <li>Multi-part form data parameters</li>
   *     <li>Cookies</li>
   *     <li>Headers</li>
   *     <li>Filters</li>
   * </ul>
   */
  def static void merge(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
    notNull thisOne, "Specification to merge"
    notNull with, "Specification to merge with"

    thisOne.@port = with.@port
    thisOne.baseUri = with.baseUri
    thisOne.basePath = with.basePath
    thisOne.requestParameters.putAll(with.requestParameters)
    thisOne.queryParameters.putAll(with.queryParams)
    thisOne.formParameters.putAll(with.formParams)
    thisOne.namedPathParameters.putAll(with.pathParams)
    thisOne.multiParts.addAll(with.multiParts)
    thisOne.authenticationScheme = with.authenticationScheme
    mergeSessionId(thisOne, with)
    thisOne.cookies(with.cookies)
    thisOne.requestBody = with.requestBody
    mergeFilters(thisOne, with)
    thisOne.urlEncodingEnabled = with.urlEncodingEnabled
    thisOne.proxySpecification = with.proxySpecification
    thisOne.method = with.method
    thisOne.unnamedPathParamsTuples = with.unnamedPathParamValues
    thisOne.path = with.path

    mergeConfig(thisOne, with)
    // It's important that headers are merged after the configs are merged since HeaderConfig affects that way headers are merged.
    thisOne.headers(with.requestHeaders)
  }

  private static def mergeConfig(RequestSpecificationImpl thisOne, RequestSpecificationImpl other) {
    def RestAssuredConfig thisConfig = thisOne.restAssuredConfig()
    def RestAssuredConfig otherConfig = other.restAssuredConfig()
    def thisIsUserConfigured = thisConfig.isUserConfigured()
    def otherIsUserConfigured = otherConfig.isUserConfigured()
    if (thisIsUserConfigured && otherIsUserConfigured) {
      def configsToUse = [:]
      thisConfig.configs.each { configType, thisTempConfig ->
        def otherTempConfig = otherConfig.configs.get(configType)
        if (otherTempConfig.isUserConfigured()) {
          configsToUse.put(configType, otherTempConfig)
        } else {
          configsToUse.put(configType, thisTempConfig);
        }
      }

      def newConfig = new RestAssuredConfig()
      newConfig.configs.putAll(configsToUse)
      thisOne.restAssuredConfig = newConfig

    } else if (!thisIsUserConfigured && otherIsUserConfigured) {
      thisOne.restAssuredConfig = otherConfig;
    }
  }

  private static def mergeSessionId(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
    def thisOneConfig = thisOne.config
    def thisOneCookies = thisOne.cookies

    def otherConfig = with.config
    def otherCookies = with.cookies;

    def oldSessionIdName = SessionConfig.DEFAULT_SESSION_ID_NAME
    if (thisOneConfig != null) {
      oldSessionIdName = thisOneConfig.sessionConfig.sessionIdName()
    }

    def shouldRemoveSessionFromThis
    if (otherConfig == null) {
      shouldRemoveSessionFromThis = otherCookies.hasCookieWithName(oldSessionIdName);
    } else {
      def otherSessionIdName = otherConfig.sessionConfig.sessionIdName();
      shouldRemoveSessionFromThis = otherCookies.hasCookieWithName(otherSessionIdName);
    }

    if (shouldRemoveSessionFromThis) {
      def cookieList = thisOneCookies.findAll { !it.getName().equalsIgnoreCase(oldSessionIdName) }
      thisOne.cookies = new Cookies(cookieList);
    }
  }

  private static def mergeFilters(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
    def thisFilters = thisOne.filters;
    def withFilters = with.filters

    // Overwrite auth filters
    def instanceOfAuthFilter = { it instanceof AuthFilter }
    if ((thisFilters.any(instanceOfAuthFilter) && withFilters.any(instanceOfAuthFilter)) ||
            with.authenticationScheme instanceof ExplicitNoAuthScheme) {
      thisFilters.removeAll(instanceOfAuthFilter)
    }
    // Only add filters not already present
    def toAdd = withFilters.findAll({ !thisFilters.contains(it) })
    thisFilters.addAll(toAdd)
  }
}
