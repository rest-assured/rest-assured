package io.restassured.internal;

import io.restassured.specification.OperationSpecification;
import io.restassured.specification.QuerySpecification;

public class OperationSpecificationImpl implements OperationSpecification {
    @Override
    public QuerySpecification query(String name) {
        return new QuerySpecificationImpl(name);
    }

    @Override
    public QuerySpecification query() {
        return new QuerySpecificationImpl();
    }

    @Override
    public QuerySpecification mutation(String name) {
        return null;
    }

    @Override
    public QuerySpecification mutation() {
        return null;
    }

    @Override
    public QuerySpecification subscription(String name) {
        return null;
    }

    @Override
    public QuerySpecification subscription() {
        return null;
    }
}
