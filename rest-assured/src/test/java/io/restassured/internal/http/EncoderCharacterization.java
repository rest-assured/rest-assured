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

package io.restassured.internal.http;

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.runtime.GStringImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared helpers for the request body encoding characterization tests. The expected output of each matrix is kept in a
 * golden file under {@code src/test/resources/encoder-characterization}. The actual output is always written to
 * {@code target/encoder-characterization} so that a difference can be inspected with a plain diff.
 */
final class EncoderCharacterization {

    static final String TEXT = "hello åäö";

    private EncoderCharacterization() {
    }

    /**
     * Creates a GString the same way the Groovy compiler does for {@code "hello ${'åäö'}"}.
     */
    static GString gString() {
        return new GStringImpl(new Object[]{"åäö"}, new String[]{"hello ", ""});
    }

    /**
     * A real Groovy closure. It writes to a Writer or OutputStream argument (the TEXT and BINARY encoders pass one)
     * and returns a String.
     */
    @SuppressWarnings("rawtypes")
    static Closure closure() {
        return (Closure) new GroovyShell().evaluate(
                "return { arg -> if (arg instanceof Writer || arg instanceof OutputStream) { arg << 'closure-body' }; 'closure-result' }");
    }

    static String render(Throwable t, Path tmpDir) {
        if (t instanceof FileNotFoundException || t instanceof NullPointerException) {
            // The messages of these depend on the operating system and the JDK
            return "EXCEPTION " + t.getClass().getName();
        }
        return "EXCEPTION " + t.getClass().getName() + ": " + sanitize(String.valueOf(t.getMessage()), tmpDir);
    }

    static String sanitize(String s, Path tmpDir) {
        String result = s;
        if (tmpDir != null) {
            result = result.replace(tmpDir.toString() + File.separator, "<tmp>/");
        }
        return result.replaceAll("@[0-9a-f]{4,}", "@<hash>")
                .replaceAll("_closure\\d+", "_closure")
                .replaceAll("Script\\d+", "Script")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    static String escape(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int i = b & 0xff;
            if (i >= 0x20 && i < 0x7f && i != '\\') {
                sb.append((char) i);
            } else {
                sb.append(String.format("\\x%02x", i));
            }
        }
        return sb.toString();
    }

    static byte[] readFully(InputStream in) throws IOException {
        try (InputStream is = in) {
            return is.readAllBytes();
        }
    }

    static void assertMatchesGoldenFile(String name, String actual) throws IOException, URISyntaxException {
        Path actualFile = Paths.get("target", "encoder-characterization", name);
        Files.createDirectories(actualFile.getParent());
        Files.write(actualFile, actual.getBytes(StandardCharsets.UTF_8));

        URL expectedResource = EncoderCharacterization.class.getResource("/encoder-characterization/" + name);
        assertThat(expectedResource).as("golden file " + name + " (actual output written to " + actualFile.toAbsolutePath() + ")").isNotNull();
        String expected = new String(Files.readAllBytes(Paths.get(expectedResource.toURI())), StandardCharsets.UTF_8);
        assertThat(actual).as("actual output written to " + actualFile.toAbsolutePath()).isEqualTo(expected);
    }
}
