package io.restassured.internal;

import io.restassured.specification.QuerySpecification;
import io.restassured.specification.RequestSpecification;

class QuerySpecificationImpl implements QuerySpecification {

    private String name;
    private RequestSpecification requestSpecification;

    QuerySpecificationImpl(String name, RequestSpecification request) {
        this.name = name;
        this.requestSpecification = request;
    }

    QuerySpecificationImpl(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification
    }

    def QuerySpecification name(String name) {
        this.name = name;
        return this;
    }

    def RequestSpecification when() {
        return this.requestSpecification;
    }
}
