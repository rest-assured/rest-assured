package com.jayway.restassured.internal.http

import org.junit.Test

import static org.junit.Assert.assertEquals

class CharsetExtractorTest {

    @Test
    def void shouldExtractCharsetFromTheMiddleOfTheDeclaration() throws Exception {
        assertEquals "UTF-8", CharsetExtractor.getCharsetFromContentType("application/ld+json; charset=UTF-8; qs=0.5")
    }

    @Test
    def void shouldExtractCharsetFromTheEndOfTheDeclaration() throws Exception {
        assertEquals "UTF-8", CharsetExtractor.getCharsetFromContentType("application/ld+json; qs=0.5; charset=UTF-8")
    }

    @Test
    def void shouldExtractNullCharsetWhenNotPresent() throws Exception {
        assertEquals null, CharsetExtractor.getCharsetFromContentType("application/ld+json; qs=0.5")
    }
}
