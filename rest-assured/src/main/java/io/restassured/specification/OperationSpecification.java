package io.restassured.specification;

/**
 * Operations available when sending GraphQL request.
 *
 * */
public interface OperationSpecification {

    QuerySpecification query(String name);

    QuerySpecification query();

    QuerySpecification mutation(String name);

    QuerySpecification mutation();

    QuerySpecification subscription(String name);

    QuerySpecification subscription();

}
