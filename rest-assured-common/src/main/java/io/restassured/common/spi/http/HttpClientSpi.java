/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.common.spi.http;

/**
 * The HTTP client SPI, through which REST Assured sends requests with an HTTP client library. REST Assured follows
 * redirects, answers authentication challenges, keeps cookies and decodes response bodies itself, so a client only
 * sends one request and returns its response. See {@link HttpClientProvider} for how a client is found.
 * <p>
 * The SPI is experimental in REST Assured 7: it may change in a minor version, and {@link #REVISION} then changes.
 */
public final class HttpClientSpi {

    /**
     * The revision of this SPI. A client module is built for one revision, and REST Assured refuses a module built for
     * another one.
     */
    public static final int REVISION = 1;

    private HttpClientSpi() {
    }
}
