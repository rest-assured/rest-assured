package com.jayway.restassured.assertion

import net.sf.json.JSONArray

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: Oct 23, 2010
 * Time: 2:27:27 PM
 * To change this template use File | Settings | File Templates.
 */
class XMLAssertion implements Assertion {
  String key;


  def Object getResult(Object object) {
    Object current = object;
    def keys = key.split("\\.");
    keys.each { key ->
      if(current instanceof List) {
        current.each { node ->
          if(node.name.equals(key)) {
            current = node.children
          }
        }
      } else {
        current.nodeIterator().each { node ->
          if(node.name.equals(key)) {
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

  def String description() {
    return "XML element"
  }
}
