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

package io.restassured.internal.serialization;

import groovy.lang.GString;

import java.util.Locale;
import java.util.UUID;

public class SerializationSupport {

    public static boolean isSerializableCandidate(Object object) {
        if (object == null) {
            return false;
        }
        Class clazz = object.getClass();
        return !(Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)
                || GString.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz) || clazz.isEnum() ||
                Locale.class.isAssignableFrom(clazz) || Class.class.isAssignableFrom(clazz) || UUID.class.isAssignableFrom(clazz));
    }
}
