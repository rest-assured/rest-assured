package com.jayway.restassured.internal.path.xml

import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil

class GroovyNodeSerializer {
    static String toXML(Node node) {
        return XmlUtil.serialize(new NodeChild(node, null, null))
    }
}
