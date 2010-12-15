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
