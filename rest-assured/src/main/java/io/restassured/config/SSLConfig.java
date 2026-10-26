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
import io.restassured.internal.http.HostnameVerifiers;
import io.restassured.internal.util.SafeExceptionRethrower;
import org.apache.commons.lang3.Validate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

/**
 * Configure SSL for REST Assured.
 * <p/>
 * The following documentation is taken from <a href="HTTP Builder">https://github.com/jgritman/httpbuilder/wiki/SSL</a>:
 * <p>
 * <h1>SSL Configuration</h1>
 * <p/>
 * SSL should, for the most part, "just work." There are a few situations where it is not completely intuitive. You can follow the example below.
 * <p/>
 * <h1>SSLPeerUnverifiedException</h1>
 * <p/>
 * If you can't connect to an SSL website, it is likely because the certificate chain is not trusted. To correct the untrusted certificate, you need to import a certificate into an SSL truststore.
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
 * RestAssured.config = RestAssured.config().sslConfig(sslConfig().trustStore("/truststore_javanet.jks", "test1234"));
 * </pre>
 * or
 * <pre>
 * given().config(config().sslConfig(sslConfig().trustStore("/truststore_javanet.jks", "test1234"))). ..
 * </pre>
 * </p>
 */
public class SSLConfig implements Config {

    private static final String SSL = "SSL";

    private final Object pathToKeyStore;
    private final Object pathToTrustStore;
    private final String keyStorePassword;
    private final String trustStorePassword;
    private final String keyStoreType;
    private final String trustStoreType;
    private final KeyStore trustStore;
    private final KeyStore keyStore;
    private final HostnameVerifier hostnameVerifier;
    private final String relaxedProtocol;
    private final boolean isUserConfigured;
    private final SSLContext sslContext;

