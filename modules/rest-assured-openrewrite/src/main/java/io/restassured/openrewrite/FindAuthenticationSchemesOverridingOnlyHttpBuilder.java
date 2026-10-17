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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;

public class FindAuthenticationSchemesOverridingOnlyHttpBuilder extends Recipe {

    private static final String HTTP_BUILDER = "io.restassured.internal.http.HTTPBuilder";
    private static final String AUTHENTICATION_CONTEXT = "io.restassured.authentication.AuthenticationContext";
    private static final List<String> AUTHENTICATION_SCHEMES = List.of(
            "BasicAuthScheme", "CertAuthScheme", "ExplicitNoAuthScheme", "FormAuthScheme", "NTLMAuthScheme", "NoAuthScheme",
            "OAuth2Scheme", "OAuthScheme", "PreemptiveBasicAuthScheme", "PreemptiveOAuth2HeaderScheme");
    private static final String MESSAGE = "REST Assured 7.0 calls authenticate(AuthenticationContext) instead of " +
            "authenticate(HTTPBuilder), so this override won't be called. Override authenticate(AuthenticationContext) instead.";

    @Override
    public String getDisplayName() {
        return "Find authentication schemes that only override `authenticate(HTTPBuilder)`";
    }

    @Override
    public String getDescription() {
        return "Find subclasses of REST Assured's authentication schemes, such as `BasicAuthScheme`, that override " +
                "`authenticate(HTTPBuilder)` but not `authenticate(AuthenticationContext)`. REST Assured 7.0 only " +
                "calls `authenticate(AuthenticationContext)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(HTTP_BUILDER, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                JavaType.Method type = md.getMethodType();
                if (type == null || !isAuthenticate(type, HTTP_BUILDER)) {
                    return md;
                }
                JavaType.FullyQualified declaringType = type.getDeclaringType();
                boolean extendsAuthenticationScheme = AUTHENTICATION_SCHEMES.stream()
                        .anyMatch(scheme -> TypeUtils.isAssignableTo("io.restassured.authentication." + scheme, declaringType));
                boolean overridesAuthenticationContext = declaringType.getMethods().stream()
                        .anyMatch(m -> isAuthenticate(m, AUTHENTICATION_CONTEXT));
                return extendsAuthenticationScheme && !overridesAuthenticationContext ? SearchResult.found(md, MESSAGE) : md;
            }
        });
    }

    private static boolean isAuthenticate(JavaType.Method method, String parameterType) {
        return "authenticate".equals(method.getName()) && method.getParameterTypes().size() == 1 &&
                TypeUtils.isOfClassType(method.getParameterTypes().get(0), parameterType);
    }
}
