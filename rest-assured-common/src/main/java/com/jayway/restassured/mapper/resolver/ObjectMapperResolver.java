/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.mapper.resolver;

public class ObjectMapperResolver {
    private static final boolean isJackson1Present = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator");
    private static final boolean isJackson2Present = existInCP("com.fasterxml.jackson.databind.ObjectMapper") && existInCP("com.fasterxml.jackson.core.JsonGenerator");
    private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder");
    private static final boolean isGsonPresent = existInCP("com.google.gson.Gson");

    private static boolean existInCP(String className) {
        return existsInCP(className, ObjectMapperResolver.class.getClassLoader()) || existsInCP(className, Thread.currentThread().getContextClassLoader());
    }

    private static boolean existsInCP(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isJackson1InClassPath() {
        return isJackson1Present;
    }

    public static boolean isJackson2InClassPath() {
        return isJackson2Present;
    }

    public static boolean isJAXBInClassPath() {
        return isJaxbPresent;
    }

    public static boolean isGsonInClassPath() {
        return isGsonPresent;
    }
}
