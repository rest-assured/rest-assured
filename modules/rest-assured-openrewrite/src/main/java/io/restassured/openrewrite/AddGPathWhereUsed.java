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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.AddDependency;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenTagInsertionComparator;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Adds json-path-gpath, and xml-path-gpath, to the Maven and Gradle builds whose code uses paths, since REST Assured 7
 * no longer brings a path engine.
 */
public class AddGPathWhereUsed extends ScanningRecipe<Map<JavaProject, Set<String>>> {

    private static final String GROUP_ID = "io.rest-assured";
    private static final String JSON_PATH_GPATH = "json-path-gpath";
    private static final String XML_PATH_GPATH = "xml-path-gpath";
    private static final String REST_ASSURED_ALL = "rest-assured-all";

    /**
     * The methods that evaluate paths, on the core types and so on the MockMvc and WebTestClient types that extend
     * them. A path can be evaluated on a JSON or an XML body, which can't be told apart here, so they need
     * json-path-gpath.
     */
    private static final List<MethodMatcher> PATH_METHODS = Stream.of(
                    "io.restassured.response.ValidatableResponseOptions body(String, ..)",
                    "io.restassured.response.ValidatableResponseOptions body(java.util.List, ..)",
                    "io.restassured.response.ValidatableResponseOptions rootPath(..)",
                    "io.restassured.response.ValidatableResponseOptions root(..)",
                    "io.restassured.response.ValidatableResponseOptions appendRootPath(..)",
                    "io.restassured.response.ValidatableResponseOptions appendRoot(..)",
                    "io.restassured.specification.ResponseSpecification body(String, ..)",
                    "io.restassured.specification.ResponseSpecification body(java.util.List, ..)",
                    "io.restassured.specification.ResponseSpecification rootPath(..)",
                    "io.restassured.specification.ResponseSpecification root(..)",
                    "io.restassured.specification.ResponseSpecification appendRootPath(..)",
                    "io.restassured.specification.ResponseSpecification appendRoot(..)",
                    "io.restassured.builder.ResponseSpecBuilder expectBody(String, ..)",
                    "io.restassured.builder.ResponseSpecBuilder rootPath(..)",
                    "io.restassured.builder.ResponseSpecBuilder appendRootPath(..)",
                    "io.restassured.response.ResponseBodyExtractionOptions path(..)",
                    "io.restassured.response.ResponseBodyExtractionOptions jsonPath(..)")
            .map(pattern -> new MethodMatcher(pattern, true))
            .toList();
    private static final List<MethodMatcher> XML_PATH_METHODS = Stream.of(
                    "io.restassured.response.ResponseBodyExtractionOptions xmlPath(..)",
                    "io.restassured.response.ResponseBodyExtractionOptions htmlPath(..)")
            .map(pattern -> new MethodMatcher(pattern, true))
            .toList();

    @Option(displayName = "Version",
            description = "The version of json-path-gpath and xml-path-gpath, when it can't be taken from the REST Assured dependency of the build.",
            example = "7.0.0")
    private final String version;

