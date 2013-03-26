package com.jayway.restassured.internal.path.json

import groovy.json.JsonOutput


class JsonPrettifier {

    static def String prettifyJson(String json) {
        return JsonOutput.prettyPrint(json)
    }
}
