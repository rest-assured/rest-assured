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

import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;
import io.restassured.path.xml.exception.XmlPathException;

import java.io.InputStreamReader;
import java.util.Set;

/**
 * Evaluates GPath expressions on XML and HTML documents for REST Assured.
 */
public class GPathXmlProvider implements PathProvider {

    @Override
    public String artifactId() {
        return "xml-path";
    }

    @Override
    public String engineName() {
        return "GPATH";
    }

    @Override
    public Set<DocumentType> documentTypes() {
        return Set.of(DocumentType.XML, DocumentType.HTML);
    }

    @Override
    public String description(DocumentType type) {
        return "XML path";
    }

    @Override
    public Object parse(PathInput input, DocumentType type, PathSettings settings) throws Exception {
        Object document;
        if (input.isText()) {
            document = input.text();
        } else if (input.charset() == null) {
            document = input.stream();
        } else {
            document = new InputStreamReader(input.stream(), input.charset());
        }
        return XmlSlurperParser.parse(document, type == DocumentType.HTML, settings.validating(), settings.namespaceAware(),
                settings.allowDocTypeDeclaration(), settings.properties(), settings.features(),
                settings.namespaceAware() ? settings.declaredNamespaces() : null);
    }

    /**
     * @return The exception that {@code xmlPath} throws when it can't parse the document.
     */
    @Override
    public RuntimeException parseFailure(DocumentType type, Exception cause) {
        return new XmlPathException("Failed to parse the XML document", cause);
    }

    @Override
    public PathResult evaluate(Object document, DocumentType type, String path) {
        XMLAssertion assertion = new XMLAssertion();
        assertion.setKey(path);
        return PathResult.value(assertion.getResult(document, null));
    }
}
