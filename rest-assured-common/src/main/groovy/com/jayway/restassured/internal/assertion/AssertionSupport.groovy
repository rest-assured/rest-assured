/*
 * Copyright 2013 the original author or authors.
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
package com.jayway.restassured.internal.assertion

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils

class AssertionSupport {

  private static def closureStartFragment = '{'
  private static def closureEndFragment = '}'
  private static def listGetterFragment = '('
  private static def listIndexStartFragment = '['
  private static def listIndexEndFragment = ']'
  private static def space = ' '

  def static escapePath(key, Closure... closuresToEscape) {
    def pathFragments = key.split("(?<=\\')")
    for (int i = 0; i < pathFragments.size(); i++) {
      String pathFragment = pathFragments[i]
      if (!pathFragment?.endsWith("'") || pathFragment?.contains("**")) {
        def dotFragments = pathFragment.split("\\.")
        for (int k = 0; k < dotFragments.size(); k++) {
          String dotFragment = dotFragments[k]
          for (int j = 0; j < closuresToEscape.length; j++) {
            if (closuresToEscape[j](dotFragment)) {
              dotFragments[k] = "'" + dotFragments[k].trim() + "'"
              break;
            }
          }

        }
        pathFragments[i] = dotFragments.join(".")
      }
    }
    pathFragments.join("")
  }

  def static minus() {
    return { pathFragment ->
      !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains('-') && !containsAny(pathFragment, [closureStartFragment, closureEndFragment, listGetterFragment, listIndexStartFragment, listIndexEndFragment])
    }
  }

  def static properties() {
    return { pathFragment ->
      !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains('properties') && !containsAny(pathFragment, [closureStartFragment, closureEndFragment, listGetterFragment, listIndexStartFragment, space, listIndexEndFragment])
    }
  }

  def static attributeGetter() {
    return { pathFragment ->
      pathFragment.startsWith("@") && !pathFragment.endsWith("'") && !containsAny(pathFragment, [closureStartFragment, closureEndFragment, space])
    }
  }

  def static doubleStar() {
    return { pathFragment ->
      pathFragment == "**"
    }
  }

  def static colon() {
    return { pathFragment ->
      !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains(':') && !containsAny(pathFragment, [closureStartFragment, closureEndFragment, listGetterFragment, listIndexStartFragment, listIndexEndFragment])
    }
  }

  def static integer() {
    return { pathFragment ->
      (startsWithDigit(pathFragment) || NumberUtils.isDigits(pathFragment)) && !containsAny(pathFragment, [closureStartFragment, closureEndFragment, space, listGetterFragment, listIndexStartFragment, listIndexEndFragment])
    }
  }

  private static boolean startsWithDigit(def pathFragment) {
    if (StringUtils.isEmpty(pathFragment)) {
      return false
    }
    Character.isDigit(pathFragment.charAt(0))
  }

  def static String generateWhitespace(int number) {
    if (number < 1) {
      ""
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < number; i++) {
      builder.append(' ');
    }
    return builder.toString();
  }

  /**
   * Copied from Apache commons lang (String utils)
   */
  private static boolean containsAny(String str, searchChars) {
    if (str == null || str.length() == 0 || searchChars == null || searchChars.isEmpty()) {
      return false;
    }
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      for (int j = 0; j < searchChars.size(); j++) {
        if (searchChars[j] == ch) {
          return true;
        }
      }
    }
    return false;
  }
}
