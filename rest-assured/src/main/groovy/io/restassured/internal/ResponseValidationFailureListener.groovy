package io.restassured.internal

import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification

interface ResponseValidationFailureListener {
        void onFailure(RequestSpecification requestSpecification,
                       ResponseSpecification responseSpecification,
                       Response response)
}
