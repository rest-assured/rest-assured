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

import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;

/**
 * Converts an object to a String the way Groovy does when it coerces a value to String, for example when a method
 * declared to return String returns something else. Code ported from Groovy uses it where that String is visible to users.
 * <p>
 * Groovy renders arrays and collections as {@code [a, b]}, maps as {@code [a:1]} (and {@code [:]} when empty),
 * {@code char[]} as its characters, Groovy ranges and everything else by {@code toString()}, and DOM elements as
 * pretty-printed XML. Nested values are rendered the same way, with {@code null} as {@code "null"}.
 */
public final class GroovyStringConversion {

    private static final String GROOVY_RANGE = "groovy.lang.Range";

    private GroovyStringConversion() {
    }

    /**
     * @return {@code null} for {@code null}, the String itself for a String, otherwise what Groovy renders for the object.
     */
    public static String castToString(Object object) {
        if (object == null || object instanceof String) {
            return (String) object;
        }
        return format(object);
    }

    /**
     * @return What Groovy code {@code object.toString()} returns for a non-null object. Groovy calls the
     * {@code DefaultGroovyMethods.toString(..)} extension method, which renders like {@link #castToString(Object)}, unless the
     * object's class overrides {@code toString()} below the class that the extension method is defined for
     * ({@code AbstractMap}, {@code AbstractCollection} or {@code Object}) and Groovy can call that override. Groovy can't
     * call an override declared in a non-public class of a package that isn't open to it, such as
     * {@code Collections.unmodifiableMap(..)}.
     */
    public static String callToString(Object object) {
        Class<?> type = object.getClass();
        if (type.isArray()) {
            return format(object);
        }
        Class<?> extensionMethodType = object instanceof AbstractMap ? AbstractMap.class
                : object instanceof AbstractCollection ? AbstractCollection.class : Object.class;
        return callableToStringDeclaringClass(type).isAssignableFrom(extensionMethodType) ? format(object) : object.toString();
    }

    private static Class<?> callableToStringDeclaringClass(Class<?> type) {
        Module module = GroovyStringConversion.class.getModule();
        for (Class<?> current = type; current != Object.class; current = current.getSuperclass()) {
            String packageName = current.getPackageName();
            boolean callable = current.getModule().isOpen(packageName, module)
                    || (Modifier.isPublic(current.getModifiers()) && current.getModule().isExported(packageName, module));
            if (callable && declaresToString(current)) {
                return current;
            }
        }
        return Object.class;
    }

    private static boolean declaresToString(Class<?> type) {
        try {
            type.getDeclaredMethod("toString");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static String format(Object object) {
        if (object == null) {
            return "null";
        }
        if (object.getClass().isArray()) {
            if (object instanceof Object[]) {
                return formatArray((Object[]) object);
            }
            if (object instanceof char[]) {
                return new String((char[]) object);
            }
            return formatPrimitiveArray(object);
        }
        if (isGroovyRange(object.getClass())) {
            return object.toString();
        }
        if (object instanceof Collection) {
            return formatCollection((Collection<?>) object);
        }
        if (object instanceof Map) {
            return formatMap((Map<?, ?>) object);
        }
        if (object instanceof Element) {
            return serialize((Element) object);
        }
        return object.toString();
    }

    private static String formatArray(Object[] array) {
        StringBuilder buffer = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(array[i] == array ? "(this array)" : format(array[i]));
        }
        return buffer.append(']').toString();
    }

    private static String formatPrimitiveArray(Object array) {
        StringBuilder buffer = new StringBuilder("[");
        for (int i = 0; i < Array.getLength(array); i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(Array.get(array, i));
        }
        return buffer.append(']').toString();
    }

    private static String formatCollection(Collection<?> collection) {
        StringBuilder buffer = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                buffer.append(", ");
            }
            first = false;
            buffer.append(item == collection ? "(this Collection)" : format(item));
        }
        return buffer.append(']').toString();
    }

    private static String formatMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "[:]";
        }
        StringBuilder buffer = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                buffer.append(", ");
            }
            first = false;
            buffer.append(entry.getKey() == map ? "(this Map)" : format(entry.getKey()))
                    .append(':')
                    .append(entry.getValue() == map ? "(this Map)" : format(entry.getValue()));
        }
        return buffer.append(']').toString();
    }

    private static boolean isGroovyRange(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> anInterface : current.getInterfaces()) {
                if (GROOVY_RANGE.equals(anInterface.getName()) || isGroovyRange(anInterface)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Same output as Groovy's {@code XmlUtil.serialize(Element)}.
     */
    private static String serialize(Element element) {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (TransformerConfigurationException ignored) {
            // feature is not supported, ignore
        }
        try {
            factory.setAttribute("indent-number", 2);
        } catch (IllegalArgumentException ignored) {
            // ignore for factories that don't support this
        }
        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.transform(new DOMSource(element), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return writer.toString();
    }
}
