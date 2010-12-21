package com.jayway.restassured.specification

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/16/10
 * Time: 10:15 AM
 * To change this template use File | Settings | File Templates.
 */
class TestSpecification implements RequestSender {
  def final RequestSpecification requestSpecification
  def final ResponseSpecification responseSpecification


  TestSpecification(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
    this.requestSpecification = requestSpecification
    this.responseSpecification = responseSpecification;
    responseSpecification.requestSpecification = requestSpecification
    requestSpecification.responseSpecification = responseSpecification
  }

  @Override
  void get(String path) {
    requestSpecification.get path
  }

  @Override
  void post(String path) {
    requestSpecification.post path
  }
}
