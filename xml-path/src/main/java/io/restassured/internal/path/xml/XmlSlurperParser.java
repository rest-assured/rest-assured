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

package io.restassured.internal.path.xml;

import groovy.xml.XmlSlurper;
import groovy.xml.slurpersupport.GPathResult;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Internal: parses an XML or HTML document with a configured {@link XmlSlurper}, so that callers that evaluate
 * GPath expressions against the result don't need to depend on Groovy types themselves.
 */
public class XmlSlurperParser {

    private XmlSlurperParser() {
    }

    /**
     * Parse an XML or HTML document.
     *
     * @param document                The document, either a {@link String} or an {@link InputStream}.
     * @param html                    <code>true</code> to parse the document leniently as HTML (using TagSoup), in which case
     *                                <code>validating</code>, <code>namespaceAware</code> and <code>allowDocTypeDeclaration</code> are not used.
     * @param validating              Whether the XML parser should validate the document.
     * @param namespaceAware          Whether the XML parser should be namespace aware.
     * @param allowDocTypeDeclaration Whether the XML parser should allow DOCTYPE declarations.
     * @param properties              Properties to set on the underlying parser (applied before the features).
     * @param features                Features to set on the underlying parser.
     * @param namespacesToDeclare     Namespaces to declare on the result, or <code>null</code> to not declare any namespaces.
     * @return The parsed document (a {@link GPathResult}).
     */
    public static Object parse(Object document, boolean html, boolean validating, boolean namespaceAware, boolean allowDocTypeDeclaration,
                               Map<String, Object> properties, Map<String, Boolean> features, Map<String, String> namespacesToDeclare)
            throws IOException, SAXException, ParserConfigurationException {
        final XmlSlurper slurper = html ? new XmlSlurper(new org.ccil.cowan.tagsoup.Parser()) : new XmlSlurper(validating, namespaceAware, allowDocTypeDeclaration);
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            slurper.setProperty(property.getKey(), property.getValue());
        }
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            slurper.setFeature(feature.getKey(), feature.getValue());
        }

        final GPathResult result = document instanceof InputStream ? slurper.parse((InputStream) document) : slurper.parseText((String) document);
        if (namespacesToDeclare != null) {
            result.declareNamespace(namespacesToDeclare);
        }
        return result;
    }
}
