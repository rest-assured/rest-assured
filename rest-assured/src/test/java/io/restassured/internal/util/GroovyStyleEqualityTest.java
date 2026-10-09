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

package io.restassured.internal.util;

import groovy.lang.GString;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compares {@link GroovyStyleEquality} with Groovy's own {@code ==} for every pair of a set of values. It lives in
 * core because Groovy is on core's test classpath.
 */
class GroovyStyleEqualityTest {

    @Test
    void compares_like_groovy_equality() {
        List<Object> values = values();
        List<String> differences = new ArrayList<>();
        for (Object left : values) {
            for (Object right : values) {
                boolean expected;
                try {
                    expected = DefaultTypeTransformation.compareEqual(left, right);
                } catch (RuntimeException e) {
                    // Groovy fails for some arrays compared with other objects, GroovyStyleEquality returns false
                    continue;
                }
                if (GroovyStyleEquality.isEqual(left, right) != expected) {
                    differences.add(describe(left) + " == " + describe(right) + " should be " + expected);
                }
            }
        }
        assertThat(differences).isEmpty();
    }

    private static List<Object> values() {
        Map<String, Object> intMap = new LinkedHashMap<>();
        intMap.put("a", 1);
        Map<String, Object> longMap = new LinkedHashMap<>();
        longMap.put("a", 1L);
        return Arrays.asList(
                null, 0, 1, 1L, (short) 1, (byte) 1, BigInteger.ONE, new BigDecimal("1.0"), new BigDecimal("1.00"),
                1.0d, 1.0f, 1.5d, Double.NaN, -0.0d, 97, new AtomicLong(1), Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE),
                'a', "a", "b", "ab", gString("a"), gString("ab"), new StringBuilder("a"),
                Arrays.asList(1, 2), Arrays.asList(1L, 2L), Arrays.asList(1, 3), new Object[]{1, 2}, new int[]{1, 2},
                new long[]{1, 2}, new Object[]{"a"}, new String[]{"a", "b"}, Collections.singletonList(Arrays.asList(1)),
                intMap, longMap, Collections.emptyMap(), new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(Arrays.asList(1L, 2L)),
                Collections.emptyList(), TimeUnit.SECONDS, TimeUnit.MINUTES, new Object());
    }

    private static GString gString(String value) {
        return new GStringImpl(new Object[0], new String[]{value});
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        String rendered = value.getClass().isArray() ? Arrays.deepToString(new Object[]{value}) : String.valueOf(value);
        return value.getClass().getSimpleName() + " " + rendered;
    }
}
