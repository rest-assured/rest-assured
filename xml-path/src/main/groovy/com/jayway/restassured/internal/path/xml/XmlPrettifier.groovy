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

package com.jayway.restassured.internal.path.xml

import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil

class XmlPrettifier {

    static def String prettify(XmlParser xmlParser, xml) {
        doPrettify { StringWriter stringWriter ->
            def Node node = xmlParser.parseText(xml);
            def printer = new XmlNodePrinter(new PrintWriter(stringWriter))
            printer.setNamespaceAware(xmlParser.isNamespaceAware())
            printer.setPreserveWhitespace(!xmlParser.isTrimWhitespace())
            printer.print(node)
        }
    }

    static def String prettify(GPathResult gPathResult) {
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
