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
