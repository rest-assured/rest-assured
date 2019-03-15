/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.internal.http

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
    def void shouldExtractCharsetFromTheEndOfTheDeclarationWhenCharsetIsNotLowercase() throws Exception {
      assertEquals "UTF-8", CharsetExtractor.getCharsetFromContentType("application/ld+json; qs=0.5; CharseT=UTF-8")
    }

    @Test
    def void shouldExtractNullCharsetWhenNotPresent() throws Exception {
        assertEquals null, CharsetExtractor.getCharsetFromContentType("application/ld+json; qs=0.5")
    }

    @Test
    def void shouldExtractCharsetIfQuoted() throws Exception {
        assertEquals "UTF-8", CharsetExtractor.getCharsetFromContentType("application/ld+json; charset=\"UTF-8\"")
    }
}
