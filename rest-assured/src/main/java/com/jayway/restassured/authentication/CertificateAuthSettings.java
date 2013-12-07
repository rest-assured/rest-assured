package com.jayway.restassured.authentication;

import com.jayway.restassured.internal.assertion.AssertParameter;

import java.security.KeyStore;

/**
 * A specification for more advanced usages of certificate authentication. Example usage:
 */
public class CertificateAuthSettings {
    private final String certType;
    private final int port;
    private final KeystoreProvider keyStoreProvider;
    private final boolean checkServerHostname;

    /**
     * Create a new instance of the Certificate Authentication Options with the default settings of:
     * <ul>
     * <li>certType = {@link java.security.KeyStore#getDefaultType()}</li>
     * <li>port = 443</li>
     * <li>keyStoreProvider = null</li>
     * <li>checkServerHostname = true</li>
     * </ul>
     * @see #certAuthSettings()
     */
    public CertificateAuthSettings() {
        this(KeyStore.getDefaultType(), 443, null, true);
    }

    /**
     * @param certType            The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @param port                The port, by default 443.
     * @param keyStoreProvider    The keystore provider, by default no provider is used.
     * @param checkServerHostname <code>true</code> REST Assured should verify that the host name of the server match the host name of the certificate, <code>false</code> otherwise.
     */
    private CertificateAuthSettings(String certType, int port, KeystoreProvider keyStoreProvider, boolean checkServerHostname) {
        AssertParameter.notNull(certType, "Certificate type");
        this.certType = certType;
        this.port = port;
        this.keyStoreProvider = keyStoreProvider;
        this.checkServerHostname = checkServerHostname;
    }

    public String getCertType() {
        return certType;
    }

    public int getPort() {
        return port;
    }

    public KeystoreProvider getKeyStoreProvider() {
        return keyStoreProvider;
    }

    public boolean shouldCheckServerHostname() {
        return checkServerHostname;
    }

    /**
     * @param checkServerHostname <code>true</code> REST Assured should verify that the host name of the server match the host name of the certificate, <code>false</code> otherwise.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings checkServerHostname(boolean checkServerHostname) {
        return new CertificateAuthSettings(certType, port, keyStoreProvider, checkServerHostname);
    }

    /**
     * @param certType The certificate type, by default {@link java.security.KeyStore#getDefaultType()}.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings certType(String certType) {
        return new CertificateAuthSettings(certType, port, keyStoreProvider, checkServerHostname);
    }

    /**
     * @param port The port, by default 443.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings port(int port) {
        return new CertificateAuthSettings(certType, port, keyStoreProvider, checkServerHostname);
    }

    /**
     * @param keyStoreProvider The keystore provider, by default no (<code>null</code>) provider is used.
     * @return A new instance of {@link CertificateAuthSettings} with the updated setting.
     */
    public CertificateAuthSettings keyStoreProvider(KeystoreProvider keyStoreProvider) {
        return new CertificateAuthSettings(certType, port, keyStoreProvider, checkServerHostname);
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
