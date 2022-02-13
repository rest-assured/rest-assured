package io.restassured.internal;

import io.restassured.specification.OperationSpecification;
import io.restassured.specification.QuerySpecification;
import io.restassured.specification.RequestSpecification;

class OperationSpecificationImpl implements OperationSpecification {

    RequestSpecification requestSpecification;

    OperationSpecificationImpl(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification
    }

    def QuerySpecification query(String name) {
        return new QuerySpecificationImpl(name, this.requestSpecification);
    }


    def QuerySpecification query() {
        return new QuerySpecificationImpl(this.requestSpecification);
    }

    def QuerySpecification mutation(String name) {
        return null;
    }

    def QuerySpecification mutation() {
        return null;
    }

    def QuerySpecification subscription(String name) {
        return null;
    }

    def QuerySpecification subscription() {
        return null;
    }
}
