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

package io.restassured.internal.matcher.xml;

import io.restassured.internal.util.SafeExceptionRethrower;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class XmlXsdMatcher extends BaseMatcher<String> {

    // A Source, File or URL
    private Object xsd;
    private Object resourceResolver;

    private XmlXsdMatcher(Object xsd) {
        notNull(xsd, "xsd");
        this.xsd = xsd;
    }

    public XmlXsdMatcher using(LSResourceResolver resourceResolver) {
        notNull(resourceResolver, LSResourceResolver.class);
        XmlXsdMatcher matcher = new XmlXsdMatcher(xsd);
        matcher.setResourceResolver(resourceResolver);
        return matcher;
    }

    public XmlXsdMatcher with(LSResourceResolver resourceResolver) {
        return using(resourceResolver);
    }

    public static XmlXsdMatcher matchesXsd(String xsd) {
        notNull(xsd, "xsd");
        return new XmlXsdMatcher(new StreamSource(new StringReader(xsd.trim())));
    }

    public static XmlXsdMatcher matchesXsd(InputStream xsd) {
        notNull(xsd, "xsd");
        return new XmlXsdMatcher(new StreamSource(xsd));
    }

    public static XmlXsdMatcher matchesXsd(Reader xsd) {
        notNull(xsd, "xsd");
        return new XmlXsdMatcher(new StreamSource(xsd));
    }

    public static XmlXsdMatcher matchesXsd(File xsd) {
        notNull(xsd, "xsd");
        return new XmlXsdMatcher(xsd);
    }

    public static XmlXsdMatcher matchesXsd(URL url) {
        notNull(url, "url");
        return new XmlXsdMatcher(url);
    }

    public static XmlXsdMatcher matchesXsdInClasspath(String path) {
        notNull(path, "Path that points to the XSD in classpath");
        InputStream stream = LoadFromClasspathSupport.loadFromClasspath(path);
        if (stream == null) {
            throw new IllegalArgumentException(String.format("Couldn't find the XSD \"%s\" in classpath.", path));
        }
        return matchesXsd(stream);
    }

    @Override
    public boolean matches(Object item) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (resourceResolver != null) {
                factory.setResourceResolver((LSResourceResolver) resourceResolver);
            }
            Schema schema = newSchema(factory);
            schema.newValidator().validate(new StreamSource(new StringReader(XmlMatcherItem.toXml(item, "XSD"))));
            return true;
        } catch (Exception e) {
            // Rethrow checked exceptions such as SAXParseException unchanged
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private Schema newSchema(SchemaFactory factory) throws SAXException {
        if (xsd instanceof File) {
            return factory.newSchema((File) xsd);
        } else if (xsd instanceof URL) {
            return factory.newSchema((URL) xsd);
        }
        return factory.newSchema((Source) xsd);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("the supplied XSD");
    }

    public Object getXsd() {
        return xsd;
    }

    public void setXsd(Object xsd) {
        this.xsd = xsd;
    }

    public Object getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(Object resourceResolver) {
        this.resourceResolver = resourceResolver;
    }
}