    @JsonCreator
    public AddGPathWhereUsed(@JsonProperty("version") String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getDisplayName() {
        return "Add json-path-gpath and xml-path-gpath where paths are used";
    }

    @Override
    public String getDescription() {
        return "Add `io.rest-assured:json-path-gpath` to the Maven and Gradle builds whose code evaluates paths, such as " +
                "`body(\"a.b\", ..)`, `path(..)` or `jsonPath()`, and `io.rest-assured:xml-path-gpath` to those whose " +
                "code calls `xmlPath()` or `htmlPath()` or uses `XmlPath`, since REST Assured 7 no longer includes a path " +
                "engine. They get the scope (or Gradle configuration) of the REST Assured dependency, and in Maven builds " +
                "its version. Paths " +
                "on XML bodies look like JSON paths, so add xml-path-gpath yourself if you use them.";
    }

    @Override
    public Map<JavaProject, Set<String>> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<JavaProject, Set<String>> engines) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile source = (JavaSourceFile) tree;
                    source.getMarkers().findFirst(JavaProject.class).ifPresent(project ->
                            engines.computeIfAbsent(project, p -> new TreeSet<>()).addAll(enginesUsedBy(source, ctx)));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<JavaProject, Set<String>> engines) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                Set<String> artifactIds = ((SourceFile) tree).getMarkers().findFirst(JavaProject.class)
                        .map(engines::get).orElse(Set.of());
                Tree result = tree;
                for (String artifactId : artifactIds) {
                    if (result.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                        result = new AddToPom(artifactId).visitNonNull(result, ctx);
                    } else {
                        Optional<GradleProject> gradleProject = result.getMarkers().findFirst(GradleProject.class);
                        if (gradleProject.isPresent()) {
                            result = addToGradleBuild(result, gradleProject.get(), artifactId, ctx);
                        }
                    }
                }
                return result;
            }
        };
    }

    private static Set<String> enginesUsedBy(JavaSourceFile source, ExecutionContext ctx) {
        Set<String> artifactIds = new TreeSet<>();
        if (new UsesType<>("io.restassured.path.json.JsonPath", false).visit(source, ctx) != source) {
            artifactIds.add(JSON_PATH_GPATH);
        }
        if (new UsesType<>("io.restassured.path.xml.XmlPath", false).visit(source, ctx) != source) {
            artifactIds.add(XML_PATH_GPATH);
        }
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> found) {
                if (PATH_METHODS.stream().anyMatch(matcher -> matcher.matches(method))) {
                    found.add(JSON_PATH_GPATH);
                } else if (XML_PATH_METHODS.stream().anyMatch(matcher -> matcher.matches(method))) {
                    found.add(XML_PATH_GPATH);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.visit(source, artifactIds);
        return artifactIds;
    }

    private static boolean providesEngine(@Nullable String groupId, String artifactId, String engine) {
        return GROUP_ID.equals(groupId) && (engine.equals(artifactId) || REST_ASSURED_ALL.equals(artifactId));
    }

    private class AddToPom extends MavenIsoVisitor<ExecutionContext> {
        private final String artifactId;

        AddToPom(String artifactId) {
            this.artifactId = artifactId;
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            MavenResolutionResult model = getResolutionResult();
            boolean present = model.getPom().getRequestedDependencies().stream()
                                      .anyMatch(d -> providesEngine(d.getGroupId(), d.getArtifactId(), artifactId)) ||
                              model.getDependencies().values().stream().flatMap(List::stream)
                                      .anyMatch(d -> providesEngine(d.getGroupId(), d.getArtifactId(), artifactId));
            if (present) {
                return document;
            }
            Xml.Tag root = document.getRoot();
            Optional<Xml.Tag> dependencies = root.getChild("dependencies");
            // The REST Assured dependency, whose version (often ${rest-assured.version}) and scope the path engine gets
            Optional<Xml.Tag> restAssured = dependencies.flatMap(tag -> tag.getChildren("dependency").stream()
                    .filter(dependency -> GROUP_ID.equals(dependency.getChildValue("groupId").orElse(null)))
                    .findFirst());
            String dependencyVersion = restAssured.flatMap(tag -> tag.getChildValue("version")).orElse(null);
            if (dependencyVersion == null && model.getPom().getManagedVersion(GROUP_ID, artifactId, null, null) == null) {
                dependencyVersion = version;
            }
            String scope = restAssured.isPresent() ? restAssured.get().getChildValue("scope").orElse(null) : "test";
            String dependency = "<dependency>\n" +
                                "<groupId>" + GROUP_ID + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                (dependencyVersion == null ? "" : "<version>" + dependencyVersion + "</version>\n") +
                                (scope == null ? "" : "<scope>" + scope + "</scope>\n") +
                                "</dependency>";
            if (dependencies.isPresent()) {
                doAfterVisit(new AddToTagVisitor<>(dependencies.get(), Xml.Tag.build(dependency)));
            } else {
                doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<dependencies>\n" + dependency + "\n</dependencies>"),
                        new MavenTagInsertionComparator(root.getChildren())));
            }
            maybeUpdateModel();
            return document;
        }
    }

    /**
     * Adds the path engine to the Gradle configuration that has a REST Assured dependency, such as testImplementation.
     */
    private Tree addToGradleBuild(Tree build, GradleProject project, String artifactId, ExecutionContext ctx) {
        List<GradleDependencyConfiguration> configurations = project.getConfigurations();
        boolean present = configurations.stream().anyMatch(configuration ->
                configuration.getRequested().stream().anyMatch(d -> providesEngine(d.getGroupId(), d.getArtifactId(), artifactId)) ||
                configuration.getResolved().stream().anyMatch(d -> providesEngine(d.getGroupId(), d.getArtifactId(), artifactId)));
        if (present) {
            return build;
        }
        String configuration = configurations.stream()
                .filter(c -> c.getRequested().stream().anyMatch(d -> GROUP_ID.equals(d.getGroupId())))
                .map(GradleDependencyConfiguration::getName)
                .findFirst()
                .orElse("testImplementation");
        AddDependency addDependency = new AddDependency(GROUP_ID, artifactId, version, null, configuration, null, null, null, null, null);
        return addDependency.getVisitor(addDependency.getInitialValue(ctx)).visitNonNull(build, ctx);
    }
}
