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
import com.jayway.restassured.response.Response
import groovyx.net.http.ParserRegistry
import net.sf.json.groovy.JsonSlurper
import org.xml.sax.XMLReader
import static com.jayway.restassured.parsing.Parser.*

class ContentParser {
  def parse(Response response, ResponseParserRegistrar rpr) {
    Parser parser = rpr.getParser(response.contentType())
    def content;
    def bodyAsInputStream = response.asInputStream()
    if(parser == null) {
      content = bodyAsInputStream
    } else {
      switch(parser) {
        case JSON:
          content = new JsonSlurper().parse(bodyAsInputStream)
          break;
        case XML:
          content = new XmlSlurper().parse(bodyAsInputStream)
          break
        case HTML:
          XMLReader p = new org.cyberneko.html.parsers.SAXParser();
          p.setEntityResolver( ParserRegistry.getCatalogResolver() );
          content = new XmlSlurper( p ).parse(bodyAsInputStream);
          break
        case TEXT:
        default:
          content = bodyAsInputStream
      }
    }
    content
  }
}
