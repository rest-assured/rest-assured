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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

final class RecipeSupport {

    /**
     * Stubs of the REST Assured types that the templates produce, so that the recipes don't need REST Assured on
     * their classpath.
     */
    private static final String[] STUBS = {
            "package io.restassured.config;\n" +
            "public enum MultipartMode { STRICT, BROWSER_COMPATIBLE, RFC6532 }",
            "package io.restassured.config;\n" +
            "import java.time.Duration;\n" +
            "public class HttpClientConfig {\n" +
            "    public HttpClientConfig multipartMode(MultipartMode multipartMode) { return this; }\n" +
            "    public HttpClientConfig connectTimeout(Duration connectTimeout) { return this; }\n" +
            "    public HttpClientConfig readTimeout(Duration readTimeout) { return this; }\n" +
            "    public HttpClientConfig connectionRequestTimeout(Duration connectionRequestTimeout) { return this; }\n" +
            "    public HttpClientConfig keepResponseCookies(boolean keepResponseCookies) { return this; }\n" +
            "    public HttpClientConfig expectContinue(boolean expectContinue) { return this; }\n" +
            "}",
            "package io.restassured.common.config;\n" +
            "public enum NumberReturnType { FLOAT_AND_DOUBLE, BIG_DECIMAL, DOUBLE, BIG_INTEGER }"
    };

    private RecipeSupport() {
    }

    static JavaParser.Builder<?, ?> parser() {
        return JavaParser.fromJavaVersion().dependsOn(STUBS);
    }

    /**
     * @return The fully qualified name of the field that the expression refers to, such as
     * <code>org.example.Type.FIELD</code> for <code>Type.FIELD</code> or a statically imported <code>FIELD</code>, or
     * <code>null</code> if it doesn't refer to a field.
     */
    static @Nullable String fieldName(Expression expression) {
        JavaType.Variable field = null;
        if (expression instanceof J.Identifier) {
            field = ((J.Identifier) expression).getFieldType();
        } else if (expression instanceof J.FieldAccess) {
            field = ((J.FieldAccess) expression).getName().getFieldType();
        }
        JavaType.FullyQualified owner = field == null ? null : TypeUtils.asFullyQualified(field.getOwner());
        return owner == null ? null : owner.getFullyQualifiedName() + "." + field.getName();
    }

    /**
     * Takes the name, arguments and type of the call that a template produced, and keeps the rest of the original
     * call, since a template that replaces a call in a chain of calls changes the line breaks and indentation of the
     * chain.
     */
    static J.MethodInvocation withNameAndArgumentsOf(J.MethodInvocation original, J.MethodInvocation replacement) {
        return original.withName(replacement.getName().withPrefix(original.getName().getPrefix()))
                .withMethodType(replacement.getMethodType())
                .getPadding().withArguments(replacement.getPadding().getArguments());
    }
}
