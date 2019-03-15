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

package io.restassured.internal.matcher.xml

class LoadFromClasspathSupport {

  static InputStream loadFromClasspath(String path) {
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)
    if (!stream) {
      // Fallback if not found (this enables paths starting with slash)
      // Note that this fallback doesn't work when using Java 11 (see below)
      stream = getClass().getResourceAsStream(path)
    }

    if (!stream && path.startsWith("/")) {
      // When using Java 11 the previous fallback doesn't work so then we simply check if the path starts with "/" and if so remove it and try again
      stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1))
    }
    stream
  }
}