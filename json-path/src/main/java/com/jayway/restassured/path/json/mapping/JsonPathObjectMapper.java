package com.jayway.restassured.path.json.mapping;

public interface JsonPathObjectMapper {
    <T> T toObject(Class<T> objectType, String json);
}
