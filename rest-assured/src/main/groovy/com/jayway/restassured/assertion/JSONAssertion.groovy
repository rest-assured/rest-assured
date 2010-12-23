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

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: Oct 23, 2010
 * Time: 2:27:27 PM
 * To change this template use File | Settings | File Templates.
 */
class JSONAssertion implements Assertion {
  String key;


  def Object getResult(Object object) {
    Object current = object;
    def keys = key.split("\\.");
    keys.each { key ->
      if(current instanceof JSONArray) {
        current = current?.getAt(key)
      } else if(current?.has(key)) {
        current = current.get(key)
      } else {
        throw new IllegalArgumentException("$object doesn't contain key $key")
      }
    }

    return convertToJavaArrayIfNeeded(current);
  }

  private Object convertToJavaArrayIfNeeded(current) {
    if (current instanceof JSONArray) {
      current = current.toArray()
    }
    return current
  }

  def String description() {
    return "JSON element"
  }
}
