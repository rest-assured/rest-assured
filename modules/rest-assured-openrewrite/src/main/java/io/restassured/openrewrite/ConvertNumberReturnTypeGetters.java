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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class ConvertNumberReturnTypeGetters extends Recipe {

    private static final String NUMBER_RETURN_TYPE = "io.restassured.common.config.NumberReturnType";
    private static final MethodMatcher JSON_PATH_CONFIG_GETTER = new MethodMatcher("io.restassured.path.json.config.JsonPathConfig numberReturnType()");
    private static final MethodMatcher JSON_CONFIG_GETTER = new MethodMatcher("io.restassured.config.JsonConfig numberReturnType()");

    @Override
    public String getDisplayName() {
        return "Convert the result of `numberReturnType()` to the `NumberReturnType` in rest-assured-common";
    }

    @Override
    public String getDescription() {
        return "`JsonPathConfig.numberReturnType()` and `JsonConfig.numberReturnType()` still return the deprecated " +
                "`JsonPathConfig.NumberReturnType`, so in code that imports `io.restassured.common.config.NumberReturnType` " +
                "wrap them in `NumberReturnType.valueOf(..name())`, so that the result can be compared with and assigned " +
                "to the new type. Calls such as `numberReturnType().isFloatOrDouble()`, which both types have, and a " +
                "switch on the result are left as they are.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new UsesMethod<>(JSON_PATH_CONFIG_GETTER), new UsesMethod<>(JSON_CONFIG_GETTER)), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // The getters only need converting where the code refers to the new type, which it imports
                boolean importsNumberReturnType = cu.getImports().stream().anyMatch(i -> NUMBER_RETURN_TYPE.equals(i.getTypeName()));
                return importsNumberReturnType ? super.visitCompilationUnit(cu, ctx) : cu;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!(JSON_PATH_CONFIG_GETTER.matches(m) || JSON_CONFIG_GETTER.matches(m)) || worksWithEitherType(getCursor())) {
                    return m;
                }
                // The code may only import constants of NumberReturnType statically
                maybeAddImport(NUMBER_RETURN_TYPE);
                return JavaTemplate.builder("NumberReturnType.valueOf(#{any(java.lang.Enum)}.name())")
                        .imports(NUMBER_RETURN_TYPE)
                        .javaParser(RecipeSupport.parser())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m);
            }
        });
    }

    /**
     * Whether the getter is the target of a method that both enums have (except <code>equals</code>, which would
     * compare enums of different types) or the selector of a switch.
     */
    private static boolean worksWithEitherType(Cursor getter) {
        Cursor parent = getter.getParentTreeCursor();
        if (parent.getValue() instanceof J.MethodInvocation) {
            J.MethodInvocation invocation = parent.getValue();
            return invocation.getSelect() == getter.getValue() && !"equals".equals(invocation.getSimpleName());
        }
        return parent.getValue() instanceof J.ControlParentheses &&
                (parent.getParentTreeCursor().getValue() instanceof J.Switch || parent.getParentTreeCursor().getValue() instanceof J.SwitchExpression);
    }
}
