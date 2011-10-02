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

import com.jayway.restassured.parsing.Parser
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.ParserRegistry
import org.codehaus.groovy.runtime.MethodClosure

class RestAssuredParserRegistry extends ParserRegistry {
  /*
   * We need to set it statically because buildDefaultParserMap is called BEFORE the
   * super constructor is called.
   */
  def static ResponseSpecification responseSpecification

  @Override
  protected Map<String, Closure> buildDefaultParserMap() {
    if(responseSpecification == null || !responseSpecification.hasBodyAssertionsDefined()) {
      def parsers = [:].withDefault { new MethodClosure(this, "parseStream") }
      return parsers
    } else {
      def Parser restAssuredDefaultParser = responseSpecification.rpr.defaultParser
      def hasDefaultParser = restAssuredDefaultParser != null
      if(hasDefaultParser) {
        defaultParser = findDefaultParserMethod(restAssuredDefaultParser)
      }

      return super.buildDefaultParserMap()
    }
  }

  private findDefaultParserMethod(Parser defaultParser) {
    def parserMethodName = null
    switch(defaultParser) {
      case Parser.XML:
        parserMethodName = "parseXML"
        break;
      case Parser.JSON:
        parserMethodName = "parseJSON"
        break;
      case Parser.HTML:
        parserMethodName = "parseHTML"
        break;
    }
    return new MethodClosure(this, parserMethodName)
  }

}
