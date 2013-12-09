package com.jayway.restassured.authentication;

import com.jayway.restassured.internal.assertion.AssertParameter;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import java.security.KeyStore;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;

/**
 * A specification for more advanced usages of certificate authentication. Example usage:
 * <pre>
 * given().auth().certificate("keystore.jks", "my_password", certAuthSettings().allowAllHostNames());
 * </pre>
 */
public class CertificateAuthSettings {
    private static final int UNDEFINED_PORT = -1;
    private final String certType;
    private final int port;
    private final KeyStore trustStore;
    private final X509HostnameVerifier x509HostnameVerifier;

    /**
     * Create a new instance of the Certificate Authentication Options with the default settings of:
     * <ul>
     * <li>certType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>port = 443</li>
     * <li>trustStore = null</li>
     * <li>x509HostnameVerifier = {@link org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER}</li>
     * </ul>
     *
     * @see #certAuthSettings()
     */
    public CertificateAuthSettings() {
        this(KeyStore.getDefaultType(), UNDEFINED_PORT, null, STRICT_HOSTNAME_VERIFIER);
    }

    /**
     * @param certType             The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param port                 The port, by default 443.
     * @param trustStore           The trust store, by default no provider is used (<code>null</code>).
     * @param x509HostnameVerifier The X509HostnameVerifier to use
     */
    private CertificateAuthSettings(String certType, int port, KeyStore trustStore, X509HostnameVerifier x509HostnameVerifier) {
        AssertParameter.notNull(certType, "Certificate type");
        this.certType = certType;
        this.port = port;
        this.trustStore = trustStore;
        this.x509HostnameVerifier = x509HostnameVerifier;
    }

    public String getCertType() {
        return certType;
    }

    public int getPort() {
        return port;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }


    /**
     * Configure the CertificateAuthSettings to use strict host name verification (this is the default behavior).
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings strictHostNames() {
        return new CertificateAuthSettings(certType, port, trustStore, STRICT_HOSTNAME_VERIFIER);
    }

    /**
     * Configure the CertificateAuthSettings to allow all host names.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     */
    public CertificateAuthSettings allowAllHostNames() {
        return new CertificateAuthSettings(certType, port, trustStore, ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    /**
     * Configure the CertificateAuthSettings to use the provided {@link X509HostnameVerifier} instance.
     *
     * @return A new CertificateAuthSettings instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     * @see #allowAllHostNames()
     */
    public CertificateAuthSettings x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        return new CertificateAuthSettings(certType, port, trustStore, x509HostnameVerifier);
    }

    /**
     * @return The configured X509HostnameVerifier
     */
    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    /**
     * @param certType The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings certType(String certType) {
        return new CertificateAuthSettings(certType, port, trustStore, x509HostnameVerifier);
    }

    /**
     * @param port The port, by default 443.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings port(int port) {
        return new CertificateAuthSettings(certType, port, trustStore, x509HostnameVerifier);
    }

    /**
     * @param trustStore The trust store to use, by default no (<code>null</code>) trust store is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings trustStore(KeyStore trustStore) {
        return new CertificateAuthSettings(certType, port, trustStore, x509HostnameVerifier);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings with() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same CertificateAuthOptions instance.
     */
    public CertificateAuthSettings using() {
        return this;
    }

    /**
     * Create a new instance of {@link CertificateAuthSettings} with default values. Same as called {@link CertificateAuthSettings#CertificateAuthSettings()} but a bit more "fluent".
     *
     * @return a new instance of {@link CertificateAuthSettings} with default values.
     */
    public static CertificateAuthSettings certAuthSettings() {
        return new CertificateAuthSettings();
    }
}