/*
 * Copyright 2013 the original author or authors.
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



package com.jayway.restassured.internal.matcher.xml

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull

class XmlXsdMatcher extends BaseMatcher<String> {

  def xsd;

  private XmlXsdMatcher(Object xsd) {
    notNull(xsd, "xsd")
    this.xsd = xsd
  }

  public static Matcher<String> matchesXsd(String xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(new StringReader(xsd.trim())))
  }

  public static Matcher<String> matchesXsd(InputStream xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(xsd))
  }

  public static Matcher<String> matchesXsd(Reader xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(new StreamSource(xsd))
  }

  public static Matcher<String> matchesXsd(File xsd) {
    notNull(xsd, "xsd")
    return new XmlXsdMatcher(xsd)
  }

  public static Matcher<String> matchesXsd(URL url) {
    notNull(url, "url")
    return new XmlXsdMatcher(url)
  }

  @Override
  boolean matches(Object item) {
    def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    def schema = factory.newSchema(xsd)
    def validator = schema.newValidator()
    return validator.validate(new StreamSource(new StringReader(item))) == null
  }

  @Override
  void describeTo(Description description) {
    description.appendText("the supplied XSD")
  }
}
