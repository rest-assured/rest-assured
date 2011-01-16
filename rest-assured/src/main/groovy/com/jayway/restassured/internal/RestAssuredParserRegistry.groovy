/*
 * Copyright 2011 the original author or authors.
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

import groovyx.net.http.ContentType
import org.codehaus.groovy.runtime.MethodClosure
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.ParserRegistry

class RestAssuredParserRegistry extends ParserRegistry {
  /*
   * We need to set it statically because buildDefaultParserMap is called BEFORE the
   * super constructor is called.
   */
  def static ResponseSpecification responseSpecification

  @Override
  protected Map<String, Closure> buildDefaultParserMap() {
    if(responseSpecification == null || !responseSpecification.hasAssertionsDefined()) {
      Map<String,Closure> parsers = new HashMap<String,Closure>();
      parsers.put( ContentType.BINARY.toString(), new MethodClosure(this, "parseStream" ) );
      def parseText = new MethodClosure(this, "parseText")
      parsers.put( ContentType.TEXT.toString(), parseText );
      parsers.put( ContentType.URLENC.toString(), parseText );
      parsers.put( ContentType.HTML.toString(), parseText);

      for ( String ct : ContentType.XML.getContentTypeStrings() )
        parsers.put( ct, parseText );

      for ( String ct : ContentType.JSON.getContentTypeStrings() )
        parsers.put( ct, parseText );

      return parsers
    } else {
      return super.buildDefaultParserMap()
    }
  }
}
