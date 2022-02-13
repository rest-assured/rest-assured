package io.restassured.internal

import io.restassured.specification.QuerySpecification;
import io.restassured.specification.RequestSpecification;

class QuerySpecificationImpl implements QuerySpecification {

    private String name;
    private Map<String, String> params = [:];
    private Set<String> fields = [];
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
        this
    }

    def QuerySpecification param(String key, String value) {
        this.params.put(key, value);
        this
    }

    def QuerySpecification field(String field) {
        this.fields.add(field)
        this
    }

    def QuerySpecification fields(String... fields) {
        this.fields.addAll(fields)
        this
    }

    def RequestSpecification when() {

        String requestBody =
                "{\"query\":" +
                        "\"" +
                        "{" +
                        " "+ this.name +"  {" +
                      //  " title" +
                        this.fields.first() +
                        " director" +
                        " releaseDate" +
                        " episodeID" +
                        "}" +
                        "}" +
                        "\"" +
                        "}";

        this.requestSpecification.body(requestBody)
        requestSpecification
    }
}
