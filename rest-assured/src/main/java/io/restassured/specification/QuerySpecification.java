package io.restassured.specification;

/**
 * Allows you to specify how the GraphQL Query will look like.
 */
public interface QuerySpecification {

    QuerySpecification name(String name);

    RequestSpecification when();
}
