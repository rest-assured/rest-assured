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

package com.jayway.restassured.internal

import static com.jayway.restassured.assertion.AssertParameter.notNull

class SpecificationMerger {

  /**
   * Merge this builder with settings from another specification. Note that the supplied specification
   * can overwrite data in the current specification. The following settings are overwritten:
   * <ul>
   *     <li>Content type</li>
   *     <li>Root path</
   *     <li>Status code</li>
   *     <li>Status line</li>
   * </ul>
   * The following settings are merged:
   * <ul>
   *     <li>Response body expectations</li>
   *     <li>Cookies</li>
   *     <li>Headers</li>
   * </ul>
   * @param specification The specification the add.
   * @return The builder
   */
  def static void merge(ResponseSpecificationImpl thisOne, ResponseSpecificationImpl with) {
    notNull thisOne, "Specification to merge"
    notNull with, "Specification to merge with"

    thisOne.contentType = with.contentType
    thisOne.bodyMatchers << with.bodyMatchers
    thisOne.bodyRootPath = with.bodyRootPath
    thisOne.cookieAssertions.addAll(with.cookieAssertions)
    thisOne.expectedStatusCode = with.expectedStatusCode
    thisOne.expectedStatusLine = with.expectedStatusLine
    thisOne.headerAssertions.addAll(with.headerAssertions)
  }
}
