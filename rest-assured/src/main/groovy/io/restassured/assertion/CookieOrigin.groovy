package io.restassured.assertion

import io.restassured.specification.RequestSpecification


class CookieOrigin {

    def host

    CookieOrigin(host) {
        this.host = host
    }

    static CookieOrigin create(RequestSpecification requestSpecification) {
        return new CookieOrigin(URI.create(requestSpecification.getURI()).getHost())
    }
}
