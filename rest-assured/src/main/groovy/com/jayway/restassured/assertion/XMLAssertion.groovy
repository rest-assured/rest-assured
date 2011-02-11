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

package com.jayway.restassured.assertion

class XMLAssertion implements Assertion {
  String key;
  boolean toUpperCase;


  def Object getResult(Object object) {
    def indexOfDot = key.indexOf(".")
    def baseString
    def evaluationString
    if (indexOfDot > 0) {
      if(toUpperCase) {
        key = key.toUpperCase();
      }
      evaluationString = key.substring(indexOfDot);
      baseString = key.substring(0, indexOfDot)
    } else {
      evaluationString = "";
      baseString = key;
    }
    def result;
    try {
      result = Eval.me(baseString, object, "$baseString$evaluationString")
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage().replace("startup failed:", "Invalid XML expression:"));
    }
    return convertToJavaObject(result)
  }

  private def convertToJavaObject(result) {
    def nodes = []
    result.childNodes().each {
      nodes << it;
    }
    if(nodes.isEmpty()) {
      return result.toString()
    } else if (nodes.size() == 1) {
      result = result.toString()
    } else {
      def temp = []
      nodes.each {
        CharArrayWriter caw = new CharArrayWriter();
        it.writeTo(caw);
        caw.close();
        temp << caw.toString()
      }
      result = temp;
    }
    return result
  }



  def String description() {
    return "XML element"
  }
}

class XmlEntity {
  def children
  def attributes
}

