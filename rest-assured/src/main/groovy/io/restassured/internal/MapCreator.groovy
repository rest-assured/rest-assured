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

import groovy.transform.Canonical
import io.restassured.specification.Argument
import org.hamcrest.Matcher

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class MapCreator {

  static enum CollisionStrategy {
    MERGE, OVERWRITE
  }

  static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy,
                                                 String firstParam, Object firstValue, ... parameters) {
    return createMapFromObjects(collisionStrategy, createArgumentArrayFromKeyAndValue(firstParam, firstValue, parameters))
  }

  static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy, String firstParam, ... parameters) {
    return createMapFromObjects(collisionStrategy, createArgumentArray(firstParam, parameters))
  }

  static Map<String, Object> createMapFromObjects(CollisionStrategy collisionStrategy, ... parameters) {
    if (parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.")
    } else if (parameters.length % 2 != 0 && parameters.length % 3 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    int step
    if (parameters.length >= 3 && isRestAssuredArguments(parameters[1]) && parameters[2] instanceof Matcher) {
      step = 3
    } else if (parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    } else {
      step = 2
    }

    boolean argumentsDefined = step == 3
    Map<String, Object> map = new LinkedHashMap<String, Object>()
    for (int i = 0; i < parameters.length; i += step) {
      def key = parameters[i]
      def args
      def val
      if (!argumentsDefined) {
        args = null
        val = parameters[i + 1]
      } else {
        args = parameters[i + 1]
        val = parameters[i + 2]
        if (!isRestAssuredArguments(args)) {
          throw new IllegalArgumentException("Illegal argument '$args' passed to body expectation '$key', a list of ${Argument.class.name} is required.")
        }
      }

      if (map.containsKey(key) && collisionStrategy == CollisionStrategy.MERGE) {
        def currentValue = map.get(key)
        def value = argumentsDefined ? new ArgsAndValue(args, val) : val
        if (currentValue instanceof List) {
          currentValue << value
        } else {
          map.put(key, [currentValue, value])
        }
      } else if (argumentsDefined) {
        map.put(key, new ArgsAndValue(args, val))
      } else {
        map.put(key, val)
      }
    }

    return map
  }

  private static boolean isRestAssuredArguments(args) {
    args instanceof List && args.every { it instanceof Argument }
  }

  private static Object[] createArgumentArray(String firstParam, ... parameters) {
    notNull firstParam, "firstParam"
    if (parameters == null || parameters.length == 0) {
      return [firstParam: new NoParameterValue()] as Object[]
    }

    def params = [firstParam, parameters[0]]
    if (parameters.length > 1) {
      parameters[1..-1].each {
        params << it
      }
    }
    return params as Object[]
  }

  private static Object[] createArgumentArrayFromKeyAndValue(String firstParam, Object firstValue, ... parameters) {
    notNull firstParam, "firstParam"
    notNull firstValue, "firstValue"
    def params = [firstParam, firstValue]
    parameters.each {
      params << it
    }
    return params as Object[]
  }

  @Canonical
  static class ArgsAndValue {
    List<Argument> args
    def value
  }

}