/*
 * Copyright 2011 the original author or authors.
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

import com.jayway.restassured.specification.KeyStoreSpec
import groovyx.net.http.HTTPBuilder
import java.security.KeyStore
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.commons.lang.Validate

class KeyStoreSpecImpl implements KeyStoreSpec {

  def path
  def password

  def void apply(HTTPBuilder builder, int port) {
    def keyStore = KeyStore.getInstance( KeyStore.defaultType )
    def resource = getClass().getResource(path)
    Validate.notNull(resource, "Couldn't find java keystore file in classpath at '$path'.")
    resource.withInputStream {
      keyStore.load( it, password.toCharArray() )
    }

    def factory = new SSLSocketFactory(keyStore)
    factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
    builder.client.connectionManager.schemeRegistry.register(
            new Scheme("https", factory, port)
    )
  }

}
