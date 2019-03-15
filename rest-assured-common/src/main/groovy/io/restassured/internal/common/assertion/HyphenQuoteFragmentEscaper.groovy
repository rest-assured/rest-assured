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

package io.restassured.internal.common.assertion

import org.apache.commons.lang3.StringUtils

/**
 * A {@link PathFragmentEscaper} that is is specific to path fragments that consists of one or more hyphens
 */
abstract class HyphenQuoteFragmentEscaper implements PathFragmentEscaper {
  private static def indexStartChar = '['
  private static def indexEndChar = ']'

  @Override
  String escape(String pathFragment) {
    def indexOfStart
    def indexOfEnd
    // Check if this path fragment contains reads an index from a collection (for example some-list[0])
    // If this is the case we should escape "some-list" but leave the index lookup ([0]) outside, i.e. 'some-list'[0]
    if (pathFragment.trim().endsWith(indexEndChar)
            && (indexOfStart = pathFragment.indexOf(indexStartChar)) > 1
            && pathFragment.indexOf(indexEndChar) > indexOfStart) {

      def toEscape = StringUtils.substringBeforeLast(pathFragment, indexStartChar);
      def indexLookup = indexStartChar + StringUtils.substringAfterLast(pathFragment, indexStartChar);
      doEscape(toEscape) + indexLookup
    } else {
      doEscape(pathFragment)
    }
  }

  private def doEscape(String str) {
    "'" + str + "'"
  }
}
