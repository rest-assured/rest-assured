package io.restassured.internal;

import io.restassured.specification.QuerySpecification;
import io.restassured.specification.RequestSpecification;

public class QuerySpecificationImpl implements QuerySpecification {

    private String name;

    public QuerySpecificationImpl(String name) {
        this.name = name;
    }

    public QuerySpecificationImpl() {

    }

    @Override
    public QuerySpecification name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public RequestSpecification when() {
        return null;
    }
}
