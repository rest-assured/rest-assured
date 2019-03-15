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

package io.restassured.internal.support

import org.apache.commons.lang3.StringUtils

import static org.apache.commons.lang3.StringUtils.substringAfter


class PathSupport {
  private static final String SLASH = "/"

  def static String mergeAndRemoveDoubleSlash(String thisOne, String otherOne) {
    thisOne = thisOne.trim()
    otherOne = otherOne.trim()
    final boolean otherOneStartsWithSlash = otherOne.startsWith(SLASH);
    final boolean thisOneEndsWithSlash = thisOne.endsWith(SLASH)
    if (thisOneEndsWithSlash && otherOneStartsWithSlash) {
      return thisOne + substringAfter(otherOne, SLASH);
    } else if (thisOneEndsWithSlash && isFullyQualified(otherOne)) {
      thisOne = ""
    } else if (!thisOneEndsWithSlash && !otherOneStartsWithSlash && !(otherOne == "" ^ thisOne == "")) {
      return "$thisOne/$otherOne"
    }
    return thisOne + otherOne;
  }

  def static boolean isFullyQualified(String targetUri) {
    if (StringUtils.isBlank(targetUri)) {
      return false
    }

    def indexOfFirstSlash = targetUri.indexOf("/");
    def indexOfScheme = targetUri.indexOf("://");
    if (indexOfScheme == -1) {
      // If we didn't find a single :// in the path then we know that the targetUri is not fully-qualified
      return false
    }
    return indexOfScheme < indexOfFirstSlash
  }

  def static String getPath(String targetUri) {
    if (StringUtils.isBlank(targetUri)) {
      return targetUri
    }

    def indexOfScheme = targetUri.indexOf("://");
    if (indexOfScheme == -1) {
      def path = StringUtils.substringBefore(targetUri, "?")
      return StringUtils.startsWith(path, "/") ? path : "/" + path
    }
    def indexOfPath = StringUtils.indexOf(targetUri, "/", indexOfScheme + 3);
    if (indexOfPath == -1) {
      return "/"
    }
    StringUtils.substringBefore(targetUri.substring(indexOfPath), "?")
  }
}
