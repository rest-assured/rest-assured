package com.jayway.restassured.internal.path.xml.mapping

import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder


class GPathResultToXml {

    static def String toXML(GPathResult gPathResult) {
        return new StreamingMarkupBuilder().bind {
            out << gPathResult
        }.toString()
    }

}
