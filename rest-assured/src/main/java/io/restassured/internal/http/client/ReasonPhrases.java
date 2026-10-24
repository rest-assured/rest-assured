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

package io.restassured.internal.http.client;

import java.util.Map;

import static java.util.Map.entry;

/**
 * The standard English reason phrases of the status codes (RFC 9110 and the IANA registry), for a client that gives
 * none, such as the JDK client, so that the status line reads as it did with HttpClient 4, for example
 * {@code HTTP/1.1 200 OK}. A reason phrase of the server's own is lost with such a client.
 */
final class ReasonPhrases {
    private static final Map<Integer, String> PHRASES = Map.ofEntries(
            entry(100, "Continue"), entry(101, "Switching Protocols"), entry(102, "Processing"), entry(103, "Early Hints"),
            entry(200, "OK"), entry(201, "Created"), entry(202, "Accepted"), entry(203, "Non-Authoritative Information"),
            entry(204, "No Content"), entry(205, "Reset Content"), entry(206, "Partial Content"), entry(207, "Multi-Status"),
            entry(208, "Already Reported"), entry(226, "IM Used"),
            entry(300, "Multiple Choices"), entry(301, "Moved Permanently"), entry(302, "Found"), entry(303, "See Other"),
            entry(304, "Not Modified"), entry(305, "Use Proxy"), entry(307, "Temporary Redirect"), entry(308, "Permanent Redirect"),
            entry(400, "Bad Request"), entry(401, "Unauthorized"), entry(402, "Payment Required"), entry(403, "Forbidden"),
            entry(404, "Not Found"), entry(405, "Method Not Allowed"), entry(406, "Not Acceptable"),
            entry(407, "Proxy Authentication Required"), entry(408, "Request Timeout"), entry(409, "Conflict"), entry(410, "Gone"),
            entry(411, "Length Required"), entry(412, "Precondition Failed"), entry(413, "Content Too Large"),
            entry(414, "URI Too Long"), entry(415, "Unsupported Media Type"), entry(416, "Range Not Satisfiable"),
            entry(417, "Expectation Failed"), entry(421, "Misdirected Request"), entry(422, "Unprocessable Content"),
            entry(423, "Locked"), entry(424, "Failed Dependency"), entry(425, "Too Early"), entry(426, "Upgrade Required"),
            entry(428, "Precondition Required"), entry(429, "Too Many Requests"), entry(431, "Request Header Fields Too Large"),
            entry(451, "Unavailable For Legal Reasons"),
            entry(500, "Internal Server Error"), entry(501, "Not Implemented"), entry(502, "Bad Gateway"),
            entry(503, "Service Unavailable"), entry(504, "Gateway Timeout"), entry(505, "HTTP Version Not Supported"),
            entry(506, "Variant Also Negotiates"), entry(507, "Insufficient Storage"), entry(508, "Loop Detected"),
            entry(511, "Network Authentication Required"));

    private ReasonPhrases() {
    }

    /**
     * @return The reason phrase that the client gave, or else the standard one of the status code, or {@code null} if
     * the code has none
     */
    static String orStandard(String reasonPhrase, int statusCode) {
        return reasonPhrase == null || reasonPhrase.isEmpty() ? PHRASES.get(statusCode) : reasonPhrase;
    }
}
