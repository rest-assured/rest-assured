package io.restassured.internal.assertion;

import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BodyMatcherGroup {
    private final List<BodyMatcher> bodyAssertions = new ArrayList<>();

    public void add(BodyMatcherGroup group) {
        bodyAssertions.addAll(group.bodyAssertions);
    }

    public void add(BodyMatcher bodyMatcher) {
        bodyAssertions.add(bodyMatcher);
    }

    public void reset() {
        bodyAssertions.clear();
    }

    public int size() {
        return bodyAssertions.size();
    }

    public List<Map<String, Object>> validate(final Response response, final Object contentParser, final RestAssuredConfig config) {
        return bodyAssertions.stream().map(bodyMatcher -> bodyMatcher.validate(response, contentParser, config)).collect(Collectors.toList());

    }

    public boolean containsMatchers() {
        return !bodyAssertions.isEmpty();
    }

    public boolean requiresTextParsing() {
        return bodyAssertions.stream().anyMatch(BodyMatcher::requiresTextParsing);
    }

    public boolean requiresPathParsing() {
        return bodyAssertions.stream().anyMatch(BodyMatcher::requiresPathParsing);
    }
}
