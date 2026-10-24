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

package io.restassured.itest.java.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * JDK differences table, header names and order: HttpClient 4 delivers the response headers as the server sent them,
 * the JDK client with lower case names, sorted by name. {@link #normalize(String)} rewrites an expected response log
 * (or a message that lists the response headers) the way the JDK client delivers the headers.
 */
public final class ResponseLog {

    // "Name: value" in a response log, "Name=value" in a message that lists the response headers
    private static final Pattern HEADER = Pattern.compile("[A-Za-z][A-Za-z0-9-]*(: |=).*");

    private ResponseLog() {
    }

    /**
     * @param expected What HttpClient 4 logs
     * @return {@code expected} unchanged on HttpClient 4. On the JDK client, each block of consecutive header lines has
     * lower case names and is sorted by name, keeping the order of the values of a name.
     */
    public static String normalize(String expected) {
        if (!TestClient.isJdk()) {
            return expected;
        }
        StringBuilder result = new StringBuilder();
        List<String> block = new ArrayList<>();
        // Splits after each line separator, keeping it
        for (String line : expected.split("(?<=\n)")) {
            if (HEADER.matcher(line.stripTrailing()).matches()) {
                block.add(lowerCaseName(line));
            } else {
                flush(block, result);
                result.append(line);
            }
        }
        flush(block, result);
        return result.toString();
    }

    private static String lowerCaseName(String line) {
        String name = name(line);
        return name.toLowerCase(Locale.ROOT) + line.substring(name.length());
    }

    private static void flush(List<String> block, StringBuilder result) {
        block.sort(Comparator.comparing(ResponseLog::name));
        block.forEach(result::append);
        block.clear();
    }

    private static String name(String line) {
        return line.split("[:=]", 2)[0];
    }
}
