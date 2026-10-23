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

package io.restassured.internal.support;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The elements of an HTML page, such as a login page, in document order with their attributes. The page is parsed
 * leniently by TagSoup, which closes unclosed tags and lower-cases the names of elements and attributes.
 */
public class HtmlDocument {
    private static final String TAGSOUP_IS_MISSING = "Form authentication and CSRF need TagSoup (org.ccil.cowan.tagsoup:tagsoup) to read the login page. " +
            "Add it to your OSGi framework.";

    private final List<Element> elements;

    private HtmlDocument(List<Element> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    /**
     * @throws IllegalStateException if TagSoup isn't on the class path, which happens only in OSGi
     */
    public static HtmlDocument parse(String html) {
        XMLReader reader = tagSoup();
        List<Element> elements = new ArrayList<>();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                Map<String, String> attributeValues = new HashMap<>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    attributeValues.put(attributes.getLocalName(i), attributes.getValue(i));
                }
                elements.add(new Element(localName, attributeValues));
            }
        });
        try {
            reader.setFeature("http://www.ccil.org/~cowan/tagsoup/features/default-attributes", false);
            reader.parse(new InputSource(new StringReader(html == null ? "" : html)));
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException("Can't parse the HTML page: " + e.getMessage(), e);
        }
        return new HtmlDocument(elements);
    }

    private static XMLReader tagSoup() {
        try {
            return new org.ccil.cowan.tagsoup.Parser();
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException(TAGSOUP_IS_MISSING, e);
        }
    }

    /**
     * @return The elements with the given (lower-case) name, in document order
     */
    public Stream<Element> elements(String name) {
        return elements.stream().filter(element -> element.name.equals(name));
    }

    /**
     * @return The {@code input} elements of the given type, ignoring case. An {@code input} without {@code type} is a
     * text field, as in a browser.
     */
    public Stream<Element> inputsOfType(String type) {
        return elements("input").filter(input -> type.equalsIgnoreCase(input.attributes.getOrDefault("type", "text")));
    }

    /**
     * @return The {@code input} elements with the given name
     */
    public Stream<Element> inputsNamed(String name) {
        return elements("input").filter(input -> name.equals(input.attribute("name")));
    }

    public static class Element {
        private final String name;
        private final Map<String, String> attributes;

        private Element(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        /**
         * @return The value of the attribute, or {@code null} if the element doesn't have it
         */
        public String attribute(String name) {
            return attributes.get(name);
        }
    }
}
