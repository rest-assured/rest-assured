/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
