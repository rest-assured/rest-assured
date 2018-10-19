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