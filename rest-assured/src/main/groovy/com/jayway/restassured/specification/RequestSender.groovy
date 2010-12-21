package com.jayway.restassured.specification

/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: 12/21/10
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RequestSender {
  void get(String path);

  void post(String path);

  void put(String path);

  void delete(String path);
}