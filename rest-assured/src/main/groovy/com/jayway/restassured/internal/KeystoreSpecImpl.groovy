/*
 * Copyright 2013 the original author or authors.
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
package com.jayway.restassured.internal

import com.jayway.restassured.internal.http.HTTPBuilder
import org.apache.commons.lang3.Validate
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.X509HostnameVerifier

import java.security.KeyStore

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER

class KeystoreSpecImpl implements KeystoreSpec {

  def path
  def password

  def String keyStoreType
  def int port
  SSLSocketFactory factory
  KeyStore trustStore
  X509HostnameVerifier x509HostnameVerifier;

  def void apply(HTTPBuilder builder, int port) {
    if (factory == null) {
      def trustStore = trustStore ?: createTrustStore()
      factory = createSSLSocketFactory(trustStore)
      factory.setHostnameVerifier(x509HostnameVerifier ?: ALLOW_ALL_HOSTNAME_VERIFIER)
    }
    int portToUse = this.port == -1 ? port : this.port
    builder.client.connectionManager.schemeRegistry.register(new Scheme("https", portToUse, factory)
    )
  }

  private static def createSSLSocketFactory(KeyStore truststore) {
    final SSLSocketFactory ssl;
    if (truststore == null) {
      ssl = SSLSocketFactory.getSocketFactory()
    } else {
      ssl = new SSLSocketFactory(truststore);
    }
    ssl
  }

  def KeyStore createTrustStore() {
    def keyStore = KeyStore.getInstance(keyStoreType)
    if (path == null)
      return null

    def resource
    if (path instanceof File) {
      resource = path
    } else {
      resource = Thread.currentThread().getContextClassLoader()?.getResource(path)
      if (resource == null) { // To allow for backward compatibility
        resource = getClass().getResource(path)
      }

      if (resource == null) { // Fallback to load path as file if not found in classpath
        resource = new File(path)
      }
    }

    Validate.notNull(resource, "Couldn't find java keystore file at '$path'.")
    resource.withInputStream {
      keyStore.load(it, password?.toCharArray())
    }

    return keyStore
  }
}
