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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Set;

public class ReplaceHttpMultipartMode extends Recipe {

    private static final String HTTP_MULTIPART_MODE = "org.apache.http.entity.mime.HttpMultipartMode";
    private static final MethodMatcher HTTP_MULTIPART_MODE_SETTER =
            new MethodMatcher("io.restassured.config.HttpClientConfig httpMultipartMode(" + HTTP_MULTIPART_MODE + ")");
    private static final Set<String> MODES = Set.of(HTTP_MULTIPART_MODE + ".STRICT", HTTP_MULTIPART_MODE + ".BROWSER_COMPATIBLE", HTTP_MULTIPART_MODE + ".RFC6532");

    @Override
    public String getDisplayName() {
        return "Use `HttpClientConfig.multipartMode(MultipartMode)`";
    }

    @Override
    public String getDescription() {
        return "Replace `HttpClientConfig.httpMultipartMode(HttpMultipartMode.X)`, which takes the httpmime enum, with " +
                "`multipartMode(MultipartMode.X)`, which takes REST Assured's own enum.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(HTTP_MULTIPART_MODE_SETTER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!HTTP_MULTIPART_MODE_SETTER.matches(m) || m.getSelect() == null) {
                    return m;
                }
                String mode = RecipeSupport.fieldName(m.getArguments().get(0));
                if (mode == null || !MODES.contains(mode)) {
                    return m;
                }
                maybeAddImport("io.restassured.config.MultipartMode");
                maybeRemoveImport(HTTP_MULTIPART_MODE);
                J.MethodInvocation replaced = JavaTemplate.builder("#{any(io.restassured.config.HttpClientConfig)}.multipartMode(MultipartMode." + mode.substring(HTTP_MULTIPART_MODE.length() + 1) + ")")
                        .imports("io.restassured.config.MultipartMode")
                        .javaParser(RecipeSupport.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                return RecipeSupport.withNameAndArgumentsOf(m, replaced);
            }
        });
    }
}
