/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.internal

import static com.jayway.restassured.assertion.AssertParameter.notNull

class MapCreator {

  def static Map<String, Object> createMapFromParams(String firstParam,  Object firstValue, ... parameters) {
    return createMapFromObjects(createArgumentArray(firstParam, firstValue, parameters));
  }

  def static Map<String, Object> createMapFromObjects(... parameters) {
    if(parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < parameters.length; i+=2) {
      map.put(parameters[i], parameters[i+1]);
    }
    return map;
  }

  private static Object[] createArgumentArray(String firstParam,  Object firstValue,
                                              ... parameters) {
    notNull firstParam, "firstParam"
    notNull firstValue, "firstValue"
    def params = [firstParam, firstValue]
    parameters.each {
      params << it
    }
    return params as Object[]
  }
}
