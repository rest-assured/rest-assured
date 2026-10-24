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

package io.restassured.internal.http;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.internal.http.HostnameVerifiers.ALLOW_ALL;
import static io.restassured.internal.http.HostnameVerifiers.STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HostnameVerifiersTest {
    private static final int DNS_NAME = 2;
    private static final int IP_ADDRESS = 7;

    @Test
    void a_dns_name_matches_regardless_of_case_and_a_trailing_dot() throws Exception {
        X509Certificate certificate = certificate("CN=other.example", List.of(List.of(DNS_NAME, "Www.Example.com")));

        assertThat(verify("www.example.com", certificate)).isTrue();
        assertThat(verify("WWW.EXAMPLE.COM.", certificate)).isTrue();
        assertThat(verify("example.com", certificate)).isFalse();
        assertThat(verify("other.example", certificate)).isFalse();
    }

    @Test
    void a_wildcard_matches_only_the_whole_first_label() throws Exception {
        X509Certificate certificate = certificate("CN=x", List.of(List.of(DNS_NAME, "*.example.com"), List.of(DNS_NAME, "a*.example.org"), List.of(DNS_NAME, "*.com")));

        assertThat(verify("api.example.com", certificate)).isTrue();
        assertThat(verify("example.com", certificate)).isFalse();
        assertThat(verify("a.api.example.com", certificate)).isFalse();
        assertThat(verify("ab.example.org", certificate)).isFalse();
        // Not a wildcard for a whole top level domain
        assertThat(verify("other.com", certificate)).isFalse();
    }

    @Test
    void an_internationalized_host_matches_its_ascii_form() throws Exception {
        X509Certificate certificate = certificate("CN=x", List.of(List.of(DNS_NAME, "xn--bcher-kva.example")));

        assertThat(verify("b\u00fccher.example", certificate)).isTrue();
    }

    @Test
    void the_most_specific_common_name_is_used_only_without_dns_and_ip_alternative_names() throws Exception {
        assertThat(verify("www.example.com", certificate("CN=www.example.com, CN=example.com", null))).isTrue();
        assertThat(verify("example.com", certificate("CN=www.example.com, CN=example.com", null))).isFalse();
        assertThat(verify("www.example.com", certificate("CN=www.example.com", List.of(List.of(DNS_NAME, "other.example"))))).isFalse();
        assertThat(verify("www.example.com", certificate("CN=www.example.com", List.of(List.of(IP_ADDRESS, "127.0.0.1"))))).isFalse();
    }

    @Test
    void an_ipv4_address_matches_only_an_ip_alternative_name_with_the_same_address() throws Exception {
        assertThat(verify("127.0.0.1", certificate("CN=x", List.of(List.of(IP_ADDRESS, "127.0.0.1"))))).isTrue();
        assertThat(verify("127.0.0.2", certificate("CN=x", List.of(List.of(IP_ADDRESS, "127.0.0.1"))))).isFalse();
        assertThat(verify("127.0.0.1", certificate("CN=x", List.of(List.of(DNS_NAME, "127.0.0.1"))))).isFalse();
        assertThat(verify("127.0.0.1", certificate("CN=127.0.0.1", null))).isFalse();
        assertThat(verify("127.0.0.256", certificate("CN=x", List.of(List.of(IP_ADDRESS, "127.0.0.0"))))).isFalse();
    }

    @Test
    void an_ipv6_address_matches_the_same_address_however_it_is_written() throws Exception {
        // As the JDK lists an IPv6 address of a certificate
        X509Certificate certificate = certificate("CN=x", List.of(List.of(IP_ADDRESS, "0:0:0:0:0:0:0:1")));

        assertThat(verify("[::1]", certificate)).isTrue();
        assertThat(verify("::1", certificate)).isTrue();
        assertThat(verify("0:0:0:0:0:0:0:1", certificate)).isTrue();
        assertThat(verify("[::2]", certificate)).isFalse();
        assertThat(verify("[not:an:address]", certificate)).isFalse();
    }

    @Test
    void strict_rejects_a_session_without_a_verified_peer_and_allow_all_accepts_any_host() throws Exception {
        SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("unverified"));

        assertThat(STRICT.verify("www.example.com", session)).isFalse();
        assertThat(ALLOW_ALL.verify("www.example.com", session)).isTrue();
    }

    private static boolean verify(String host, X509Certificate certificate) throws SSLPeerUnverifiedException {
        SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenReturn(new Certificate[]{certificate});
        return STRICT.verify(host, session);
    }

    private static X509Certificate certificate(String subject, List<List<?>> alternativeNames) throws CertificateParsingException {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(subject));
        when(certificate.getSubjectAlternativeNames()).thenReturn(alternativeNames == null ? null : new ArrayList<>(alternativeNames));
        return certificate;
    }
}
