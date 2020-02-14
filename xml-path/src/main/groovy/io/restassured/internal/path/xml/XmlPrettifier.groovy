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

package io.restassured.internal.path.xml

import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils

class XmlPrettifier {

    static String prettify(XmlParser xmlParser, xml) {
        doPrettify { StringWriter stringWriter ->
          if (StringUtils.isBlank(xml)) {
            ""
          } else {
            Node node = xmlParser.parseText(xml)
            def printer = new XmlNodePrinter(new PrintWriter(stringWriter))
            printer.setNamespaceAware(xmlParser.isNamespaceAware())
            printer.setPreserveWhitespace(!xmlParser.isTrimWhitespace())
            printer.print(node)
          }
        }
    }

    static String prettify(GPathResult gPathResult) {
        doPrettify { StringWriter stringWriter -> XmlUtil.serialize(gPathResult, stringWriter) }
    }

    private static def doPrettify(Closure<String> closure) {
        def stringWriter = new StringWriter()
        closure.call(stringWriter);
        def body = stringWriter.toString()
        if (body.endsWith(("\r\n"))) {
            body = body.substring(0, body.length() - 2)
        } else if (body.endsWith("\n")) {
            body = body.substring(0, body.length() - 1)
        }
        body
    }
}
