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

import com.jayway.restassured.authentication.KeystoreProvider
import com.jayway.restassured.internal.http.HTTPBuilder
import org.apache.commons.lang3.Validate
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory

import java.security.KeyStore

class KeystoreSpecImpl implements KeystoreSpec, KeystoreProvider {

    def path
    def password

    def void apply(HTTPBuilder builder, int port) {
        def trustStore = build()
        def factory = new SSLSocketFactory(trustStore)
        factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        builder.client.connectionManager.schemeRegistry.register(
                new Scheme("https", factory, port)
        )
    }

    def Boolean canBuild() {
        return true
    }

    def KeyStore build() {
        def keyStore = KeyStore.getInstance(KeyStore.defaultType)
        if (path == null)
            path = System.getProperty("user.home") + File.separatorChar + ".keystore"

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
            keyStore.load(it, password.toCharArray())
        }

        return keyStore
    }

}
