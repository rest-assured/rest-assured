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

package io.restassured.internal;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A scheme compiled against REST Assured 6.x, which implements only authenticate(HTTPBuilder), is compiled here against
 * stubs of the 6.x types and then loaded with REST Assured 7's {@link AuthenticationScheme}.
 */
class AuthenticationSchemeCompiledAgainstRestAssured6Test {
    private static final String URL = "http://127.0.0.1:1/";

    @TempDir
    Path directory;

    @Test
    void a_scheme_that_implements_only_authenticate_http_builder_fails_with_an_error_that_says_what_to_implement() throws Exception {
        AuthenticationScheme scheme = compileRestAssured6Scheme();

        assertThatThrownBy(() -> given(new RequestSpecBuilder().setAuth(scheme).build()).get(URL))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("The authentication scheme legacy.Rest6Scheme doesn't implement authenticate(io.restassured.authentication.AuthenticationContext), "
                        + "which REST Assured 7 calls instead of authenticate(HTTPBuilder). Implement authenticate(AuthenticationContext) and compile "
                        + "the scheme against REST Assured 7.")
                .hasCauseExactlyInstanceOf(AbstractMethodError.class);
    }

    @Test
    void an_abstract_method_error_from_within_a_scheme_that_implements_authenticate_is_thrown_as_it_is() {
        AbstractMethodError error = new AbstractMethodError("from within the scheme");
        AuthenticationScheme scheme = context -> {
            throw error;
        };

        assertThatThrownBy(() -> given(new RequestSpecBuilder().setAuth(scheme).build()).get(URL)).isSameAs(error);
    }

    private AuthenticationScheme compileRestAssured6Scheme() throws Exception {
        Path sources = directory.resolve("sources");
        Path classes = directory.resolve("classes");
        Path httpBuilder = write(sources, "io/restassured/internal/http/HTTPBuilder.java",
                "package io.restassured.internal.http; public class HTTPBuilder { }");
        Path restAssured6Interface = write(sources, "io/restassured/authentication/AuthenticationScheme.java",
                "package io.restassured.authentication; public interface AuthenticationScheme { void authenticate(io.restassured.internal.http.HTTPBuilder httpBuilder); }");
        Path scheme = write(sources, "legacy/Rest6Scheme.java",
                "package legacy; public class Rest6Scheme implements io.restassured.authentication.AuthenticationScheme { "
                        + "public void authenticate(io.restassured.internal.http.HTTPBuilder httpBuilder) { } }");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-proc:none", "-d", classes.toString(), httpBuilder.toString(), restAssured6Interface.toString(),
                scheme.toString());
        assertThat(result).isZero();

        // Parent first, so the scheme gets REST Assured 7's AuthenticationScheme rather than the stub
        URLClassLoader classLoader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
        return (AuthenticationScheme) classLoader.loadClass("legacy.Rest6Scheme").getDeclaredConstructor().newInstance();
    }

    private static Path write(Path sources, String path, String source) throws Exception {
        Path file = sources.resolve(path);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, source);
    }
}
