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

package io.restassured.config;

import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.util.SafeExceptionRethrower;
import org.apache.commons.lang3.Validate;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;

/**
 * Configure SSL for REST Assured.
 * <p/>
 * The following documentation is taken from <a href="HTTP Builder">https://github.com/jgritman/httpbuilder/wiki/SSL</a>:
 * <p>
 * <h1>SSL Configuration</h1>
 * <p/>
 * SSL should, for the most part, "just work." There are a few situations where it is not completely intuitive. You can follow the example below, or see HttpClient's SSLSocketFactory documentation for more information.
 * <p/>
 * <h1>SSLPeerUnverifiedException</h1>
 * <p/>
 * If you can't connect to an SSL website, it is likely because the certificate chain is not trusted. This is an Apache HttpClient issue, but explained here for convenience. To correct the untrusted certificate, you need to import a certificate into an SSL truststore.
 * <p/>
 * First, export a certificate from the website using your browser. For example, if you go to https://dev.java.net in Firefox, you will probably get a warning in your browser. Choose "Add Exception," "Get Certificate," "View," "Details tab." Choose a certificate in the chain and export it as a PEM file. You can view the details of the exported certificate like so:
 * <pre>
 * $ keytool -printcert -file EquifaxSecureGlobaleBusinessCA-1.crt
 * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
 * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
 * Serial number: 1
 * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
 * Certificate fingerprints:
 * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
 * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
 * Signature algorithm name: MD5withRSA
 * Version: 3
 * ....
 * </pre>
 * Now, import that into a Java keystore file:
 * <pre>
 * $ keytool -importcert -alias "equifax-ca" -file EquifaxSecureGlobaleBusinessCA-1.crt -keystore truststore_javanet.jks -storepass test1234
 * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
 * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
 * Serial number: 1
 * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
 * Certificate fingerprints:
 * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
 * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
 * Signature algorithm name: MD5withRSA
 * Version: 3
 * ...
 * Trust this certificate? [no]:  yes
 * Certificate was added to keystore
 * </pre>
 * Now you want to use this truststore in your client:
 * <pre>
 * RestAssured.config = RestAssured.newConfig().sslConfig(new SSLConfig("/truststore_javanet.jks", "test1234");
 * </pre>
 * or
 * <pre>
 * given().config(newConfig().sslConfig(new SSLConfig("/truststore_javanet.jks", "test1234")). ..
 * </pre>
 * </p>
 */
public class SSLConfig implements Config {

    private static final String SSL = "SSL";
    private static final int UNDEFINED_PORT = -1;

    private final Object pathToKeyStore;
    private final Object pathToTrustStore;
    private final String keyStorePassword;
    private final String trustStorePassword;
    private final String keyStoreType;
    private final String trustStoreType;
    private final int port;
    private final KeyStore trustStore;
    private final KeyStore keyStore;
    private final X509HostnameVerifier x509HostnameVerifier;
    private final boolean isUserConfigured;
    private final SSLSocketFactory sslSocketFactory;

