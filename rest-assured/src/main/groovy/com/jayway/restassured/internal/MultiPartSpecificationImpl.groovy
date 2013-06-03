package com.jayway.restassured.internal

import com.jayway.restassured.specification.MultiPartSpecification


class MultiPartSpecificationImpl implements MultiPartSpecification {
    def content
    def String controlName
    def String mimeType
    def String charset
    def String fileName

    def Object getContent() {
        return content
    }

    def String getControlName() {
        return controlName
    }

    def String getMimeType() {
        return mimeType
    }

    def String getCharset() {
        return charset
    }

    def String getFileName() {
        return fileName
    }
}
