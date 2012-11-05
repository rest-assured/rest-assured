/*
 * Copyright 2011 the original author or authors.
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

import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody

class MultiPart {
    private static final String OCTET_STREAM = "application/octet-stream"
    private static final String TEXT_PLAIN = "text/plain"

    def content
    def name
    def fileName
    def mimeType

    def getContentBody() {
        if(content instanceof NoParameterValue) {
            content = "";
        }

        if(content instanceof File) {
            new FileBody(content, mimeType ?: OCTET_STREAM)
        } else if(content instanceof InputStream) {
            returnInputStreamBody()
        } else if(content instanceof byte[]) {
            content = new ByteArrayInputStream(content)
            returnInputStreamBody()
        } else if(content instanceof String) {
            returnStringBody(content)
        } else if(content != null) {
            returnStringBody(content.toString())
        } else {
            throw new IllegalArgumentException("Illegal content: $content")
        }
    }

    private def returnStringBody(String content) {
        new StringBody(content, mimeType ?: TEXT_PLAIN, null)
    }

    private def returnInputStreamBody() {
        new InputStreamBody(content, mimeType ?: OCTET_STREAM, fileName ?: "file")
    }
}
