package com.jayway.restassured.internal

import com.jayway.restassured.exception.AssertionFailedException
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/16/10
 * Time: 10:15 AM
 * To change this template use File | Settings | File Templates.
 */
class TestSpecification {
  def final RequestSpecification requestSpecification
  def final ResponseSpecification responseSpecification


  TestSpecification(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
    this.requestSpecification = requestSpecification
    this.responseSpecification = responseSpecification;
    responseSpecification.requestSpecification = requestSpecification
    requestSpecification.responseSpecification = responseSpecification
  }
}
