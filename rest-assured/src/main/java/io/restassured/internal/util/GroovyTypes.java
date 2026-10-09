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

/**
 * Recognizes Groovy types by class name so that code can handle them without depending on Groovy.
 */
public class GroovyTypes {

    private static final String GSTRING_CLASS_NAME = "groovy.lang.GString";
    private static final String CLOSURE_CLASS_NAME = "groovy.lang.Closure";

    private GroovyTypes() {
    }

    /**
     * @return {@code true} if {@code object} is a {@code groovy.lang.GString}
     */
    public static boolean isGString(Object object) {
        return isInstanceOfClassNamed(object, GSTRING_CLASS_NAME);
    }

    /**
     * @return {@code true} if {@code object} is a {@code groovy.lang.Closure}
     */
    public static boolean isClosure(Object object) {
        return isInstanceOfClassNamed(object, CLOSURE_CLASS_NAME);
    }

    private static boolean isInstanceOfClassNamed(Object object, String className) {
        if (object == null) {
            return false;
        }
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
