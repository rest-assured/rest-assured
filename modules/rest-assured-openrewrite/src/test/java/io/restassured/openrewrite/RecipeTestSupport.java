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

package io.restassured.openrewrite;

import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.TypeValidation;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecipeTestSupport {

    private static final List<Path> CLASSPATH = JavaParser.runtimeClasspath();
    private static final Pattern TYPE_NAME = Pattern.compile("public (?:final )?(?:class|interface|enum) (\\w+)");

    /**
     * Stubs of the HttpClient 4 types that code written for REST Assured 6 uses, and of REST Assured 6's HTTPBuilder.
     * REST Assured 7 has neither, so the recipes are tested against these.
     */
    private static final String[] REST_ASSURED_6_TYPES = {
            "package org.apache.http.entity.mime;\n" +
            "public enum HttpMultipartMode { STRICT, BROWSER_COMPATIBLE, RFC6532 }",
            "package org.apache.http.conn.ssl;\n" +
            "public interface X509HostnameVerifier extends javax.net.ssl.HostnameVerifier { }",
            "package org.apache.http.conn.ssl;\n" +
            "public class SSLSocketFactory {\n" +
            "    public static final X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = null;\n" +
            "    public static final X509HostnameVerifier BROWSER_COMPATIBLE_HOSTNAME_VERIFIER = null;\n" +
            "    public static final X509HostnameVerifier STRICT_HOSTNAME_VERIFIER = null;\n" +
            "}",
            "package org.apache.http.params;\n" +
            "public interface CoreConnectionPNames {\n" +
            "    String CONNECTION_TIMEOUT = \"http.connection.timeout\";\n" +
            "    String SO_TIMEOUT = \"http.socket.timeout\";\n" +
            "}",
            "package org.apache.http.params;\n" +
            "public interface CoreProtocolPNames {\n" +
            "    String USER_AGENT = \"http.useragent\";\n" +
            "    String USE_EXPECT_CONTINUE = \"http.protocol.expect-continue\";\n" +
            "}",
            "package org.apache.http.client.params;\n" +
            "public interface ClientPNames {\n" +
            "    String CONN_MANAGER_TIMEOUT = \"http.conn-manager.timeout\";\n" +
            "    String COOKIE_POLICY = \"http.protocol.cookie-policy\";\n" +
            "    String HANDLE_REDIRECTS = \"http.protocol.handle-redirects\";\n" +
            "}",
            "package org.apache.http.client.params;\n" +
            "public final class CookiePolicy {\n" +
            "    public static final String BROWSER_COMPATIBILITY = \"compatibility\";\n" +
            "    public static final String IGNORE_COOKIES = \"ignoreCookies\";\n" +
            "}",
            "package org.apache.http.client.config;\n" +
            "public final class CookieSpecs {\n" +
            "    public static final String STANDARD = \"standard\";\n" +
            "    public static final String DEFAULT = \"default\";\n" +
            "    public static final String IGNORE_COOKIES = \"ignoreCookies\";\n" +
            "}",
            "package org.apache.http.cookie.params;\n" +
            "public interface CookieSpecPNames {\n" +
            "    String DATE_PATTERNS = \"http.protocol.cookie-datepatterns\";\n" +
            "}",
            "package io.restassured.internal.http;\n" +
            "public class HTTPBuilder { }"
    };

    /**
     * REST Assured 6's configuration classes with the methods that take or return HttpClient 4 types and the port
     * settings, which 7.0 removed, and the other methods the tests call, PreemptiveBasicAuthScheme with its
     * authenticate(HTTPBuilder) and AuthenticationContext with the port argument of certificate(..).
     * They replace the classes of REST Assured 7 when the sources are parsed, as REST Assured 6 is on the classpath of
     * a project that the recipes migrate.
     */
    private static final String[] REST_ASSURED_6_CONFIGURATION = {
            "package io.restassured.config;\n" +
            "import org.apache.http.entity.mime.HttpMultipartMode;\n" +
            "public class HttpClientConfig {\n" +
            "    public static HttpClientConfig httpClientConfig() { return new HttpClientConfig(); }\n" +
            "    public HttpClientConfig setParam(String parameterName, Object parameterValue) { return this; }\n" +
            "    public HttpClientConfig reuseHttpClientInstance() { return this; }\n" +
            "    public HttpClientConfig httpMultipartMode(HttpMultipartMode httpMultipartMode) { return this; }\n" +
            "    public HttpMultipartMode httpMultipartMode() { return null; }\n" +
            "}",
            "package io.restassured.config;\n" +
            "import org.apache.http.conn.ssl.X509HostnameVerifier;\n" +
            "public class SSLConfig {\n" +
            "    public static SSLConfig sslConfig() { return new SSLConfig(); }\n" +
            "    public SSLConfig x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) { return this; }\n" +
            "    public X509HostnameVerifier getX509HostnameVerifier() { return null; }\n" +
            "    public SSLConfig port(int port) { return this; }\n" +
            "    public SSLConfig allowAllHostnames() { return this; }\n" +
            "}",
            "package io.restassured.authentication;\n" +
            "import org.apache.http.conn.ssl.X509HostnameVerifier;\n" +
            "public class CertificateAuthSettings {\n" +
            "    public static CertificateAuthSettings certAuthSettings() { return new CertificateAuthSettings(); }\n" +
            "    public CertificateAuthSettings x509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) { return this; }\n" +
            "    public CertificateAuthSettings port(int port) { return this; }\n" +
            "    public CertificateAuthSettings allowAllHostnames() { return this; }\n" +
            "}",
            "package io.restassured.authentication;\n" +
            "import org.apache.http.conn.ssl.X509HostnameVerifier;\n" +
            "public class CertAuthScheme {\n" +
            "    public void setX509HostnameVerifier(X509HostnameVerifier x509HostnameVerifier) { }\n" +
            "    public void setPort(int port) { }\n" +
            "    public void setKeyStorePassword(String keyStorePassword) { }\n" +
            "}",
            "package io.restassured.authentication;\n" +
            "public interface AuthenticationContext {\n" +
            "    void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, java.security.KeyStore keyStore,\n" +
            "                     Object trustStorePath, String trustStorePassword, String trustStoreType, java.security.KeyStore trustStore,\n" +
            "                     int port, javax.net.ssl.HostnameVerifier hostnameVerifier, javax.net.ssl.SSLContext sslContext);\n" +
            "}",
            "package io.restassured.authentication;\n" +
            "import io.restassured.internal.http.HTTPBuilder;\n" +
            "public class PreemptiveBasicAuthScheme implements AuthenticationScheme {\n" +
            "    public void authenticate(HTTPBuilder httpBuilder) { }\n" +
            "    public void authenticate(AuthenticationContext context) { }\n" +
            "}"
    };

    // Emptied first, so that a stub removed from the arrays doesn't stay on the classpath. In the target directory of
    // the module, wherever the tests are run from.
    private static final Path OUTPUT = emptyDirectory("recipe-test-support");
    // Compiled once, since dependsOn(..) doesn't work together with classpath(..). The configuration classes come
    // before REST Assured 7 on the classpath, so they replace its classes.
    private static final List<Path> WITH_TYPES = classpath(compile("rest-assured-6-types", REST_ASSURED_6_TYPES, CLASSPATH), CLASSPATH);
    private static final List<Path> WITH_CONFIGURATION = classpath(compile("rest-assured-6-configuration", REST_ASSURED_6_CONFIGURATION, WITH_TYPES), WITH_TYPES);

    private RecipeTestSupport() {
    }

    /**
     * Parses the sources with the test classpath, which has REST Assured, and the stubs of the types that REST Assured 7
     * removed, so that they're type attributed, and checks that the Java sources the recipe changed compile against
     * the current REST Assured.
     */
    static void defaults(RecipeSpec spec) {
        defaults(spec, WITH_TYPES);
    }

    /**
     * {@link #defaults(RecipeSpec)} for code that calls the methods of the configuration classes that take or return
     * HttpClient 4 types, which 7.0 removed: parses the sources with REST Assured 6's configuration classes.
     */
    static void defaultsWithRestAssured6Configuration(RecipeSpec spec) {
        defaults(spec, WITH_CONFIGURATION);
    }

    private static void defaults(RecipeSpec spec, List<Path> classpath) {
        spec.parser(JavaParser.fromJavaVersion().classpath(classpath))
                .afterRecipe(RecipeTestSupport::assertChangedSourcesCompile);
    }

    /**
     * For code written for REST Assured 6 that calls methods which 7.0 removed and the stubs don't have, such as the
     * JsonConfig methods taking JsonPathConfig.NumberReturnType: those calls have no type on the test classpath. The
     * changed sources must still compile against the current REST Assured.
     */
    static void allowingRemovedMethods(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).constructorInvocations(false).build());
    }

    private static void assertChangedSourcesCompile(RecipeRun run) throws IOException {
        List<JavaFileObject> sources = run.getChangeset().getAllResults().stream()
                .map(Result::getAfter)
                .filter(source -> source instanceof J.CompilationUnit)
                // Code that a search recipe marks is code to change by hand
                .filter(source -> !source.printAll().contains("/*~~"))
                .map(RecipeTestSupport::toJavaFileObject)
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            return;
        }
        // With the HttpClient 4 types that the changed sources may still use
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean compiles = compile(sources, WITH_TYPES, Files.createDirectories(OUTPUT.resolve("changed-sources")), diagnostics);
        assertTrue(compiles, () -> "The changed sources don't compile: " + diagnostics.getDiagnostics());
    }

    private static Path compile(String name, String[] stubs, List<Path> classpath) {
        List<JavaFileObject> sources = Stream.of(stubs).map(stub -> {
            Matcher type = TYPE_NAME.matcher(stub);
            type.find();
            return javaFileObject(type.group(1) + ".java", stub);
        }).collect(Collectors.toList());
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try {
            Path output = Files.createDirectories(OUTPUT.resolve(name));
            if (!compile(sources, classpath, output, diagnostics)) {
                throw new IllegalStateException("The stubs don't compile: " + diagnostics.getDiagnostics());
            }
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path emptyDirectory(String name) {
        try {
            // The test classes are in target/test-classes
            Path directory = Path.of(RecipeTestSupport.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolveSibling(name);
            if (Files.exists(directory)) {
                try (Stream<Path> paths = Files.walk(directory)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                        Files.delete(path);
                    }
                }
            }
            return Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean compile(List<JavaFileObject> sources, List<Path> classpath, Path output, DiagnosticCollector<JavaFileObject> diagnostics) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String path = classpath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        return compiler.getTask(null, null, diagnostics,
                List.of("-proc:none", "-nowarn", "-classpath", path, "-d", output.toString()), null, sources).call();
    }

    private static List<Path> classpath(Path first, List<Path> rest) {
        return Stream.concat(Stream.of(first), rest.stream()).collect(Collectors.toList());
    }

    private static JavaFileObject toJavaFileObject(SourceFile source) {
        return javaFileObject(source.getSourcePath().toString().replace(File.separatorChar, '/'), source.printAll());
    }

    private static JavaFileObject javaFileObject(String path, String content) {
        return new SimpleJavaFileObject(URI.create("string:///" + path), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return content;
            }
        };
    }
}
