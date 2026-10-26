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

import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.TlsSettings;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The verifiers of {@code strictHostnames()} and {@code allowAllHostnames()}. The HTTP clients check host names
 * themselves: these instances only stand for those two settings, and verify host names the same way when called.
 */
public final class HostnameVerifiers {

    /**
     * Checks the host name against the DNS or IP subject alternative names of the server certificate, or its common
     * name if it has none, with a wildcard only as the whole first label (RFC 2818 and RFC 6125)
     */
    public static final HostnameVerifier STRICT = new HostnameVerifier() {
        @Override
        public boolean verify(String host, SSLSession session) {
            try {
                Certificate[] certificates = session.getPeerCertificates();
                return certificates.length > 0 && certificates[0] instanceof X509Certificate && matches(host, (X509Certificate) certificates[0]);
            } catch (SSLPeerUnverifiedException e) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "STRICT";
        }
    };

    /**
     * Accepts every host name
     */
    public static final HostnameVerifier ALLOW_ALL = new HostnameVerifier() {
        @Override
        public boolean verify(String host, SSLSession session) {
            return true;
        }

        @Override
        public String toString() {
            return "ALLOW_ALL";
        }
    };

    private static final int DNS_NAME = 2;
    private static final int IP_ADDRESS = 7;

    private HostnameVerifiers() {
    }

    /**
     * Sets how the settings check host names from a verifier: {@link #STRICT} and {@link #ALLOW_ALL} stand for what they
     * do, and any other verifier is a custom one.
     */
    public static TlsSettings.Builder hostnames(TlsSettings.Builder tls, HostnameVerifier hostnameVerifier) {
        if (hostnameVerifier == STRICT) {
            return tls.hostnames(HostnameVerification.STRICT);
        } else if (hostnameVerifier == ALLOW_ALL) {
            return tls.hostnames(HostnameVerification.ALLOW_ALL);
        }
        return tls.hostnameVerifier(hostnameVerifier);
    }

    static boolean matches(String host, X509Certificate certificate) {
        String name = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        boolean ipAddress = name.indexOf(':') >= 0 || name.matches("[0-9.]+");
        List<String> alternativeNames = alternativeNames(certificate, ipAddress ? IP_ADDRESS : DNS_NAME);
        if (ipAddress) {
            // Compared as addresses, since the JDK lists ::1 as 0:0:0:0:0:0:0:1, for example
            InetAddress address = ipAddress(name);
            return address != null && alternativeNames.stream().map(HostnameVerifiers::ipAddress).anyMatch(address::equals);
        }
        String asciiName = toAscii(name);
        List<String> names = alternativeNames;
        if (names.isEmpty() && alternativeNames(certificate, IP_ADDRESS).isEmpty()) {
            names = commonNames(certificate);
        }
        return names.stream().anyMatch(pattern -> matchesDnsName(asciiName, toAscii(pattern)));
    }

    private static boolean matchesDnsName(String host, String pattern) {
        if (pattern.startsWith("*.") && pattern.indexOf('.', 2) > 0) {
            int firstDot = host.indexOf('.');
            return firstDot > 0 && host.substring(firstDot).equals(pattern.substring(1));
        }
        return host.equals(pattern);
    }

    /**
     * @return The address of an IP literal, without looking anything up, or <code>null</code> if it isn't one
     */
    private static InetAddress ipAddress(String literal) {
        try {
            if (literal.indexOf(':') >= 0) {
                // In brackets, the JDK only parses it as an IPv6 address and never looks it up
                return InetAddress.getByName("[" + literal + "]");
            } else if (literal.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                byte[] address = new byte[4];
                String[] parts = literal.split("\\.");
                for (int i = 0; i < address.length; i++) {
                    int part = Integer.parseInt(parts[i]);
                    if (part > 255) {
                        return null;
                    }
                    address[i] = (byte) part;
                }
                return InetAddress.getByAddress(address);
            }
        } catch (UnknownHostException e) {
            // Not an IP literal
        }
        return null;
    }

    private static String toAscii(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        try {
            return IDN.toASCII(lowerCase.endsWith(".") ? lowerCase.substring(0, lowerCase.length() - 1) : lowerCase);
        } catch (IllegalArgumentException e) {
            return lowerCase;
        }
    }

    private static List<String> alternativeNames(X509Certificate certificate, int type) {
        List<String> names = new ArrayList<>();
        try {
            Collection<List<?>> entries = certificate.getSubjectAlternativeNames();
            if (entries != null) {
                for (List<?> entry : entries) {
                    if (entry.size() >= 2 && entry.get(0) instanceof Integer && (Integer) entry.get(0) == type && entry.get(1) instanceof String) {
                        names.add((String) entry.get(1));
                    }
                }
            }
        } catch (CertificateParsingException e) {
            // No names to match
        }
        return names;
    }

    private static List<String> commonNames(X509Certificate certificate) {
        List<String> names = new ArrayList<>();
        try {
            for (Rdn rdn : new LdapName(certificate.getSubjectX500Principal().getName()).getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    names.add(String.valueOf(rdn.getValue()));
                }
            }
        } catch (InvalidNameException e) {
            // No names to match
        }
        // The most specific common name, which LdapName lists last
        return names.isEmpty() ? names : List.of(names.get(names.size() - 1));
    }
}