    /**
     * @param pathToJks The path to the JKS. REST Assured will first look in the classpath and if not found it will look for the JKS in the local file-system
     * @param password  The store pass
     * @return A new SSLConfig instance
     */
    public SSLConfig keyStore(String pathToJks, String password) {
        Validate.notNull(pathToJks, "Path to JKS on the file system cannot be null");
        Validate.notEmpty(password, "Password cannot be empty");
        return new SSLConfig(pathToJks, pathToTrustStore, password, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Use a keystore located on the file-system. See {@link #keyStore(String, String)} for more details.
     *
     * @param pathToJks The path to JKS file on the file-system
     * @param password  The password for the keystore
     * @return The request specification
     * @see #keyStore(String, String)
     */
    public SSLConfig keyStore(File pathToJks, String password) {
        Validate.notNull(pathToJks, "Path to JKS on the file system cannot be null");
        Validate.notEmpty(password, "Password cannot be empty");
        return new SSLConfig(pathToJks, pathToTrustStore, password, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Uses the user default keystore stored in &lt;user.home&gt;/.keystore
     *
     * @param password - Use null for no password
     * @return The keystore specification
     */
    public SSLConfig keyStore(String password) {
        Validate.notEmpty(password, "Password cannot be empty");
        return keyStore(System.getProperty("user.home") + File.separatorChar + ".keystore", password);
    }

    /**
     * @param pathToTrustStore The path to the trust store. REST Assured will first look in the classpath and if not found it will look for the JKS in the local file-system
     * @param password         The store pass
     * @return A new SSLConfig instance
     */
    public SSLConfig trustStore(String pathToTrustStore, String password) {
        Validate.notNull(pathToTrustStore, "Path to trust store on the file system cannot be null");
        Validate.notEmpty(password, "Password cannot be empty");
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, password, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Use a trustStore located on the file-system. See {@link #trustStore(String, String)} for more details.
     *
     * @param pathToTrustStore The path to trust store file on the file-system
     * @param password         The password for the trustStore
     * @return The request specification
     * @see #trustStore(String, String)
     */
    public SSLConfig trustStore(File pathToTrustStore, String password) {
        Validate.notNull(pathToTrustStore, "Path to trust store on the file system cannot be null");
        Validate.notEmpty(password, "Password cannot be empty");
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, password, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Creates a new SSL Config instance with the following settings:
     * <ul>
     * <li>No keystore</li>
     * <li>No truststore</li>
     * <li>No password</li>
     * <li>{@link java.security.KeyStore#getDefaultType()}</li>
     * <li>No explicit default port</li>
     * <li>No trust store</li>
     * <li>No SSLSocketFactory</li>
     * <li>{@link org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER} as {@link X509HostnameVerifier} implementation</li>
     * </ul>
     */
    public SSLConfig() {
        this(null, null, null, null, KeyStore.getDefaultType(), KeyStore.getDefaultType(), UNDEFINED_PORT, null, null, STRICT_HOSTNAME_VERIFIER, null, false);
    }

    private SSLConfig(Object pathToKeyStore, Object pathToTrustStore, String keyStorePassword, String trustStorePassword,
                      String keyStoreType, String trustStoreType, int port, KeyStore keystore,
                      KeyStore trustStore, X509HostnameVerifier x509HostnameVerifier, SSLSocketFactory sslSocketFactory,
                      boolean isUserConfigured) {
        AssertParameter.notNull(keyStoreType, "Certificate type");
        AssertParameter.notNull(x509HostnameVerifier, X509HostnameVerifier.class);
        this.pathToKeyStore = pathToKeyStore;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePassword = trustStorePassword;
        this.pathToTrustStore = pathToTrustStore;
        this.keyStoreType = keyStoreType;
        this.trustStoreType = trustStoreType;
        this.port = port;
        this.trustStore = trustStore;
        this.keyStore = keystore;
        this.x509HostnameVerifier = x509HostnameVerifier;
        this.isUserConfigured = isUserConfigured;
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * The certificate type, will use {@link java.security.KeyStore#getDefaultType()} by default.
     *
     * @param keystoreType The keystore type.
     * @return A new SSLConfig instance
     */
    public SSLConfig keystoreType(String keystoreType) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keystoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * The trust store type, will use {@link java.security.KeyStore#getDefaultType()} by default.
     *
     * @param trustStoreType The trust store type.
     * @return A new SSLConfig instance
     */
    public SSLConfig trustStoreType(String trustStoreType) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * The port for which REST Assured will apply the SSL configuration. This is advanced configuration and most of the time you do not need to specify a port since
     * REST Assured will apply the configuration to the https port defined in the URI.
     *
     * @param port port.
     * @return A new SSLConfig instance
     */
    public SSLConfig port(int port) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * A trust store to use during SSL/Certificate authentication.
     *
     * @param trustStore The trust store to use.
     * @return A new SSLConfig instance
     */
    public SSLConfig trustStore(KeyStore trustStore) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Use relaxed HTTP validation. This means that you'll trust all hosts regardless if the SSL certificate is invalid. By using this
     * method you don't need to specify a keystore (see {@link #keyStore(String, String)} or trust store (see {@link #trustStore(java.security.KeyStore)}.
     * This method assumes that the protocol for the {@link SSLContext} instance is {@value #SSL}. If this is not the case use {@link #relaxedHTTPSValidation(String)}.
     *
     * @return A new SSLConfig instance.
     */
    public SSLConfig relaxedHTTPSValidation() {
        return relaxedHTTPSValidation(SSL);
    }

    /**
     * Use relaxed HTTP validation. This means that you'll trust all hosts regardless if the SSL certificate is invalid. By using this
     * method you don't need to specify a keystore (see {@link #keyStore(String, String)} or trust store (see {@link #trustStore(java.security.KeyStore)}.
     *
     * @param protocol The standard name of the requested protocol. See the SSLContext section in the <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">Java Cryptography Architecture Standard Algorithm Name Documentation</a> for information about standard protocol names.
     * @return A new SSLConfig instance
     */
    public SSLConfig relaxedHTTPSValidation(String protocol) {
        AssertParameter.notNull(protocol, "Protocol");
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance(protocol);
        } catch (NoSuchAlgorithmException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }

        // Set up a TrustManager that trusts everything
        try {
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }}, new SecureRandom());
        } catch (KeyManagementException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }

        SSLSocketFactory sf = new SSLSocketFactory(sslContext, ALLOW_ALL_HOSTNAME_VERIFIER);
        return sslSocketFactory(sf);
    }

    /**
     * Specify a {@link org.apache.http.conn.ssl.SSLSocketFactory}. This will override settings from trust store as well as keystore and password.
     *
     * @param sslSocketFactory The {@link org.apache.http.conn.ssl.SSLSocketFactory} to use.
     * @return A new SSLConfig instance
     */
    public SSLConfig sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        AssertParameter.notNull(sslSocketFactory, SSLSocketFactory.class);
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Provide a custom {@link X509HostnameVerifier} implementation that'll be used by the {@link org.apache.http.conn.ssl.SSLSocketFactory}. You can replace the
     * {@link X509HostnameVerifier} for example if you want to allow all host names etc.
     *
     * @param x509HostnameVerifier The X509HostnameVerifier to use.
     * @return A new SSLConfig instance
     * @see #allowAllHostnames()
     * @see #strictHostnames()
     */
    public SSLConfig x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, x509HostnameVerifier, sslSocketFactory, true);
    }

    /**
     * Configure the SSLConfig to use strict host name verification (this is the default behavior).
     *
     * @return A new SSLConfig instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#STRICT_HOSTNAME_VERIFIER
     */
    public SSLConfig strictHostnames() {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, STRICT_HOSTNAME_VERIFIER, sslSocketFactory, true);
    }

    /**
     * Configure the SSLConfig to allow all host names.
     *
     * @return A new SSLConfig instance
     * @see org.apache.http.conn.ssl.SSLSocketFactory#ALLOW_ALL_HOSTNAME_VERIFIER
     */
    public SSLConfig allowAllHostnames() {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, port, keyStore, trustStore, ALLOW_ALL_HOSTNAME_VERIFIER, sslSocketFactory, true);
    }

    /**
     * @return A static way to create a new SSLConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static SSLConfig sslConfig() {
        return new SSLConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same SSL config instance.
     */
    public SSLConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same SSL config instance.
     */
    public SSLConfig with() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same SSL config instance.
     */
    public SSLConfig using() {
        return this;
    }

    /**
     * @return The path or file to the JKS
     */
    public Object getPathToKeyStore() {
        return pathToKeyStore;
    }

    /**
     * @return The path or file to the trust store
     */
    public Object getPathToTrustStore() {
        return pathToTrustStore;
    }

    /**
     * @return The password to the JKS
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * @return The password to the trust store
     */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    /**
     * @return The keystore type
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * @return The trust store type
     */
    public String getTrustStoreType() {
        return trustStoreType;
    }

    /**
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The trust store
     */
    public KeyStore getTrustStore() {
        return trustStore;
    }

    /**
     * @return The key store
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * @return The configured SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * @return The X509HostnameVerifier instance.
     */
    public X509HostnameVerifier getX509HostnameVerifier() {
        return x509HostnameVerifier;
    }

    /**
     * @return <code>true</code> if user has configured this SSL Configuration instance, <code>false</code> otherwise.
     */
    public boolean isUserConfigured() {
        return isUserConfigured;
    }

}
