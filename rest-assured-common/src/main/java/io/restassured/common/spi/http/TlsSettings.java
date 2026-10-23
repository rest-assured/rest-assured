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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * The TLS settings as the user gave them: key store, trust store, an {@link SSLContext} of their own, relaxed
 * validation and how host names are checked. Nothing is loaded until {@link #createSslContext()} is called. Equal
 * settings create equivalent contexts; a {@link KeyStore}, {@link SSLContext} or {@link HostnameVerifier} is compared
 * by identity.
 */
public final class TlsSettings {

    private final KeyMaterial keyStore;
    private final KeyMaterial trustStore;
    private final SSLContext sslContext;
    private final String relaxedProtocol;
    private final HostnameVerification hostnames;
    private final HostnameVerifier hostnameVerifier;

    private TlsSettings(Builder builder) {
        this.keyStore = builder.keyStore;
        this.trustStore = builder.trustStore;
        this.sslContext = builder.sslContext;
        this.relaxedProtocol = builder.relaxedProtocol;
        this.hostnames = builder.hostnames;
        this.hostnameVerifier = builder.hostnameVerifier;
    }

    /**
     * @return a builder of settings without stores or context of their own, which check host names strictly
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the key store, which holds the client certificate
     */
    public KeyMaterial keyStore() {
        return keyStore;
    }

    /**
     * @return the trust store, which decides which server certificates are trusted. Without one the JVM's default trust is used.
     */
    public KeyMaterial trustStore() {
        return trustStore;
    }

    /**
     * @return the user's own context, which is used instead of the stores, or {@code null}
     */
    public SSLContext sslContext() {
        return sslContext;
    }

    /**
     * @return the protocol of the context that trusts every certificate and every host name, or {@code null} if
     * certificates are validated
     */
    public String relaxedProtocol() {
        return relaxedProtocol;
    }

    /**
     * @return how host names are checked, unless validation is relaxed
     */
    public HostnameVerification hostnames() {
        return hostnames;
    }

    /**
     * @return the verifier for {@link HostnameVerification#CUSTOM}, otherwise {@code null}
     */
    public HostnameVerifier hostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Creates the context to connect with: the user's own context if there is one; with relaxed validation, a context
     * of that protocol that trusts every certificate and sends the client certificate of the key store; otherwise a
     * TLS context with the key store and the trust store. Stores are loaded from their paths on every call.
     *
     * @return the context
     * @throws IOException              if a store can't be read
     * @throws GeneralSecurityException if a store or its key can't be loaded
     * @throws IllegalArgumentException if the private key of the key store needs a password that isn't set
     */
    public SSLContext createSslContext() throws IOException, GeneralSecurityException {
        if (sslContext != null) {
            return sslContext;
        }
        KeyStore keys = keyStore.load();
        if (relaxedProtocol != null) {
            SSLContext context = SSLContext.getInstance(relaxedProtocol);
            context.init(keyManagers(keys, false), new TrustManager[]{new TrustAll()}, new SecureRandom());
            return context;
        }
        KeyStore trust = trustStore.load();
        SSLContext context = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = null;
        if (trust != null) {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(trust);
            trustManagers = factory.getTrustManagers();
        }
        // Certificate authentication reads the same file as key store and trust store
        boolean keyStoreFileIsAlsoTrustStore = keyStore.keyStore() == null && keyStore.path() != null && keyStore.path().equals(trustStore.path());
        context.init(keyManagers(keys, keyStoreFileIsAlsoTrustStore), trustManagers, null);
        return context;
    }

    private KeyManager[] keyManagers(KeyStore keys, boolean keyStoreFileIsAlsoTrustStore) throws GeneralSecurityException {
        if (keys == null) {
            return null;
        }
        String password = keyStore.password();
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        try {
            factory.init(keys, password == null ? null : password.toCharArray());
        } catch (UnrecoverableKeyException e) {
            // A key store without password is fine when its keys don't need one (for example a PKCS#11 key store),
            // so this is only reported once reading a key has failed
            if (password == null) {
                throw new IllegalArgumentException("The private key in the key store can't be read without a password. " +
                        "Set the key store password with SSLConfig.keyStore(String), for example " +
                        "config(sslConfig().keyStore(keyStore).keyStore(\"password\")), or, for certificate authentication, " +
                        "with the key store password given to RestAssured.certificate(..).", e);
            }
            if (keyStoreFileIsAlsoTrustStore) {
                UnrecoverableKeyException exception = new UnrecoverableKeyException("The private key in " + keyStore.path() +
                        " can't be read with the given password. The file given to certificate(..) is used as key store as well as " +
                        "trust store, so its private key must have the password of the file. Use RestAssured.certificate(trustStorePath, " +
                        "trustStorePassword, keyStorePath, keyStorePassword, CertificateAuthSettings) to use separate files, or an " +
                        "empty key store path to use the file only as trust store.");
                exception.initCause(e);
                throw exception;
            }
            throw e;
        }
        return factory.getKeyManagers();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TlsSettings)) return false;
        TlsSettings that = (TlsSettings) o;
        return keyStore.equals(that.keyStore) && trustStore.equals(that.trustStore) && sslContext == that.sslContext &&
                Objects.equals(relaxedProtocol, that.relaxedProtocol) && hostnames == that.hostnames && hostnameVerifier == that.hostnameVerifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyStore, trustStore, System.identityHashCode(sslContext), relaxedProtocol, hostnames, System.identityHashCode(hostnameVerifier));
    }

    @Override
    public String toString() {
        return "TlsSettings[keyStore=" + keyStore + ", trustStore=" + trustStore + ", sslContext=" + sslContext + ", relaxedProtocol=" +
                relaxedProtocol + ", hostnames=" + hostnames + ", hostnameVerifier=" + hostnameVerifier + "]";
    }

    /**
     * Builds {@link TlsSettings}.
     */
    public static final class Builder {
        private KeyMaterial keyStore = new KeyMaterial(null, null, KeyStore.getDefaultType(), null);
        private KeyMaterial trustStore = new KeyMaterial(null, null, KeyStore.getDefaultType(), null);
        private SSLContext sslContext;
        private String relaxedProtocol;
        private HostnameVerification hostnames = HostnameVerification.STRICT;
        private HostnameVerifier hostnameVerifier;

        private Builder() {
        }

        public Builder keyStore(KeyMaterial keyStore) {
            this.keyStore = Objects.requireNonNull(keyStore, "keyStore");
            return this;
        }

        public Builder trustStore(KeyMaterial trustStore) {
            this.trustStore = Objects.requireNonNull(trustStore, "trustStore");
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * @param relaxedProtocol the protocol of a context that trusts every certificate and host name, or {@code null}
         */
        public Builder relaxedProtocol(String relaxedProtocol) {
            this.relaxedProtocol = relaxedProtocol;
            return this;
        }

        /**
         * @param hostnames {@link HostnameVerification#STRICT} or {@link HostnameVerification#ALLOW_ALL}
         */
        public Builder hostnames(HostnameVerification hostnames) {
            if (hostnames == HostnameVerification.CUSTOM) {
                throw new IllegalArgumentException("Use hostnameVerifier(..) for a custom verifier");
            }
            this.hostnames = Objects.requireNonNull(hostnames, "hostnames");
            this.hostnameVerifier = null;
            return this;
        }

        /**
         * @param hostnameVerifier the verifier that decides, which makes the verification {@link HostnameVerification#CUSTOM}
         */
        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = Objects.requireNonNull(hostnameVerifier, "hostnameVerifier");
            this.hostnames = HostnameVerification.CUSTOM;
            return this;
        }

        /**
         * @throws IllegalStateException if both an SSLContext and relaxed validation are set
         */
        public TlsSettings build() {
            if (sslContext != null && relaxedProtocol != null) {
                throw new IllegalStateException("An SSLContext and relaxed HTTPS validation can't be used together, since " +
                        "relaxed validation creates an SSLContext of its own. Set only one of them.");
            }
            return new TlsSettings(this);
        }
    }

    // Trusts every certificate. As an X509ExtendedTrustManager it also makes the JSSE skip the host name check that a
    // client asks for through the endpoint identification algorithm, and the algorithm constraints check, so a
    // certificate signed with a disabled algorithm such as MD5withRSA is trusted too.
    private static final class TrustAll extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
