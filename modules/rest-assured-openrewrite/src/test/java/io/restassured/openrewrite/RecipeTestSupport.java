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
import org.openrewrite.test.RecipeSpec;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecipeTestSupport {

    private static final List<Path> CLASSPATH = JavaParser.runtimeClasspath();

    private RecipeTestSupport() {
    }

    /**
     * Parses the sources with the test classpath, which has REST Assured and HttpClient 4, so that they're type
     * attributed, and checks that the sources the recipe changed compile against the current REST Assured.
     */
    static void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(CLASSPATH))
                .afterRecipe(RecipeTestSupport::assertChangedSourcesCompile);
    }

    private static void assertChangedSourcesCompile(RecipeRun run) throws IOException {
        List<JavaFileObject> sources = run.getChangeset().getAllResults().stream()
                .map(Result::getAfter)
                .filter(Objects::nonNull)
                .map(RecipeTestSupport::toJavaFileObject)
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            return;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        String classpath = CLASSPATH.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        Path output = Files.createDirectories(Path.of("target", "changed-sources"));
        boolean compiles = compiler.getTask(null, null, diagnostics,
                List.of("-proc:none", "-nowarn", "-classpath", classpath, "-d", output.toString()), null, sources).call();
        assertTrue(compiles, () -> "The changed sources don't compile: " + diagnostics.getDiagnostics());
    }

    private static JavaFileObject toJavaFileObject(SourceFile source) {
        URI uri = URI.create("string:///" + source.getSourcePath().toString().replace(File.separatorChar, '/'));
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source.printAll();
            }
        };
    }
}
