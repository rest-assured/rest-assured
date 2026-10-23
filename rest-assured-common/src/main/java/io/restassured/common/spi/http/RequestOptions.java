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

import java.time.Duration;

/**
 * How one request is sent, as opposed to the {@link ClientSettings} that every request of a client shares.
 *
 * @param readTimeout    how long to wait for the response, or {@code null} for the library's default
 * @param expectContinue whether to send {@code Expect: 100-continue} and wait for the server before sending the body
 * @param ntlm           the NTLM credentials to authenticate with, or {@code null}
 */
public record RequestOptions(Duration readTimeout, boolean expectContinue, NtlmCredentials ntlm) {

    /**
     * No read timeout, no {@code Expect: 100-continue} and no NTLM.
     */
    public static final RequestOptions DEFAULT = new RequestOptions(null, false, null);
}
