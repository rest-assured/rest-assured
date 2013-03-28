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


package com.jayway.restassured.internal

import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.internal.path.json.ConfigurableJsonSlurper
import com.jayway.restassured.parsing.Parser
import com.jayway.restassured.response.Response

import static com.jayway.restassured.parsing.Parser.*

class ContentParser {
    def parse(Response response, ResponseParserRegistrar rpr, RestAssuredConfig config) {
        Parser parser = rpr.getParser(response.contentType())
        def content;
        def bodyAsInputStream = response.asInputStream()
        if (parser == null) {
            content = bodyAsInputStream
        } else {
            switch (parser) {
                case JSON:
                    content = new ConfigurableJsonSlurper(config.getJsonPathConfig().shouldRepresentJsonNumbersAsBigDecimal()).
                            parse(new InputStreamReader(new BufferedInputStream(bodyAsInputStream)))
                    break;
                case XML:
                    content = new XmlSlurper().parse(bodyAsInputStream)
                    break
                case HTML:
                    content = new XmlSlurper(new org.ccil.cowan.tagsoup.Parser()).parse(bodyAsInputStream);
                    break
                case TEXT:
                default:
                    content = bodyAsInputStream
            }
        }
        content
    }
}
