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
import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class XmlDtdMatcher extends BaseMatcher<String> {

    // A byte[], File or URL, which is read for every match (or an InputStream, which can only be read once)
    private Object dtd;

    private XmlDtdMatcher(Object dtd) {
        notNull(dtd, "dtd");
        this.dtd = dtd;
    }

    public static Matcher<String> matchesDtd(String dtd) {
        notNull(dtd, "dtd");
        return new XmlDtdMatcher(dtd.getBytes());
    }

    public static Matcher<String> matchesDtd(InputStream dtd) {
        notNull(dtd, "dtd");
        try (InputStream stream = dtd) {
            return new XmlDtdMatcher(stream.readAllBytes());
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    public static Matcher<String> matchesDtd(File dtd) {
        notNull(dtd, "file");
        // Fail fast with a FileNotFoundException if the file can't be read
        try (InputStream ignored = new FileInputStream(dtd)) {
            return new XmlDtdMatcher(dtd);
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    public static Matcher<String> matchesDtd(URL url) {
        notNull(url, "url");
        return new XmlDtdMatcher(url);
    }

    public static Matcher<String> matchesDtdInClasspath(String path) {
        notNull(path, "Path that points to the DTD in classpath");
        InputStream stream = LoadFromClasspathSupport.loadFromClasspath(path);
        if (stream == null) {
            throw new IllegalArgumentException(String.format("Couldn't find the DTD \"%s\" in classpath.", path));
        }
        return matchesDtd(stream);
    }

    @Override
    public boolean matches(Object item) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();

            //parse file into DOM
            Document doc = db.parse(new ByteArrayInputStream(XmlMatcherItem.toXml(item, "DTD").getBytes()));
            DOMSource source = new DOMSource(doc);

            //now use a transformer to add the DTD element
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            File file = writeToTempFile();
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
            return true;
        } catch (Exception e) {
            // Rethrow checked exceptions such as SAXParseException unchanged
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private File writeToTempFile() throws IOException {
        File file = File.createTempFile("restassured", "temp");
        file.deleteOnExit();
        try (InputStream inputStream = getInputStream(); OutputStream out = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (IOException | RuntimeException e) {
            file.delete();
            throw e;
        }
        return file;
    }

    private InputStream getInputStream() throws IOException {
        if (dtd instanceof URL) {
            return ((URL) dtd).openConnection().getInputStream();
        } else if (dtd instanceof File) {
            return new FileInputStream((File) dtd);
        } else if (dtd instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) dtd);
        }
        return (InputStream) dtd;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("the supplied DTD");
    }

    public Object getDtd() {
        return dtd;
    }

    public void setDtd(Object dtd) {
        this.dtd = dtd;
    }

    private static class ExceptionThrowingErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }
    }
}
