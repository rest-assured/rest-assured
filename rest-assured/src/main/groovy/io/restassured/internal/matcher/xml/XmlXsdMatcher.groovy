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


package io.restassured.internal.matcher.xml

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.w3c.dom.ls.LSResourceResolver

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class XmlXsdMatcher extends BaseMatcher<String> {

  def xsd
  def resourceResolver

  private XmlXsdMatcher(Object xsd) {
    notNull(xsd, "xsd")
    this.xsd = xsd
  }

  public XmlXsdMatcher using(LSResourceResolver resourceResolver) {
    notNull(resourceResolver, LSResourceResolver.class)
    def matcher = new XmlXsdMatcher(xsd)
    matcher.setResourceResolver(resourceResolver)
    matcher
  }

  public XmlXsdMatcher with(LSResourceResolver resourceResolver) {
    using(resourceResolver)
  }

  public static XmlXsdMatcher matchesXsd(String xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(new StringReader(xsd.trim())))
  }

  public static XmlXsdMatcher matchesXsd(InputStream xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(xsd))
  }

  public static XmlXsdMatcher matchesXsd(Reader xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(xsd))
  }

  public static XmlXsdMatcher matchesXsd(File xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(xsd)
  }

  public static XmlXsdMatcher matchesXsd(URL url) {
    notNull(url, "url")
    return new XmlXsdMatcher(url)
  }

  @Override
  boolean matches(Object item) {
    def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    if (resourceResolver != null) {
      factory.setResourceResolver(resourceResolver)
    }
    def schema = factory.newSchema(xsd)
    def validator = schema.newValidator()
    return validator.validate(new StreamSource(new StringReader(item))) == null
  }

  @Override
  void describeTo(Description description) {
    description.appendText("the supplied XSD")
  }

  public static XmlXsdMatcher matchesXsdInClasspath(String path) {
    notNull(path, "Path that points to the XSD in classpath")
    InputStream stream = LoadFromClasspathSupport.loadFromClasspath(path)
    return matchesXsd(stream);
  }
}
