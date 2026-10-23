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

package io.restassured.internal.common.spi.path;

/**
 * Joins a root path and a key the way GPath, and REST Assured before 7.0.0, does.
 */
public final class GPathRootPath {
    private static final String DOT = ".";

    private GPathRootPath() {
    }

    /**
     * Join the root path and the key. A {@code null} or empty root path gives the key, and an empty key gives the root
     * path. Otherwise they are joined with a dot, unless the root path ends with a dot or the key starts with a dot,
     * {@code ?.}, {@code [}, {@code ?[} or {@code *.}. When both have the dot, one is dropped.
     */
    public static String join(String rootPath, String key) {
        if (rootPath == null || rootPath.isEmpty()) {
            return key;
        } else if (key.isEmpty()) {
            return rootPath;
        } else if (rootPath.endsWith(DOT) && key.startsWith(DOT)) {
            return rootPath + key.substring(DOT.length());
        } else if (!rootPath.endsWith(DOT) && !key.startsWith(DOT) && !key.startsWith("?.") && !key.startsWith("[")
                && !key.startsWith("?[") && !key.startsWith("*.")) {
            return rootPath + DOT + key;
        }
        return rootPath + key;
    }
}
