/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.path.jayway;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import io.restassured.internal.common.spi.path.PathResult;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates JSONPath expressions with Jayway JsonPath on documents that {@link JsonParser} has read. It's the only class
 * of the provider that uses Jayway, so that {@link JaywayJsonProvider} can check the Jayway version first.
 */
public final class JaywayEvaluator {
    static final String DESCRIPTION = "JSONPath (Jayway)";

    /**
     * Built explicitly, since other libraries can change Jayway's default configuration. No options, so that a missing
     * path throws and can be told apart from a list addressed as an object.
     */
    public static final Configuration CONFIGURATION = Configuration.builder().jsonProvider(new MapsAndListsJsonProvider())
            .mappingProvider(new MapsAndListsMappingProvider()).options(EnumSet.noneOf(Option.class)).build();

    // The same message on Jayway 2.8.0 to 3.0.0, such as: Expected to find an object with property ['title'] in path $['store']['book'] but found 'java.util.ArrayList'.
    private static final Pattern NOT_AN_OBJECT = Pattern.compile("^Expected to find an object with property (\\[.*?]) in path (\\$.*?) but found '");
    private static final Pattern BRACKET = Pattern.compile("\\['((?:[^'\\\\]|\\\\.)*)']|\\[[^]]*]");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");

    RuntimeException parseFailure(Exception cause) {
        return new InvalidJsonException("Failed to parse the JSON document", cause);
    }

    PathResult evaluate(Object document, String path) {
        String gpathQuotedKey = withoutDotQuotedKeys(path);
        if (!gpathQuotedKey.equals(path)) {
            return PathResult.invalidPath(DESCRIPTION + " can't evaluate " + path + ": a quoted key after a dot is GPath syntax. Use "
                    + gpathQuotedKey + " instead.");
        }
        JsonPath jsonPath;
        try {
            jsonPath = JsonPath.compile(rewrite(path));
        } catch (InvalidPathException e) {
            return PathResult.invalidPath(DESCRIPTION + " can't evaluate " + path + ": " + e.getMessage());
        }
        try {
            return PathResult.value(jsonPath.read(document, CONFIGURATION));
        } catch (PathNotFoundException e) {
            return notFound(document, path, e);
        } catch (InvalidPathException e) {
            // Such as a GPath method that Jayway doesn't have, like unique(): "Function with name: unique does not exist."
            return PathResult.invalidPath(DESCRIPTION + " can't evaluate " + path + ": " + e.getMessage());
        } catch (JsonPathException e) {
            throw new IllegalArgumentException(DESCRIPTION + " can't evaluate " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Jayway throws the same exception for a missing property and for a property of something that isn't an object,
     * such as a list. Only a list fails, since a path written in GPath, such as {@code store.book.title}, would expect
     * the property of each element. A property of {@code null}, a string or a number is missing, as in GPath.
     */
    private static PathResult notFound(Object document, String path, PathNotFoundException e) {
        Matcher matcher = NOT_AN_OBJECT.matcher(String.valueOf(e.getMessage()));
        if (matcher.find()) {
            String parentPath = matcher.group(2);
            Object parent;
            try {
                parent = JsonPath.compile(parentPath).read(document, CONFIGURATION);
            } catch (JsonPathException ignored) {
                return PathResult.missing();
            }
            if (parent instanceof List) {
                String parentInDotNotation = dotNotation(parentPath);
                String property = dotNotation("$" + matcher.group(1));
                return PathResult.listAddressedAsObject(DESCRIPTION + " can't evaluate " + path + ": " + parentInDotNotation + " is a list, so "
                        + withElements(path, parentInDotNotation, property.startsWith("[") ? property : "." + property) + ".");
            }
        }
        return PathResult.missing();
    }

    /**
     * @return How to address the elements of the list: the path with {@code [*]} after the list when the path names
     * the list and the property as {@link #dotNotation(String)} writes them, such as {@code store.book[*].title}, and
     * otherwise the list with {@code [*]}. Also the latter when a function follows, since Jayway applies a function
     * after {@code [*]} to each element, so {@code store.book[*].price.sum()} fails.
     */
    private static String withElements(String path, String list, String property) {
        String root = path.startsWith("$.") ? "$." : "";
        String relative = path.substring(root.length());
        if (list.equals("$")) {
            return "use $[*]" + (relative.startsWith("$") ? relative.substring(1) : (property.startsWith("[") ? "" : ".") + relative);
        }
        String listAndProperty = list + property;
        if (relative.startsWith(listAndProperty) && relative.indexOf('(', listAndProperty.length()) < 0
                && (relative.length() == listAndProperty.length() || ".[".indexOf(relative.charAt(listAndProperty.length())) >= 0)) {
            return "use " + root + list + "[*]" + relative.substring(list.length());
        }
        return "address its elements with " + list + "[*]";
    }

    /**
     * Rewrites a path for Jayway: an empty path is the root, and a leading key that starts with {@code $}, such as
     * {@code $schema}, is put in brackets, since Jayway reads a leading {@code $} as the root.
     */
    static String rewrite(String path) {
        if (path.isEmpty()) {
            return "$";
        } else if (path.length() > 1 && path.charAt(0) == '$' && path.charAt(1) != '.' && path.charAt(1) != '[') {
            int end = 1;
            while (end < path.length() && path.charAt(end) != '.' && path.charAt(end) != '[') {
                end++;
            }
            return "$['" + path.substring(0, end) + "']" + path.substring(end);
        }
        return path;
    }

    /**
     * @return The path with each GPath quoted key in dot notation, such as {@code .'my-key'} or a leading
     * {@code 'my-key'}, in bracket notation instead, such as {@code ['my-key']}. Quotes inside brackets, as in
     * {@code $['my-key']} or a filter, are left alone.
     */
    static String withoutDotQuotedKeys(String path) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (quote != 0) {
                result.append(c);
                if (c == '\\' && i + 1 < path.length()) {
                    result.append(path.charAt(++i));
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (depth > 0) {
                result.append(c);
                if (c == '\'' || c == '"') {
                    quote = c;
                } else if (c == '[' || c == '(') {
                    depth++;
                } else if (c == ']' || c == ')') {
                    depth--;
                }
            } else if ((c == '\'' || c == '"') && (i == 0 || path.charAt(i - 1) == '.')) {
                int end = closingQuote(path, i);
                if (end == -1) {
                    result.append(path.substring(i));
                    break;
                }
                if (i > 0 && !(i > 1 && path.charAt(i - 2) == '.')) {
                    // The dot before the key, unless it's a deep scan, such as $..'title', which stays $..['title']
                    result.setLength(result.length() - 1);
                }
                result.append('[').append(path, i, end + 1).append(']');
                i = end;
            } else {
                result.append(c);
                if (c == '[' || c == '(') {
                    depth++;
                }
            }
        }
        return result.toString();
    }

    /**
     * @return The index of the quote that closes the one at {@code start}, skipping escaped quotes, or -1.
     */
    private static int closingQuote(String path, int start) {
        char quote = path.charAt(start);
        for (int i = start + 1; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == quote) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return A normalized path of Jayway, such as {@code $['store']['book'][0]}, in dot notation, such as
     * {@code store.book[0]}, with keys that aren't identifiers in brackets. The root alone is {@code $}.
     */
    static String dotNotation(String normalizedPath) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = BRACKET.matcher(normalizedPath.substring(1));
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key != null && IDENTIFIER.matcher(key).matches()) {
                result.append(result.length() == 0 ? "" : ".").append(key);
            } else {
                result.append(matcher.group());
            }
        }
        return result.length() == 0 ? "$" : result.toString();
    }
}