    /**
     * Use a key store that holds the client certificate (and its private key) to send to a server that asks for one
     * (client certificate authentication, also known as mutual TLS). The password is used both for the key store and
     * for the private key in it. The key store doesn't affect which server certificates are trusted: that's decided by
     * the trust store if one is configured (see {@link #trustStore(String, String)}) and by the JVM's default trust otherwise.
     *
     * @param pathToJks The path to the JKS. REST Assured will first look in the classpath and if not found it will look for the JKS in the local file-system
     * @param password  The store pass
     * @return A new SSLConfig instance
     */
    public SSLConfig keyStore(String pathToJks, String password) {
        Validate.notNull(pathToJks, "Path to JKS on the file system cannot be null");
        Validate.notEmpty(password, "Password cannot be empty");
        return new SSLConfig(pathToJks, pathToTrustStore, password, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
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
        return new SSLConfig(pathToJks, pathToTrustStore, password, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
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
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, password, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
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
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, password, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * Creates a new SSL Config instance with the following settings:
     * <ul>
     * <li>No keystore</li>
     * <li>No truststore</li>
     * <li>No password</li>
     * <li>{@link java.security.KeyStore#getDefaultType()}</li>
     * <li>No trust store</li>
     * <li>No SSLContext</li>
     * <li>Strict host name verification (see {@link #strictHostnames()})</li>
     * </ul>
     */
    public SSLConfig() {
        this(null, null, null, null, KeyStore.getDefaultType(), KeyStore.getDefaultType(), null, null, HostnameVerifiers.STRICT, null, null, false);
    }

    private SSLConfig(Object pathToKeyStore, Object pathToTrustStore, String keyStorePassword, String trustStorePassword,
                      String keyStoreType, String trustStoreType, KeyStore keystore,
                      KeyStore trustStore, HostnameVerifier hostnameVerifier, SSLContext sslContext, String relaxedProtocol,
                      boolean isUserConfigured) {
        AssertParameter.notNull(keyStoreType, "Certificate type");
        AssertParameter.notNull(hostnameVerifier, HostnameVerifier.class);
        this.pathToKeyStore = pathToKeyStore;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePassword = trustStorePassword;
        this.pathToTrustStore = pathToTrustStore;
        this.keyStoreType = keyStoreType;
        this.trustStoreType = trustStoreType;
        this.trustStore = trustStore;
        this.keyStore = keystore;
        this.hostnameVerifier = hostnameVerifier;
        this.relaxedProtocol = relaxedProtocol;
        this.isUserConfigured = isUserConfigured;
        this.sslContext = sslContext;
    }

    /**
     * The certificate type, will use {@link java.security.KeyStore#getDefaultType()} by default.
     *
     * @param keystoreType The keystore type.
     * @return A new SSLConfig instance
     */
    public SSLConfig keystoreType(String keystoreType) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keystoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * The trust store type, will use {@link java.security.KeyStore#getDefaultType()} by default.
     *
     * @param trustStoreType The trust store type.
     * @return A new SSLConfig instance
     */
    public SSLConfig trustStoreType(String trustStoreType) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * A trust store to use during SSL/Certificate authentication.
     *
     * @param trustStore The trust store to use.
     * @return A new SSLConfig instance
     */
    public SSLConfig trustStore(KeyStore trustStore) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * A key store to use during SSL/Certificate authentication.
     *
     * @param keyStore The key store to use.
     * @return A new SSLConfig instance
     */
    public SSLConfig keyStore(KeyStore keyStore) {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * Use relaxed HTTP validation. This means that you'll trust all hosts regardless if the SSL certificate is invalid.
     * By using this method you don't need to specify a trust store (see {@link #trustStore(java.security.KeyStore)}.
     * If you need to send an SSL certificate, then you can specify a key store (see {@link #keyStore(File, String)}
     * and a key store type (see {@link #keystoreType(String)}. The key store is read when a request is sent.
     * This method assumes that the protocol for the {@link SSLContext} instance is {@value #SSL}. If this is not the case use {@link #relaxedHTTPSValidation(String)}.
     *
     * @return A new SSLConfig instance.
     */
    public SSLConfig relaxedHTTPSValidation() {
        return relaxedHTTPSValidation(SSL);
    }

    /**
     * Use relaxed HTTP validation. This means that you'll trust all hosts regardless if the SSL certificate is invalid.
     * By using this method you don't need to specify a trust store (see {@link #trustStore(java.security.KeyStore)}.
     * If you need to send an SSL certificate, then you can specify a key store (see {@link #keyStore(File, String)}
     * and a key store type (see {@link #keystoreType(String)}. The key store is read when a request is sent.
     *
     * @param protocol The standard name of the requested protocol. See the SSLContext section in the <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">Java Cryptography Architecture Standard Algorithm Name Documentation</a> for information about standard protocol names.
     * @return A new SSLConfig instance
     */
    public SSLConfig relaxedHTTPSValidation(String protocol) {
        AssertParameter.notNull(protocol, "Protocol");
        try {
            // Fails here for an unknown protocol. The context is created with the key store when the request is sent.
            SSLContext.getInstance(protocol);
        } catch (NoSuchAlgorithmException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, null, protocol, true);
    }

    /**
     * Use an {@link SSLContext} that you have set up yourself, for example with your own key and trust managers. This
     * overrides the trust store and key store settings, and relaxed HTTPS validation. Host names are still verified as configured by
     * {@link #hostnameVerifier(HostnameVerifier)}, {@link #allowAllHostnames()} or {@link #strictHostnames()} (the default).
     *
     * @param sslContext The SSL context to use.
     * @return A new SSLConfig instance
     */
    public SSLConfig sslContext(SSLContext sslContext) {
        AssertParameter.notNull(sslContext, SSLContext.class);
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, null, true);
    }

    /**
     * Provide a custom {@link HostnameVerifier} that decides whether the host name of the server matches its
     * certificate. You can use it for example to allow a specific host name. It isn't used with
     * {@link #relaxedHTTPSValidation()}, which allows every host name.
     *
     * @param hostnameVerifier The HostnameVerifier to use.
     * @return A new SSLConfig instance
     * @see #allowAllHostnames()
     * @see #strictHostnames()
     */
    public SSLConfig hostnameVerifier(HostnameVerifier hostnameVerifier) {
        AssertParameter.notNull(hostnameVerifier, HostnameVerifier.class);
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, hostnameVerifier, sslContext, relaxedProtocol, true);
    }

    /**
     * Configure the SSLConfig to use strict host name verification (this is the default behavior).
     *
     * @return A new SSLConfig instance
     */
    public SSLConfig strictHostnames() {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, HostnameVerifiers.STRICT, sslContext, relaxedProtocol, true);
    }

    /**
     * Configure the SSLConfig to allow all host names.
     *
     * @return A new SSLConfig instance
     */
    public SSLConfig allowAllHostnames() {
        return new SSLConfig(pathToKeyStore, pathToTrustStore, keyStorePassword, trustStorePassword, keyStoreType, trustStoreType, keyStore, trustStore, HostnameVerifiers.ALLOW_ALL, sslContext, relaxedProtocol, true);
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
     * @return The SSL context set by {@link #sslContext(SSLContext)}, or {@code null} if none is set.
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }

    /**
     * @return The host name verifier, either the one set by {@link #hostnameVerifier(HostnameVerifier)} or the one
     * chosen by {@link #strictHostnames()} (the default) or {@link #allowAllHostnames()}.
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * @return The protocol given to {@link #relaxedHTTPSValidation(String)} ("SSL" for
     * {@link #relaxedHTTPSValidation()}), or {@code null} if HTTPS validation isn't relaxed.
     */
    public String getRelaxedHTTPSValidationProtocol() {
        return relaxedProtocol;
    }

    /**
     * @return <code>true</code> if user has configured this SSL Configuration instance, <code>false</code> otherwise.
     */
    @Override
    public boolean isUserConfigured() {
        return isUserConfigured;
    }
}
