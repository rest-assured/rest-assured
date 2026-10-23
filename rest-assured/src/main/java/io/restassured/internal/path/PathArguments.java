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

package io.restassured.internal.path;

import io.restassured.specification.Argument;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.restassured.internal.common.util.GroovyStyleToString.format;

/**
 * Fills in the placeholders of a path with the arguments of {@code withArgs(..)}.
 */
public final class PathArguments {
    // A format specifier as java.util.Formatter parses it: %[index$][flags][width][.precision][t]conversion
    private static final Pattern FORMAT_SPECIFIER = Pattern.compile("%(\\d+\\$)?([-#+ 0,(<]*)(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

    private PathArguments() {
    }

    /**
     * Apply each list of arguments in turn, the way {@link #apply(String, List)} does.
     */
    public static String applyInOrder(String path, List<List<Argument>> argumentLists) {
        for (List<Argument> arguments : argumentLists) {
            path = apply(path, arguments);
        }
        return path;
    }

    public static String apply(String path, List<Argument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return path;
        }
        List<Object> args = new ArrayList<>(arguments.size());
        for (Object argument : arguments) {
            if (!(argument instanceof Argument)) {
                throw new IllegalArgumentException("Path arguments must be instances of " + Argument.class.getName() + ", use withArgs(..) to create them. Was '" + format(arguments) + "'.");
            }
            args.add(((Argument) argument).getArgument());
        }
        // Placeholders that have no argument are kept as they are, so that a later withArgs(..) can fill them in
        java.util.regex.Matcher specifiers = FORMAT_SPECIFIER.matcher(path);
        StringBuilder pathToFormat = new StringBuilder();
        int placeholders = 0;
        while (specifiers.find()) {
            String conversion = specifiers.group(6);
            boolean takesNoArgument = specifiers.group(5) == null && (conversion.equals("%") || conversion.equals("n"));
            boolean hasExplicitIndex = specifiers.group(1) != null || specifiers.group(2).contains("<");
            if (!takesNoArgument && !hasExplicitIndex && ++placeholders > arguments.size()) {
                args.add(specifiers.group());
                specifiers.appendReplacement(pathToFormat, "%s");
            }
        }
        specifiers.appendTail(pathToFormat);
        return String.format(pathToFormat.toString(), args.toArray());
    }
}
