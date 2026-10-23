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
 * Credentials for NTLM authentication, which the client answers itself since it's bound to a connection.
 *
 * @param host        the host to authenticate with
 * @param port        the port to authenticate with, or {@code -1} for any
 * @param userName    the user name
 * @param password    the password
 * @param workstation the workstation
 * @param domain      the domain
 */
public record NtlmCredentials(String host, int port, String userName, String password, String workstation, String domain) {

    @Override
    public String toString() {
        return "NtlmCredentials[host=" + host + ", port=" + port + ", userName=" + userName + ", workstation=" + workstation +
                ", domain=" + domain + "]";
    }
}
