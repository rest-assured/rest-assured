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

package io.restassured.internal.matcher.xml;

import io.restassured.internal.util.GroovyTypes;

class XmlMatcherItem {

    private XmlMatcherItem() {
    }

    /**
     * @return The XML that a matcher is asked to match. A {@code groovy.lang.GString} is converted to a String and
     * {@code null} is returned as is.
     * @throws IllegalArgumentException if the item is neither a String nor a GString
     */
    static String toXml(Object item, String matcherName) {
        if (item == null || item instanceof String) {
            return (String) item;
        } else if (GroovyTypes.isGString(item)) {
            return item.toString();
        }
        throw new IllegalArgumentException(String.format("The %s matcher can only match a String but was %s.", matcherName, item.getClass().getName()));
    }
}
