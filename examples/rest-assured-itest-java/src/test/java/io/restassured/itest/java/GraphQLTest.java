package io.restassured.itest.java;

import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GraphQLTest {

    @Test public void
    simple_given_query_graphql_when_then_works() {
        given(). // RequestSpecification
                operation(). // OperationSpecification
                query("allFilms"). // QuerySpecification
                param("key", "value").
                field("title").
                fields("", "", "").
        when().
                graphql(). // RequestSendOptions
        then().
                statusCode(200);
    }

    @Test public void
    simple_given_query_with_name_method_graphql_when_then_works() {
        given(). // RequestSpecification
                operation(). // OperationSpecification
                query(). // QuerySpecification
                name("foo"). // QuerySpecification
        when().
                graphql().
        then().
                statusCode(200);
    }

//
//    @Test public void
//    simple_given_query_graphql_with_fields_when_then_works() {
//        given().
//                operation(). // OperationSpecification
//                query(). // QuerySpecification
//                name("foo"). // QuerySpecification
//                fields(Fields.of(Greeting.class). // FieldSpecification
//                        fields("firstName")).
//        when().
//                graphql().
//        then().
//                status(200).
//                body("greeting", equalTo("Greetings John Doe"));
//    }
//
//    @Test public void
//    simple_given_query_graphql_name_when_then_works() {
//        given().
//                operation(). // OperationSpecification
//                query("foo"). // QuerySpecification
//                fields(Fields.of(Greeting.class). // FieldSpecification
//                        fields("firstName")).
//        when().
//                graphql().
//        then().
//                status(200).
//                body("greeting", equalTo("Greetings John Doe"));
//    }
//
//    @Test public void
//    simple_given_query_graphql_noname_when_then_works() {
//        given().
//                operation(). // OperationSpecification
//                query(). // QuerySpecification
//                fields(Fields.of(Greeting.class). // FieldSpecification
//                fields("firstName")).
//        when().
//                graphql().
//        then().
//                status(200).
//                body("greeting", equalTo("Greetings John Doe"));
//    }
//
//
//    @Test public void
//    simple_given_mutation_with_name_graphql_when_then_works() {
//        given().
//                operation().
//                mutation("name").
//                fields(new Fields[1]);
//    }
//
//    @Test public void
//    simple_given_mutation_with_name_function_graphql_when_then_works() {
//        given().
//        operation().
//                mutation().
//                name("").
//                fields().
//    }
}
