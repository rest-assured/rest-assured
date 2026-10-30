package io.restassured.internal.assertion;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BodyMatcherGroup {
    private final List<BodyMatcher> bodyAssertions = new ArrayList<>();

    public void add(BodyMatcherGroup group) {
        bodyAssertions.addAll(group.bodyAssertions);
    }

    /**
     * Adds copies of the expectations of <code>group</code> that use <code>rpr</code>, and <code>rootPath</code> when
     * they were defined without a root path.
     */
    public void add(BodyMatcherGroup group, String rootPath, ResponseParserRegistrar rpr) {
        group.bodyAssertions.forEach(bodyMatcher -> bodyAssertions.add(bodyMatcher.copy(rootPath, rpr)));
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

    public List<Object> validate(final Response response, final Object contentParser, final RestAssuredConfig config) {
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

    /**
     * @return The path of the first expectation that evaluates a path, or {@code null} if there's none
     */
    public String firstPath() {
        return bodyAssertions.stream().filter(BodyMatcher::requiresPathParsing).findFirst().map(BodyMatcher::getPath).orElse(null);
    }
}
