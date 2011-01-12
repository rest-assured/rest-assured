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

import groovy.util.slurpersupport.NodeChild

class XMLAssertion implements Assertion {
  String key;
  boolean ignoreCase;


  def Object getResult(Object object) {
    Object current = object;
    def keys = key.split("\\.");
    keys.each { key ->
      if(current instanceof List) {
        current.each { node ->
          if(nodeEquals(node, key)) {
            current = node.children
          }
        }
      } else {
        current.nodeIterator().each { node ->
          if(nodeEquals(node, key)) {
            current = node.children
          }
        }
      }
    }

    if(current instanceof List) {
      if(current.size() == 1) {
        current = current.get(0)
      } else {
        def temp = []
        current.each {
          CharArrayWriter caw = new CharArrayWriter();
          it.writeTo(caw);
          caw.close();
          temp << caw.toString()
        }
        current = temp;
      }
    }

    return current;
  }

  private boolean nodeEquals(node, currentKey) {
    return ignoreCase ? node.name.equalsIgnoreCase(currentKey) : node.name.equals(currentKey)
  }

  def String description() {
    return "XML element"
  }
}
