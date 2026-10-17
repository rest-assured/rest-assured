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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Map;

public class ReplaceHttpClientParams extends Recipe {

    private static final MethodMatcher SET_PARAM = new MethodMatcher("io.restassured.config.HttpClientConfig setParam(java.lang.String, *)");
    private static final String CORE_CONNECTION_PNAMES = "org.apache.http.params.CoreConnectionPNames";
    private static final String CORE_PROTOCOL_PNAMES = "org.apache.http.params.CoreProtocolPNames";
    private static final String CLIENT_PNAMES = "org.apache.http.client.params.ClientPNames";
    private static final String COOKIE_POLICY = "org.apache.http.client.params.CookiePolicy";
    private static final String COOKIE_SPECS = "org.apache.http.client.config.CookieSpecs";
    private static final Map<String, String> PARAMETER_CONSTANTS = Map.of(
            CORE_CONNECTION_PNAMES + ".CONNECTION_TIMEOUT", "http.connection.timeout",
            CORE_CONNECTION_PNAMES + ".SO_TIMEOUT", "http.socket.timeout",
            CLIENT_PNAMES + ".CONN_MANAGER_TIMEOUT", "http.conn-manager.timeout",
            CLIENT_PNAMES + ".COOKIE_POLICY", "http.protocol.cookie-policy",
            CORE_PROTOCOL_PNAMES + ".USE_EXPECT_CONTINUE", "http.protocol.expect-continue");
    private static final Map<String, String> COOKIE_POLICY_CONSTANTS = Map.of(
            COOKIE_POLICY + ".IGNORE_COOKIES", "ignoreCookies",
            COOKIE_SPECS + ".IGNORE_COOKIES", "ignoreCookies",
            COOKIE_SPECS + ".DEFAULT", "default");

    @Override
    public String getDisplayName() {
        return "Replace HttpClient 4 parameters with `HttpClientConfig` settings";
    }

    @Override
    public String getDescription() {
        return "Replace `HttpClientConfig.setParam(..)` for the connection, socket and connection manager timeouts, " +
                "the cookie policy (\"ignoreCookies\" or \"default\") and expect-continue with `connectTimeout(Duration)`, `readTimeout(Duration)`, " +
                "`connectionRequestTimeout(Duration)`, `keepResponseCookies(boolean)` and `expectContinue(boolean)`. " +
                "Only parameters named by a string literal or an HttpClient 4 constant are replaced.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(SET_PARAM), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!SET_PARAM.matches(m) || m.getSelect() == null) {
                    return m;
                }
                Expression value = m.getArguments().get(1);
                String setting = setting(parameterName(m.getArguments().get(0)), value);
                if (setting == null) {
                    return m;
                }
                maybeAddImport("java.time.Duration");
                for (String type : new String[]{CORE_CONNECTION_PNAMES, CORE_PROTOCOL_PNAMES, CLIENT_PNAMES, COOKIE_POLICY, COOKIE_SPECS}) {
                    maybeRemoveImport(type);
                }
                JavaTemplate template = JavaTemplate.builder("#{any(io.restassured.config.HttpClientConfig)}." + setting)
                        .imports("java.time.Duration")
                        .javaParser(RecipeSupport.parser())
                        .build();
                J.MethodInvocation replaced = setting.contains("#{")
                        ? template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), value)
                        : template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                return RecipeSupport.withNameAndArgumentsOf(m, replaced);
            }
        });
    }

    private static @Nullable String parameterName(Expression name) {
        if (name instanceof J.Literal) {
            Object value = ((J.Literal) name).getValue();
            return value instanceof String ? (String) value : null;
        }
        String constant = RecipeSupport.fieldName(name);
        return constant == null ? null : PARAMETER_CONSTANTS.get(constant);
    }

    /**
     * @return The setting that replaces the parameter, with <code>#{any(..)}</code> for the value, or
     * <code>null</code> if the parameter or its value isn't supported.
     */
    private static @Nullable String setting(@Nullable String parameterName, Expression value) {
        if (parameterName == null) {
            return null;
        }
        switch (parameterName) {
            case "http.connection.timeout":
                return isMillis(value) ? "connectTimeout(Duration.ofMillis(#{any(long)}))" : null;
            case "http.socket.timeout":
                return isMillis(value) ? "readTimeout(Duration.ofMillis(#{any(long)}))" : null;
            case "http.conn-manager.timeout":
                return isMillis(value) ? "connectionRequestTimeout(Duration.ofMillis(#{any(long)}))" : null;
            case "http.protocol.expect-continue":
                return isOfType(value, JavaType.Primitive.Boolean, "java.lang.Boolean") ? "expectContinue(#{any(boolean)})" : null;
            case "http.protocol.cookie-policy":
                Boolean ignoresCookies = ignoresCookies(value);
                return ignoresCookies == null ? null : "keepResponseCookies(" + !ignoresCookies + ")";
            default:
                return null;
        }
    }

    /**
     * Whether the value is an int or a long that isn't a negative literal, since the settings don't take a negative
     * <code>Duration</code>.
     */
    private static boolean isMillis(Expression value) {
        boolean negative = value instanceof J.Literal && ((J.Literal) value).getValue() instanceof Number &&
                ((Number) ((J.Literal) value).getValue()).longValue() < 0;
        return !negative && (isOfType(value, JavaType.Primitive.Int, "java.lang.Integer") || isOfType(value, JavaType.Primitive.Long, "java.lang.Long"));
    }

    private static boolean isOfType(Expression value, JavaType.Primitive primitive, String boxed) {
        return value.getType() == primitive || TypeUtils.isOfClassType(value.getType(), boxed);
    }

    /**
     * @return Whether the cookie policy ignores cookies, or <code>null</code> if it's neither "ignoreCookies" nor
     * "default", the two policies that <code>keepResponseCookies(..)</code> sets. Other policies, such as
     * "compatibility", have no replacement in 6.x, so they're left as they are.
     */
    private static @Nullable Boolean ignoresCookies(Expression policy) {
        String name;
        if (policy instanceof J.Literal && ((J.Literal) policy).getValue() instanceof String) {
            name = (String) ((J.Literal) policy).getValue();
        } else {
            String constant = RecipeSupport.fieldName(policy);
            name = constant == null ? null : COOKIE_POLICY_CONSTANTS.get(constant);
        }
        if ("ignoreCookies".equals(name)) {
            return true;
        }
        return "default".equals(name) ? false : null;
    }
}
