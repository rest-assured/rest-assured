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

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.test.RecipeSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Runs the recipes that change Maven and Gradle builds without network: the REST Assured artifacts they resolve come
 * from a Maven repository in target, and Gradle builds get the model that Gradle's tooling API would give them.
 */
final class BuildTestSupport {

    /**
     * The version of REST Assured that UpgradeToRestAssured7 upgrades to, which is the version of this module.
     */
    static final String VERSION = restAssuredVersion();

    private static final MavenRepository REPOSITORY = createRepository();

    private BuildTestSupport() {
    }

    static void offlineRepository(RecipeSpec spec) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new AssertionError("The recipe failed", t);
        });
        MavenExecutionContextView.view(ctx)
                .setLocalRepository(REPOSITORY)
                .setAddLocalRepository(true)
                .setAddCentralRepository(false);
        spec.executionContext(ctx);
    }

    /**
     * @param dependencies The <code>group:artifact:version</code> of the testImplementation dependencies.
     * @return The model of a Gradle build with the java plugin.
     */
    static GradleProject gradleProject(String... dependencies) {
        List<Dependency> requested = Arrays.stream(dependencies)
                .map(gav -> gav.split(":"))
                .map(gav -> Dependency.builder().gav(new GroupArtifactVersion(gav[0], gav[1], gav[2])).build())
                .collect(Collectors.toList());
        GradleDependencyConfiguration testImplementation = configuration("testImplementation", false, List.of(), requested);
        Map<String, GradleDependencyConfiguration> configurations = new LinkedHashMap<>();
        configurations.put("testImplementation", testImplementation);
        for (String classpath : List.of("testCompileClasspath", "testRuntimeClasspath")) {
            configurations.put(classpath, configuration(classpath, true, List.of(testImplementation), List.of()));
        }
        return GradleProject.builder()
                .id(Tree.randomId())
                .name("app")
                .path(":")
                .plugins(List.of())
                .mavenRepositories(List.of(REPOSITORY))
                .mavenPluginRepositories(List.of())
                .nameToConfiguration(configurations)
                .build();
    }

    private static GradleDependencyConfiguration configuration(String name, boolean resolvable,
                                                               List<GradleDependencyConfiguration> extendsFrom,
                                                               List<Dependency> requested) {
        return GradleDependencyConfiguration.builder()
                .name(name)
                .isTransitive(true)
                .isCanBeResolved(resolvable)
                .isCanBeDeclared(!resolvable)
                .extendsFrom(extendsFrom)
                .requested(requested)
                .directResolved(List.of())
                .constraints(List.of())
                .attributes(Map.of())
                .build();
    }

    /**
     * A Maven repository with the REST Assured artifacts that the tests use. The 6.3.0 rest-assured depends on json-path
     * and xml-path, and the current one has json-path-gpath and xml-path-gpath as optional dependencies.
     */
    private static MavenRepository createRepository() {
        Path repository = Path.of("target", "test-maven-repository").toAbsolutePath();
        try {
            artifact(repository, "rest-assured", "6.3.0", "json-path", "xml-path");
            for (String artifactId : List.of("json-path", "xml-path", "json-path-gpath", "xml-path-gpath")) {
                artifact(repository, artifactId, "6.3.0");
            }
            artifact(repository, "rest-assured", VERSION, "json-path-gpath", "xml-path-gpath");
            artifact(repository, "json-path-gpath", VERSION);
            artifact(repository, "xml-path-gpath", VERSION);
            for (String artifactId : List.of("rest-assured", "json-path", "xml-path", "json-path-gpath", "xml-path-gpath")) {
                metadata(repository, artifactId);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return MavenRepository.builder().id("test").uri(repository.toUri().toString()).knownToExist(true).build();
    }

    private static void artifact(Path repository, String artifactId, String version, String... dependencies) throws IOException {
        boolean optional = !version.equals("6.3.0");
        String dependencyTags = Arrays.stream(dependencies)
                .map(dependency -> "<dependency><groupId>io.rest-assured</groupId><artifactId>" + dependency + "</artifactId>" +
                                   "<version>" + version + "</version>" + (optional ? "<optional>true</optional>" : "") + "</dependency>")
                .collect(Collectors.joining());
        Path directory = Files.createDirectories(repository.resolve(Path.of("io", "rest-assured", artifactId, version)));
        Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"),
                "<project><modelVersion>4.0.0</modelVersion><groupId>io.rest-assured</groupId><artifactId>" + artifactId +
                "</artifactId><version>" + version + "</version><dependencies>" + dependencyTags + "</dependencies></project>");
        // OpenRewrite only takes an artifact from a local repository when its jar isn't empty
        Files.writeString(directory.resolve(artifactId + "-" + version + ".jar"), "jar");
    }

    private static void metadata(Path repository, String artifactId) throws IOException {
        Path directory = repository.resolve(Path.of("io", "rest-assured", artifactId));
        String versions;
        try (var versionDirectories = Files.list(directory)) {
            versions = versionDirectories.filter(Files::isDirectory)
                    .map(version -> "<version>" + version.getFileName() + "</version>")
                    .sorted()
                    .collect(Collectors.joining());
        }
        Files.writeString(directory.resolve("maven-metadata-local.xml"),
                "<metadata><groupId>io.rest-assured</groupId><artifactId>" + artifactId + "</artifactId>" +
                "<versioning><versions>" + versions + "</versions></versioning></metadata>");
    }

    private static String restAssuredVersion() {
        try (InputStream properties = BuildTestSupport.class.getResourceAsStream("/META-INF/rest-assured/rest-assured.properties")) {
            Properties restAssured = new Properties();
            restAssured.load(properties);
            return restAssured.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
