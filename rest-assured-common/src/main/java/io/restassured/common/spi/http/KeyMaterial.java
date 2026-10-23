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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * A key store or trust store as the user gave it: a loaded {@link KeyStore}, or a path to load it from. Equal values
 * load the same store. A loaded store is compared by identity.
 *
 * @param path     a {@link File}, or a class path resource or file path as a {@link String}, or {@code null}
 * @param password the password of the store, or {@code null}
 * @param type     the type of the store, for example {@code PKCS12}, or {@code null} for {@link KeyStore#getDefaultType()}
 * @param keyStore the loaded store, which is used instead of the path, or {@code null}
 */
public record KeyMaterial(Object path, String password, String type, KeyStore keyStore) {

    /**
     * @throws IllegalArgumentException if the path is neither a String nor a File
     */
    public KeyMaterial {
        if (path != null && !(path instanceof String) && !(path instanceof File)) {
            throw new IllegalArgumentException("The path to a key store or trust store must be a String or a java.io.File but was "
                    + path.getClass().getName() + ".");
        }
    }

    @Override
    public String toString() {
        return "KeyMaterial[path=" + path + ", type=" + type + ", keyStore=" + keyStore + "]";
    }

    /**
     * @return the given store, or the store loaded from the path, or {@code null} if there is neither
     */
    public KeyStore load() throws IOException, GeneralSecurityException {
        if (keyStore != null) {
            return keyStore;
        }
        // The type is checked even without a path
        KeyStore store = KeyStore.getInstance(type == null ? KeyStore.getDefaultType() : type);
        if (path == null || path instanceof String && ((String) path).isEmpty()) {
            return null;
        }
        try (InputStream in = open()) {
            store.load(in, password == null ? null : password.toCharArray());
        }
        return store;
    }

    private InputStream open() throws IOException {
        final File file;
        if (path instanceof File) {
            file = (File) path;
        } else {
            String name = (String) path;
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            URL resource = contextClassLoader == null ? null : contextClassLoader.getResource(name);
            if (resource == null) {
                // Finds a path with a leading slash in the class path
                resource = KeyMaterial.class.getResource(name);
            }
            if (resource != null) {
                return resource.openStream();
            }
            file = new File(name);
        }
        return new BufferedInputStream(new FileInputStream(file));
    }
}
