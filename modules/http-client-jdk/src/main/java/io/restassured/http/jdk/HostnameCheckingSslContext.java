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

package io.restassured.http.jdk;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.BiFunction;

/**
 * An {@link SSLContext} that checks host names its own way instead of the way the JDK HTTP client asks for. The JDK
 * client always asks the engine to check the host name (the endpoint identification algorithm {@code HTTPS}) and
 * has no host name verifier, so the engines of this context drop that algorithm, and, with a verifier, ask it once the
 * handshake has finished. The certificate is still checked by the trust managers of the wrapped context.
 * <p>
 * A host name that the verifier rejects fails the handshake with an {@link SSLPeerUnverifiedException}, which the
 * client reports as the cause of an {@link SSLException}.
 */
final class HostnameCheckingSslContext extends SSLContext {

    /**
     * @param context  the context whose engines are used
     * @param verifier decides whether the host name matches the session, or {@code null} to accept every host name
     */
    HostnameCheckingSslContext(SSLContext context, HostnameVerifier verifier) {
        super(new Spi(context, verifier), context.getProvider(), context.getProtocol());
    }

    private static final class Spi extends SSLContextSpi {
        private final SSLContext delegate;
        private final HostnameVerifier verifier;

        Spi(SSLContext delegate, HostnameVerifier verifier) {
            this.delegate = delegate;
            this.verifier = verifier;
        }

        @Override
        protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom random) {
            throw new UnsupportedOperationException("The wrapped context is already initialized");
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return delegate.getSocketFactory();
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return delegate.getServerSocketFactory();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            return new Engine(delegate.createSSLEngine(), verifier);
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            return new Engine(delegate.createSSLEngine(host, port), verifier);
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return delegate.getServerSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return delegate.getClientSessionContext();
        }

        @Override
        protected SSLParameters engineGetDefaultSSLParameters() {
            return delegate.getDefaultSSLParameters();
        }

        @Override
        protected SSLParameters engineGetSupportedSSLParameters() {
            return delegate.getSupportedSSLParameters();
        }
    }

    // Delegates everything, except that it drops the endpoint identification algorithm and runs the verifier
    private static final class Engine extends SSLEngine {
        private final SSLEngine delegate;
        private final HostnameVerifier verifier;
        // TLS 1.3 also reports FINISHED for messages after the handshake, such as a new session ticket
        private volatile boolean verified;

        Engine(SSLEngine delegate, HostnameVerifier verifier) {
            super(delegate.getPeerHost(), delegate.getPeerPort());
            this.delegate = delegate;
            this.verifier = verifier;
        }

        private SSLEngineResult verified(SSLEngineResult result) throws SSLException {
            if (verifier != null && !verified && result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                if (!verifier.verify(delegate.getPeerHost(), delegate.getSession())) {
                    throw new SSLPeerUnverifiedException("Host name '" + delegate.getPeerHost() + "' was rejected by the configured HostnameVerifier " + verifier);
                }
                verified = true;
            }
            return result;
        }

        @Override
        public void setSSLParameters(SSLParameters parameters) {
            parameters.setEndpointIdentificationAlgorithm(null);
            delegate.setSSLParameters(parameters);
        }

        @Override
        public SSLParameters getSSLParameters() {
            return delegate.getSSLParameters();
        }

        @Override
        public SSLEngineResult wrap(ByteBuffer[] sources, int offset, int length, ByteBuffer destination) throws SSLException {
            return verified(delegate.wrap(sources, offset, length, destination));
        }

        @Override
        public SSLEngineResult unwrap(ByteBuffer source, ByteBuffer[] destinations, int offset, int length) throws SSLException {
            return verified(delegate.unwrap(source, destinations, offset, length));
        }

        @Override
        public Runnable getDelegatedTask() {
            return delegate.getDelegatedTask();
        }

        @Override
        public void closeInbound() throws SSLException {
            delegate.closeInbound();
        }

        @Override
        public boolean isInboundDone() {
            return delegate.isInboundDone();
        }

        @Override
        public void closeOutbound() {
            delegate.closeOutbound();
        }

        @Override
        public boolean isOutboundDone() {
            return delegate.isOutboundDone();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return delegate.getEnabledCipherSuites();
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            delegate.setEnabledCipherSuites(suites);
        }

        @Override
        public String[] getSupportedProtocols() {
            return delegate.getSupportedProtocols();
        }

        @Override
        public String[] getEnabledProtocols() {
            return delegate.getEnabledProtocols();
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            delegate.setEnabledProtocols(protocols);
        }

        @Override
        public SSLSession getSession() {
            return delegate.getSession();
        }

        @Override
        public SSLSession getHandshakeSession() {
            return delegate.getHandshakeSession();
        }

        @Override
        public void beginHandshake() throws SSLException {
            delegate.beginHandshake();
        }

        @Override
        public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
            return delegate.getHandshakeStatus();
        }

        @Override
        public void setUseClientMode(boolean mode) {
            delegate.setUseClientMode(mode);
        }

        @Override
        public boolean getUseClientMode() {
            return delegate.getUseClientMode();
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            delegate.setNeedClientAuth(need);
        }

        @Override
        public boolean getNeedClientAuth() {
            return delegate.getNeedClientAuth();
        }

        @Override
        public void setWantClientAuth(boolean want) {
            delegate.setWantClientAuth(want);
        }

        @Override
        public boolean getWantClientAuth() {
            return delegate.getWantClientAuth();
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            delegate.setEnableSessionCreation(flag);
        }

        @Override
        public boolean getEnableSessionCreation() {
            return delegate.getEnableSessionCreation();
        }

        @Override
        public String getApplicationProtocol() {
            return delegate.getApplicationProtocol();
        }

        @Override
        public String getHandshakeApplicationProtocol() {
            return delegate.getHandshakeApplicationProtocol();
        }

        @Override
        public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
            delegate.setHandshakeApplicationProtocolSelector(selector);
        }

        @Override
        public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
            return delegate.getHandshakeApplicationProtocolSelector();
        }
    }
}
