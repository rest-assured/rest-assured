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
import org.hamcrest.Matcher
import org.w3c.dom.Document
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import static io.restassured.internal.common.assertion.AssertParameter.notNull

class XmlDtdMatcher extends BaseMatcher<String> {
  def dtd

  private XmlDtdMatcher(dtd) {
    notNull(dtd, "dtd")
    this.dtd = dtd
  }

  public static Matcher<String> matchesDtd(String dtd) {
    notNull(dtd, "dtd")
    return new XmlDtdMatcher(toInputStream(dtd))
  }

  public static Matcher<String> matchesDtd(InputStream dtd) {
    return new XmlDtdMatcher(dtd);
  }

  public static Matcher<String> matchesDtd(File dtd) {
    notNull(dtd, "file")
    return new XmlDtdMatcher(new FileInputStream(dtd))
  }

  public static Matcher<String> matchesDtd(URL url) {
    notNull(url, "url")
    return new XmlDtdMatcher(url.toString())
  }

  @Override
  boolean matches(Object item) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = factory.newDocumentBuilder();

    //parse file into DOM
    Document doc = db.parse(toInputStream(item));
    DOMSource source = new DOMSource(doc);

    //now use a transformer to add the DTD element
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    File file = writeToTempFile(dtd);
    try {
      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, file.getPath());
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      transformer.transform(source, result);

      factory.setValidating(true);
      db = factory.newDocumentBuilder();
      db.setErrorHandler(new ExceptionThrowingErrorHandler());
      db.parse(new InputSource(new StringReader(writer.toString())));
    } finally {
      file.delete();
    }
    return true
  }

  private File writeToTempFile(dtd) {
    InputStream inputStream = getInputStream(dtd);

    //write the inputStream to a FileOutputStream
    def file = File.createTempFile("restassured", "temp");
    file.deleteOnExit();
    OutputStream out = new FileOutputStream(file);

    int read = 0;
    byte[] bytes = new byte[1024];

    while ((read = inputStream.read(bytes)) != -1) {
      out.write(bytes, 0, read);
    }

    inputStream.close();
    out.flush();
    out.close();
    return file
  }

  private InputStream getInputStream(dtd) {
    if (dtd instanceof URL) {
      URLConnection uc = dtd.openConnection();
      return uc.getInputStream();
    }
    dtd
  }

  @Override
  void describeTo(Description description) {
    description.appendText("the supplied DTD")
  }

  private static ByteArrayInputStream toInputStream(String dtd) {
    return new ByteArrayInputStream(dtd.getBytes())
  }

  static Matcher<String> matchesDtdInClasspath(String path) {
    notNull(path, "Path that points to the DTD in classpath")
    InputStream stream = LoadFromClasspathSupport.loadFromClasspath(path)
    return matchesDtd(stream);
  }

  private static class ExceptionThrowingErrorHandler implements ErrorHandler {
    @Override
    void warning(SAXParseException exception) {
      throw exception;
    }

    @Override
    void error(SAXParseException exception) {
      throw exception;
    }

    @Override
    void fatalError(SAXParseException exception) {
      throw exception;
    }
  }
}