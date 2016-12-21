package io.restassured.internal.util;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public class MatcherErrorMessageBuilder<T, M extends Matcher<T>> {

    private final String name;

    public MatcherErrorMessageBuilder(String name) {
        this.name = name;
    }

    public String buildError(T actual, M matcher) {
        final StringDescription descriptionBuilder = new StringDescription();
        descriptionBuilder.appendText("Expected ").appendText(name).appendText(" ");
        matcher.describeTo(descriptionBuilder);
        descriptionBuilder.appendText(" but ");
        matcher.describeMismatch(actual, descriptionBuilder);

        final String description = descriptionBuilder.toString().replaceAll("[.\\n]+$", "");
        return description + ".\n";
    }
}
