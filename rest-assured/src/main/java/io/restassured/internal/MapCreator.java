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
package io.restassured.internal;

import io.restassured.internal.util.GroovyStyleHashCode;
import io.restassured.specification.Argument;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.common.util.GroovyStyleToString.format;

public class MapCreator {

    public enum CollisionStrategy {
        MERGE, OVERWRITE
    }

    public static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy,
                                                          String firstParam, Object firstValue, Object... parameters) {
        return createMapFromObjects(collisionStrategy, createArgumentArrayFromKeyAndValue(firstParam, firstValue, parameters));
    }

    public static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy, String firstParam, Object... parameters) {
        return createMapFromObjects(collisionStrategy, createArgumentArray(firstParam, parameters));
    }

    /**
     * @return A map from every key to its value. The keys are kept as they are given, so they need not be Strings.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> createMapFromObjects(CollisionStrategy collisionStrategy, Object... parameters) {
        if (parameters == null || parameters.length < 2) {
            throw new IllegalArgumentException("You must supply at least one key and one value.");
        } else if (parameters.length % 2 != 0 && parameters.length % 3 != 0) {
            throw new IllegalArgumentException("You must supply the same number of keys as values.");
        }

        int step;
        if (parameters.length >= 3 && isRestAssuredArguments(parameters[1]) && parameters[2] instanceof Matcher) {
            step = 3;
        } else if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("You must supply the same number of keys as values.");
        } else {
            step = 2;
        }

        boolean argumentsDefined = step == 3;
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i += step) {
            Object key = parameters[i];
            Object args;
            Object val;
            if (!argumentsDefined) {
                args = null;
                val = parameters[i + 1];
            } else {
                args = parameters[i + 1];
                val = parameters[i + 2];
                if (!isRestAssuredArguments(args)) {
                    throw new IllegalArgumentException("Illegal argument '" + format(args) + "' passed to body expectation '" + format(key) + "', a list of " + Argument.class.getName() + " is required.");
                }
            }

            if (map.containsKey(key) && collisionStrategy == CollisionStrategy.MERGE) {
                Object currentValue = map.get(key);
                Object value = argumentsDefined ? new ArgsAndValue((List<Argument>) args, val) : val;
                if (currentValue instanceof List) {
                    ((List<Object>) currentValue).add(value);
                } else {
                    map.put(key, new ArrayList<>(Arrays.asList(currentValue, value)));
                }
            } else if (argumentsDefined) {
                map.put(key, new ArgsAndValue((List<Argument>) args, val));
            } else {
                map.put(key, val);
            }
        }

        return (Map) map;
    }

    private static boolean isRestAssuredArguments(Object args) {
        if (!(args instanceof List)) {
            return false;
        }
        for (Object arg : (List<?>) args) {
            if (!(arg instanceof Argument)) {
                return false;
            }
        }
        return true;
    }

    private static Object[] createArgumentArray(String firstParam, Object... parameters) {
        notNull(firstParam, "firstParam");
        if (parameters == null || parameters.length == 0) {
            // The Groovy version returned "[firstParam: new NoParameterValue()] as Object[]". That is an array with a
            // single map entry (whose key is the literal "firstParam"), which createMapFromObjects rejected like this.
            throw new IllegalArgumentException("You must supply at least one key and one value.");
        }

        Object[] params = new Object[parameters.length + 1];
        params[0] = firstParam;
        System.arraycopy(parameters, 0, params, 1, parameters.length);
        return params;
    }

    private static Object[] createArgumentArrayFromKeyAndValue(String firstParam, Object firstValue, Object... parameters) {
        notNull(firstParam, "firstParam");
        notNull(firstValue, "firstValue");
        int numberOfParameters = parameters == null ? 0 : parameters.length;
        Object[] params = new Object[numberOfParameters + 2];
        params[0] = firstParam;
        params[1] = firstValue;
        if (numberOfParameters > 0) {
            System.arraycopy(parameters, 0, params, 2, numberOfParameters);
        }
        return params;
    }

    /**
     * Arguments and the value they belong to. {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} work
     * like the ones Groovy's {@code @Canonical} generated when this class was written in Groovy, except that equals
     * compares the properties with {@link Objects#deepEquals(Object, Object)} instead of Groovy's {@code ==}.
     */
    public static class ArgsAndValue {
        private List<Argument> args;
        private Object value;

        public ArgsAndValue() {
        }

        public ArgsAndValue(List<Argument> args) {
            this.args = args;
        }

        public ArgsAndValue(List<Argument> args, Object value) {
            this.args = args;
            this.value = value;
        }

        public List<Argument> getArgs() {
            return args;
        }

        public void setArgs(List<Argument> args) {
            this.args = args;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean canEqual(Object other) {
            return other instanceof ArgsAndValue;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ArgsAndValue)) {
                return false;
            }
            ArgsAndValue that = (ArgsAndValue) other;
            return that.canEqual(this)
                    && Objects.equals(getArgs(), that.getArgs())
                    && Objects.deepEquals(getValue(), that.getValue());
        }

        @Override
        public int hashCode() {
            return GroovyStyleHashCode.hashCode(this, getArgs(), getValue());
        }

        @Override
        public String toString() {
            return ArgsAndValue.class.getName() + "(" + format(getArgs()) + ", " + format(getValue()) + ")";
        }
    }
}
