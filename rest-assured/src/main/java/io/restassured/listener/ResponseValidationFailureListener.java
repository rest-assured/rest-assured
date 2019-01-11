package io.restassured.listener;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public interface ResponseValidationFailureListener {
    void onFailure(RequestSpecification requestSpecification,
                   ResponseSpecification responseSpecification,
                   Response response);
}
