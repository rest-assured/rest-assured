/*
 * Copyright 2012 the original author or authors.
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
    private static final boolean isJacksonPresent = existInCP("org.codehaus.jackson.map.ObjectMapper") && existInCP("org.codehaus.jackson.JsonGenerator");
    private static final boolean isJaxbPresent = existInCP("javax.xml.bind.Binder");
    private static final boolean isGsonPresent = existInCP("com.google.gson.Gson");

    private static boolean existInCP(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch(Throwable e) {
            return false;
        }
    }

    public static boolean isJacksonInClassPath() {
        return isJacksonPresent;
    }

    public static boolean isJAXBInClassPath() {
        return isJaxbPresent;
    }

    public static boolean isGsonInClassPath() {
        return isGsonPresent;
    }
}
