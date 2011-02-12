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

import net.sf.json.JSONArray
import net.sf.json.JSONNull
import static java.util.Arrays.asList

class JSONAssertion implements Assertion {
  String key;

  def Object getResult(Object object) {
    def pathFragments = key.split("\\.")
    for(int i = 0; i < pathFragments.length; i++) {
      if(pathFragments[i].contains('-')) {
        pathFragments[i] = "'"+pathFragments[i]+"'"
      }
    }
    key = pathFragments.join(".")

    def result;
    try {
      result = Eval.me('restAssuredJsonRootObject', object, "restAssuredJsonRootObject.$key")
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage().replace("startup failed:", "Invalid JSON expression:"));
    }

    return convertToJavaArrayIfNeeded(result);
  }

  private Object convertToJavaArrayIfNeeded(current) {
    if (current instanceof JSONArray) {
      current = asList(current.toArray());
    }
    if(current instanceof List) {
      for(int i = 0; i < current.size(); i++) {
        if(current.get(i) instanceof JSONNull) {
          current.set(i, null);
        }
      }
    }
    return current


  }

  def String description() {
    return "JSON element"
  }
}