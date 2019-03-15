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
package io.restassured.internal

import io.restassured.internal.http.HTTPBuilder
import org.apache.commons.lang3.Validate
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLContexts
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.X509HostnameVerifier

import java.security.KeyStore

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
import static org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER

class TrustAndKeystoreSpecImpl implements TrustAndKeystoreSpec {

  def keyStorePath
  def String keyStorePassword
  def String keyStoreType
  KeyStore keyStore

  def trustStorePath
  def String trustStorePassword
  def String trustStoreType
  KeyStore trustStore

  def int port
  SSLSocketFactory factory
  X509HostnameVerifier x509HostnameVerifier;

  def void apply(HTTPBuilder builder, int port) {
    if (factory == null) {
      def keyStore = keyStore ?: createStore(keyStoreType, keyStorePath, keyStorePassword)
      def trustStore = trustStore ?: createStore(trustStoreType, trustStorePath, trustStorePassword)
      factory = createSSLSocketFactory(trustStore, keyStore, keyStorePassword)
      factory.setHostnameVerifier(x509HostnameVerifier ?: ALLOW_ALL_HOSTNAME_VERIFIER)
    }
    int portToUse = this.port == -1 ? port : this.port
    builder.client.connectionManager.schemeRegistry.register(new Scheme("https", portToUse, factory)
    )
  }

  private static def createSSLSocketFactory(KeyStore truststore, KeyStore keyStore, String keyPassword) {
    final SSLSocketFactory ssl;
    if (truststore == null) {
      ssl = SSLSocketFactory.getSocketFactory()
    } else {
      ssl = new SSLSocketFactory(SSLContexts.custom()
              .loadKeyMaterial(keyStore, keyPassword != null ? keyPassword.toCharArray() : null)
              .loadTrustMaterial(truststore)
              .build(), BROWSER_COMPATIBLE_HOSTNAME_VERIFIER)
    }
    ssl
  }

  def KeyStore createStore(keyStoreType, keyStorePath, keyStorePassword) {
    def keyStore = KeyStore.getInstance(keyStoreType)
    if (keyStorePath == null || ((keyStorePath instanceof String) && keyStorePath.isEmpty())) {
      return null
    }

    def resource
    if (keyStorePath instanceof File) {
      resource = keyStorePath
    } else {
      resource = Thread.currentThread().getContextClassLoader()?.getResource(keyStorePath)
      if (resource == null) { // To allow for backward compatibility
        resource = getClass().getResource(keyStorePath)
      }

      if (resource == null) { // Fallback to load path as file if not found in classpath
        resource = new File(keyStorePath)
      }
    }

    Validate.notNull(resource, "Couldn't find java keystore file at '$keyStorePath'.")
    resource.withInputStream {
      keyStore.load(it, keyStorePassword?.toCharArray())
    }

    return keyStore
  }
}
