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

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Pretty prints XML and HTML for logging, the way Groovy's {@code XmlParser} and {@code XmlNodePrinter} did before
 * REST Assured 7.0.0:
 * <ul>
 * <li>Elements are indented by two spaces, with attributes in the order of the document, in double quotes.</li>
 * <li>An element without children, or with whitespace only, is printed as {@code <x/>}. Text that is only whitespace
 * is dropped, and other text is printed as it is, escaped. An element with text only is printed on one line.</li>
 * <li>In mixed content, text is printed where it is, without indentation or line breaks of its own.</li>
 * <li>CDATA is printed as escaped text. Comments, processing instructions and the XML declaration are dropped.</li>
 * <li>XML isn't namespace aware, so prefixes and {@code xmlns} attributes are printed as they are. A DOCTYPE makes it
 * fail.</li>
 * <li>HTML is parsed leniently by TagSoup, with the default attributes that TagSoup adds, such as {@code method="get"}
 * on a {@code form}.</li>
 * </ul>
 */
class XmlPrettyPrinter {
    private static final String INDENT = "  ";

    static String prettifyXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        if (isBlank(xml)) {
            return "";
        }
        SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return prettify(factory.newSAXParser().getXMLReader(), xml);
    }

    static String prettifyHtml(String html) throws SAXException, IOException {
        if (isBlank(html)) {
            return "";
        }
        return prettify(tagSoup(), html);
    }

    private static XMLReader tagSoup() {
        return new org.ccil.cowan.tagsoup.Parser();
    }

    private static String prettify(XMLReader reader, String document) throws SAXException, IOException {
        TreeBuilder treeBuilder = new TreeBuilder();
        reader.setContentHandler(treeBuilder);
        reader.setErrorHandler(treeBuilder);
        reader.parse(new InputSource(new StringReader(document)));

        StringBuilder output = new StringBuilder();
        print(treeBuilder.root, 0, output);
        return output.substring(0, output.length() - System.lineSeparator().length());
    }

    private static void print(Element element, int level, StringBuilder output) {
        output.append(INDENT.repeat(level)).append('<').append(element.name);
        for (String[] attribute : element.attributes) {
            output.append(' ').append(attribute[0]).append("=\"");
            escape(attribute[1], true, output);
            output.append('"');
        }
        if (element.children.isEmpty()) {
            output.append("/>").append(System.lineSeparator());
            return;
        }
        output.append('>');
        boolean textOnly = element.children.stream().allMatch(String.class::isInstance);
        if (!textOnly) {
            output.append(System.lineSeparator());
        }
        for (Object child : element.children) {
            if (child instanceof Element) {
                print((Element) child, level + 1, output);
            } else {
                escape((String) child, false, output);
            }
        }
        if (!textOnly) {
            output.append(INDENT.repeat(level));
        }
        output.append("</").append(element.name).append('>').append(System.lineSeparator());
    }

    private static void escape(String text, boolean attributeValue, StringBuilder output) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    output.append("&lt;");
                    break;
                case '>':
                    output.append("&gt;");
                    break;
                case '&':
                    output.append("&amp;");
                    break;
                case '"':
                    output.append(attributeValue ? "&quot;" : "\"");
                    break;
                case '\n':
                    output.append(attributeValue ? "&#10;" : "\n");
                    break;
                case '\r':
                    output.append(attributeValue ? "&#13;" : "\r");
                    break;
                default:
                    output.append(c);
            }
        }
    }

    private static final class Element {
        private final String name;
        private final List<String[]> attributes = new ArrayList<>();
        private final List<Object> children = new ArrayList<>();

        private Element(String name) {
            this.name = name;
        }
    }

    /**
     * Builds the tree of elements and text, like Groovy's {@code XmlParser}. Text is collected up to the next start or
     * end tag, and dropped when it's only whitespace.
     */
    private static final class TreeBuilder extends DefaultHandler {
        private final Deque<Element> open = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();
        private Element root;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            addText();
            Element element = new Element(name(localName, qName));
            for (int i = 0; i < attributes.getLength(); i++) {
                element.attributes.add(new String[]{name(attributes.getLocalName(i), attributes.getQName(i)), attributes.getValue(i)});
            }
            if (open.isEmpty()) {
                root = element;
            } else {
                open.peek().children.add(element);
            }
            open.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            addText();
            open.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        private void addText() {
            if (!open.isEmpty() && !text.toString().trim().isEmpty()) {
                open.peek().children.add(text.toString());
            }
            text.setLength(0);
        }

        private static String name(String localName, String qName) {
            return localName == null || localName.isEmpty() ? qName : localName;
        }
    }
}
