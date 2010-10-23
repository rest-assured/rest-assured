package com.jayway.restassured.assertion

import org.hamcrest.Matcher

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
    return object.get(key);
  }
}
