package io.restassured.specification;

/**
 * Allows you to specify how the GraphQL Query will look like.
 */
public interface QuerySpecification {

    QuerySpecification name(String name);

    QuerySpecification param(String key, String value);

    QuerySpecification field(String field);

    QuerySpecification fields(String... fields);

    RequestSpecification when();
}
