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

import com.jayway.restassured.internal.path.xml.NodeChildrenImpl
import com.jayway.restassured.internal.path.xml.NodeImpl
import com.jayway.restassured.path.xml.element.NodeChildren
import static com.jayway.restassured.assertion.AssertionSupport.escapeMinus
import static com.jayway.restassured.assertion.AssertionSupport.generateWhitespace
import groovy.util.slurpersupport.*

class XMLAssertion implements Assertion {
  String key;
  boolean toUpperCase;

  /* Matches fragment such as children() or size(2) */
  private def isInvocationFragment = ~/.*\(\d*\)|.*\{.*/

  def Object getResult(Object object) {
    key = escapeMinus(key);
    def indexOfDot = key.indexOf(".")
    def baseString
    def evaluationString
    def isRootOnly = indexOfDot < 0
    if (!isRootOnly) {
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
      def errorMessage = e.getMessage();
      if(errorMessage.startsWith("No signature of method:")) {
        errorMessage = "Path $key is invalid."
      } else {
        errorMessage = e.getMessage().replace("startup failed:", "Invalid path:").replace(rootObject, generateWhitespace(rootObject.length() - baseString.length()) + baseString)
      }
      throw new IllegalArgumentException(errorMessage);
    }
    def javaObject = convertToJavaObject(result)
    return preventTreatingRootObjectAsAList(javaObject)
  }

  private def preventTreatingRootObjectAsAList(javaObject) {
    if (javaObject instanceof List && javaObject.size() == 1) {
      javaObject = javaObject.get(0)
    }
    return javaObject
  }

  boolean isPathFragment(String fragment) {
    return !isInvocationFragment.matcher(fragment).matches()
  }

  private def convertToJavaObject(result) {
    def returnValue;
    if(result.getClass().getName().equals(Attributes.class.getName())) {
      returnValue = toJavaObject(result, true, false)
    } else if(result instanceof Node) {
      returnValue = nodeToJavaObject(result)
    } else if(result instanceof FilteredNodeChildren) {
       returnValue = toJavaObject(result, false, true)
    } else if(result instanceof NodeChild) {
      def object = toJavaObject(result, false, false)
      if(object instanceof NodeChildren) {
        returnValue = object.get(0)
      } else {
        returnValue = object
      }
    } else if(result instanceof GPathResult) {
      returnValue = toJavaObject(result, false, false)
    } else if(result instanceof List) {
      returnValue = handleList(result)
    } else {
      returnValue = result;
    }

    return returnValue
  }

  private def handleList(List result) {
    if (result.size() == 1) {
      return convertToJavaObject(result.get(0))
    } else {
      for(int i = 0; i < result.size(); i++) {
        result.set(i, convertToJavaObject(result.get(i)))
      }
    }

    result
  }

  private def nodeToJavaObject(node) {
    def nodeImpl = new NodeImpl(name: node.name())
    addAttributes(nodeImpl, node)
    for(Object child : node.children()) {
      if(child instanceof Node) {
        def object = convertToJavaObject(child)
        nodeImpl.children << object
      } else {
        nodeImpl.value = child
      }
    }
    nodeImpl
  }

  private def addAttributes(nodeImpl, node) {
    def attributes = node.attributes();
    nodeImpl.attributes = convertToJavaObject(attributes)
  }

  private boolean shouldBeTreatedAsList(child) {
    def firstGrandChild = child.children().get(0);
    return firstGrandChild instanceof Node;
  }

  private def toJavaObject(nodes, isAttributes, forceList) {
    if (nodes.size() == 1 && !hasChildren(nodes, isAttributes)) {
      return nodes.text()
    } else {
      return toJavaList(nodes, isAttributes, forceList)
    }
  }

  private boolean hasChildren(nodes, isAttributes) {
    if(isAttributes) {
      return false;
    }
    return !nodes.children().isEmpty()
  }

  private def toJavaList(nodes, isAttributes, forceList) {
    def nodeList = forceList ? [] : new NodeChildrenImpl()
    if(isAttributes) {
      def temp = []
      nodes.each {
        CharArrayWriter caw = new CharArrayWriter();
        it.writeTo(caw);
        caw.close();
        temp << caw.toString()
      }
      return temp
    } else {
      nodes.nodeIterator().each {
        def object = convertToJavaObject(it)
        nodeList << object
      }
    }
    nodeList
  }

  def String description() {
    return "XML element"
  }
}

class XmlEntity {
  def children
  def attributes

}