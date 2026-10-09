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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.internal.util.GroovyTypes.isGString;

/**
 * The equality of Groovy's {@code ==} operator, for code that used to be written in Groovy and compared objects
 * a user supplies.
 * <p>
 * Numbers are compared by value across types ({@code 1 == 1L}, {@code 1.0G == 1.00G}), a {@code Character} equals a
 * one-character String and its code point, a GString equals the String it renders, and arrays, lists, maps and sets
 * are compared element by element with these rules. Anything else is compared with {@code compareTo} when it is
 * {@code Comparable}, and with {@code equals} otherwise.
 * </p>
 */
public final class GroovyStyleEquality {

    private static final int INTEGER = 0;
    private static final int LONG = 1;
    private static final int BIG_INTEGER = 2;
    private static final int BIG_DECIMAL = 3;

    private GroovyStyleEquality() {
    }

    /**
     * @return {@code true} if Groovy's {@code left == right} is {@code true}
     */
    public static boolean isEqual(Object left, Object right) {
        if (left == right) {
            return true;
        } else if (left == null || right == null) {
            return false;
        } else if (left instanceof Comparable) {
            return comparesEqual(left, right);
        }
        boolean leftIsArray = left.getClass().isArray();
        boolean rightIsArray = right.getClass().isArray();
        if ((leftIsArray || left instanceof List) && (rightIsArray || right instanceof List)) {
            return elementsEqual(asList(left), asList(right));
        } else if (left instanceof Map && right instanceof Map) {
            return mapsEqual((Map<?, ?>) left, (Map<?, ?>) right);
        } else if (left instanceof Set && right instanceof Set) {
            return setsEqual((Set<?>) left, (Set<?>) right);
        }
        return left.equals(right);
    }

    @SuppressWarnings("unchecked")
    private static boolean comparesEqual(Object left, Object right) {
        if (left instanceof Number) {
            if (right instanceof Number || right instanceof Character) {
                return compareNumbers((Number) left, toNumber(right)) == 0;
            } else if (isSingleCharacterString(right)) {
                return compareNumbers((Number) left, (int) right.toString().charAt(0)) == 0;
            }
        } else if (left instanceof Character) {
            char character = (Character) left;
            if (isSingleCharacterString(right)) {
                return character == right.toString().charAt(0);
            } else if (right instanceof Number) {
                return compareNumbers((int) character, (Number) right) == 0;
            } else if (right instanceof String || isGString(right)) {
                return left.toString().equals(right.toString());
            }
        } else if (right instanceof Number) {
            if (isSingleCharacterString(left)) {
                return compareNumbers((int) left.toString().charAt(0), (Number) right) == 0;
            }
        } else if ((left instanceof String || isGString(left)) && (right instanceof String || isGString(right) || right instanceof Character)) {
            return left.toString().equals(right.toString());
        }

        if (left.getClass().isAssignableFrom(right.getClass())
                || (right.getClass() != Object.class && right.getClass().isAssignableFrom(left.getClass()))
                || right instanceof Comparable) {
            try {
                return ((Comparable<Object>) left).compareTo(right) == 0;
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isSingleCharacterString(Object value) {
        return (value instanceof String || isGString(value)) && value.toString().length() == 1;
    }

    private static Number toNumber(Object numberOrCharacter) {
        return numberOrCharacter instanceof Character ? (int) (Character) numberOrCharacter : (Number) numberOrCharacter;
    }

    // Groovy's NumberMath: floating point wins, then BigDecimal, BigInteger, Long and Integer
    private static int compareNumbers(Number left, Number right) {
        if (isFloatingPoint(left) || isFloatingPoint(right)) {
            return Double.compare(left.doubleValue(), right.doubleValue());
        }
        switch (Math.max(kindOf(left), kindOf(right))) {
            case INTEGER:
                return Integer.compare(left.intValue(), right.intValue());
            case LONG:
                return Long.compare(left.longValue(), right.longValue());
            case BIG_INTEGER:
                return toBigInteger(left).compareTo(toBigInteger(right));
            default:
                return toBigDecimal(left).compareTo(toBigDecimal(right));
        }
    }

    private static boolean isFloatingPoint(Number number) {
        return number instanceof Double || number instanceof Float;
    }

    private static int kindOf(Number number) {
        if (number instanceof Long) {
            return LONG;
        } else if (number instanceof BigInteger) {
            return BIG_INTEGER;
        } else if (number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return INTEGER;
        }
        return BIG_DECIMAL;
    }

    private static BigInteger toBigInteger(Number number) {
        return number instanceof BigInteger ? (BigInteger) number : BigInteger.valueOf(number.longValue());
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(number.doubleValue());
        }
    }

    private static List<?> asList(Object arrayOrList) {
        if (arrayOrList instanceof List) {
            return (List<?>) arrayOrList;
        }
        return new AbstractList<Object>() {
            @Override
            public Object get(int index) {
                return Array.get(arrayOrList, index);
            }

            @Override
            public int size() {
                return Array.getLength(arrayOrList);
            }
        };
    }

    private static boolean elementsEqual(List<?> left, List<?> right) {
        if (left.size() != right.size()) {
            return false;
        }
        Iterator<?> rightElements = right.iterator();
        for (Object leftElement : left) {
            if (!isEqual(leftElement, rightElements.next())) {
                return false;
            }
        }
        return true;
    }

    private static boolean mapsEqual(Map<?, ?> left, Map<?, ?> right) {
        if (left.size() != right.size() || !left.keySet().equals(right.keySet())) {
            return false;
        }
        for (Map.Entry<?, ?> entry : left.entrySet()) {
            if (!isEqual(entry.getValue(), right.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean setsEqual(Set<?> left, Set<?> right) {
        if (left.size() != right.size()) {
            return false;
        }
        List<Object> unmatched = new ArrayList<>(right);
        for (Object leftElement : left) {
            if (!removeFirstEqual(unmatched, leftElement)) {
                return false;
            }
        }
        return true;
    }

    private static boolean removeFirstEqual(List<Object> candidates, Object element) {
        for (Iterator<Object> iterator = candidates.iterator(); iterator.hasNext(); ) {
            if (isEqual(element, iterator.next())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }
}
