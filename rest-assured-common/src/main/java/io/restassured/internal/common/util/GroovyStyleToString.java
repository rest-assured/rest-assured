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
package io.restassured.internal.common.util;

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
import java.util.Iterator;
import java.util.Map;

/**
 * Renders values the way a dynamically dispatched {@code value.toString()} does in Groovy, which is what users saw
 * when this code was written in Groovy. For example a {@code byte[]} is rendered as {@code [65, 66]} (not {@code [B@1b6d3586}),
 * a map as {@code [a:1]} (not {@code {a=1}}), a {@code char[]} as the string it holds and a DOM element as XML. Values
 * whose class overrides {@code toString()} in a way Groovy can call use their own {@code toString()}.
 */
public class GroovyStyleToString {

    private GroovyStyleToString() {
    }

    /**
     * @param value The value
     * @return {@code value} rendered like Groovy's {@code value.toString()}.
     */
    public static String toString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof char[]) {
            return new String((char[]) value);
        } else if (value.getClass().isArray()) {
            return format(value);
        } else if ((value instanceof Collection || value instanceof Map || value instanceof Element) && !overridesToString(value.getClass())) {
            // Groovy's toString() extension methods (for Object, AbstractMap and AbstractCollection) only apply when no class
            // below them declares its own toString() that Groovy can call, e.g. ConcurrentHashMap keeps "{a=1}".
            return format(value);
        }
        return value.toString();
    }

    /**
     * @param element The element
     * @return The element as pretty-printed XML, the same as Groovy's {@code XmlUtil.serialize(Element)}.
     */
    public static String serialize(Element element) {
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

    private static boolean overridesToString(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (isCallableByGroovy(current) && declaresToString(current)) {
                return current != Object.class && current != AbstractMap.class && current != AbstractCollection.class;
            }
        }
        return false;
    }

    /**
     * Groovy can call a method of a public class in an exported package, and of any class in a package that is open to
     * it, which includes every class on the class path. It can't call one of a JDK-private class, such as the class
     * behind {@code Collections.unmodifiableMap(..)}.
     */
    private static boolean isCallableByGroovy(Class<?> type) {
        Module caller = GroovyStyleToString.class.getModule();
        Module module = type.getModule();
        String packageName = type.getPackageName();
        return module.isOpen(packageName, caller)
                || (Modifier.isPublic(type.getModifiers()) && module.isExported(packageName, caller));
    }

    private static boolean declaresToString(Class<?> type) {
        try {
            type.getDeclaredMethod("toString");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // Port of Groovy's InvokerHelper.format(Object, verbose = false)
    private static String format(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof char[]) {
            return new String((char[]) value);
        } else if (value.getClass().isArray()) {
            return formatArray(value);
        } else if (isGroovyRange(value.getClass())) {
            return value.toString();
        } else if (value instanceof Collection) {
            return formatCollection((Collection<?>) value);
        } else if (value instanceof Map) {
            return formatMap((Map<?, ?>) value);
        } else if (value instanceof Element) {
            return serialize((Element) value);
        }
        return String.valueOf(value.toString());
    }

    private static boolean isGroovyRange(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> anInterface : current.getInterfaces()) {
                if ("groovy.lang.Range".equals(anInterface.getName()) || isGroovyRange(anInterface)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String formatArray(Object array) {
        StringBuilder builder = new StringBuilder("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(format(Array.get(array, i)));
        }
        return builder.append(']').toString();
    }

    private static String formatCollection(Collection<?> collection) {
        StringBuilder builder = new StringBuilder("[");
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            builder.append(item == collection ? "(this Collection)" : format(item));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    private static String formatMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "[:]";
        }
        StringBuilder builder = new StringBuilder("[");
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            builder.append(entry.getKey() == map ? "(this Map)" : format(entry.getKey()))
                    .append(':')
                    .append(entry.getValue() == map ? "(this Map)" : format(entry.getValue()));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }
}
