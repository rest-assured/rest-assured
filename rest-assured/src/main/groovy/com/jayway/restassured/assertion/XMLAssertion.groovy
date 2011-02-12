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

import groovy.util.slurpersupport.Attributes
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren
import static com.jayway.restassured.assertion.AssertionSupport.escapeMinus

class XMLAssertion implements Assertion {
  String key;
  boolean toUpperCase;

  /* Matches fragment such as children() or size(2) */
  private def isInvocationFragment = ~/.*\(\d*\)/

  def Object getResult(Object object) {
    key = escapeMinus(key);
    def indexOfDot = key.indexOf(".")
    def baseString
    def evaluationString
    if (indexOfDot > 0) {
      if(toUpperCase) {
        def pathFragments = key.split("\\.");
        for(int i = 0; i < pathFragments.length; i++) {
          if(isPathFragment(pathFragments[i])) {
            pathFragments[i] = pathFragments[i].toUpperCase();
          }
        }
        key = pathFragments.join(".")
      }
      evaluationString = key.substring(indexOfDot);
      baseString = key.substring(0, indexOfDot)
    } else {
      evaluationString = "";
      baseString = key;
    }

    def result;
    def rootObject = "restAssuredXmlRootObject"
    try {
      result = Eval.me(rootObject, object, "$rootObject$evaluationString")
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage().replace("startup failed:", "Invalid XML expression:").replace(rootObject, baseString));
    }
    return convertToJavaObject(result)
  }

  boolean isPathFragment(String fragment) {
    return !isInvocationFragment.matcher(fragment).matches()
  }

  private def convertToJavaObject(result) {
    def returnValue;
    if(result.getClass().getName().equals(Attributes.class.getName())) {
      returnValue = toJavaObject(result, true)
    } else if(result instanceof NodeChild || result.getClass().getName().equals(NodeChildren.class.getName())) {
      returnValue = toJavaObject(result, false)
    } else {
      returnValue = result;
    }
    return returnValue
  }

  private def toJavaObject(nodes, isAttributes) {
    if (nodes.size() == 1 && !hasChildren(nodes, isAttributes)) {
      return nodes.text()
    } else {
      return toJavaList(nodes, isAttributes)
    }
  }

  private boolean hasChildren(nodes, isAttributes) {
    if(isAttributes) {
      return false;
    }
    return !nodes.children().isEmpty()
  }

  private List toJavaList(nodes, isAttributes) {
    def temp = []
    if(isAttributes) {
      nodes.each {
        CharArrayWriter caw = new CharArrayWriter();
        it.writeTo(caw);
        caw.close();
        temp << caw.toString()
      }
    } else {
      nodes.nodeIterator().each {
        temp << it.text()
      }
    }
    return temp
  }

  def String description() {
    return "XML element"
  }
}

class XmlEntity {
  def children
  def attributes
}