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
 * How a client checks that the certificate of the server is for the host it connects to.
 */
public enum HostnameVerification {
    /**
     * The host must match the certificate as RFC 2818 says, with strict wildcard matching: a wildcard matches exactly
     * one label, so {@code *.example.com} matches {@code a.example.com} but not {@code a.b.example.com}.
     */
    STRICT,
    /**
     * Any host is accepted. The certificate must still be trusted.
     */
    ALLOW_ALL,
    /**
     * {@link TlsSettings#hostnameVerifier()} decides.
     */
    CUSTOM
}
