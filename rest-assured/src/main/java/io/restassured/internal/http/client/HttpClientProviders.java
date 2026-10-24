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

package io.restassured.internal.http.client;

import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.common.spi.http.HttpClientSpi;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.HttpClientType;
import io.restassured.internal.support.ModuleVersionCheck;
import io.restassured.internal.support.VersionFiles;
import io.restassured.internal.util.SafeExceptionRethrower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Picks the HTTP client that sends a request, in this order:
 * <ol>
 * <li>the client of {@link HttpClientConfig#client()}</li>
 * <li>the client named by the system property {@code restassured.http.client}, read for each request</li>
 * <li>http-client-apache5, if it's on the classpath</li>
 * <li>http-client-jdk, the default module, which rest-assured depends on</li>
 * </ol>
 * REST Assured's own client modules are found by class name, after checking that their version matches REST
 * Assured's. The clients of other libraries are found with {@link ServiceLoader}, only when one is named or for the
 * message that says which client is used, after checking which revision of the HTTP SPI they were built for.
 */
public final class HttpClientProviders {
    static final String SYSTEM_PROPERTY = "restassured.http.client";
    static final String SERVICES_FILE = "META-INF/services/" + HttpClientProvider.class.getName();
    static final String PROVIDER_FILES = "META-INF/rest-assured/http-client/";

    static final List<BuiltIn> BUILT_INS = List.of(
            new BuiltIn(HttpClientType.JDK, "io.restassured.http.jdk.JdkHttpClientProvider", "http-client-jdk"),
            new BuiltIn(HttpClientType.APACHE5, "io.restassured.http.apache5.Apache5HttpClientProvider", "http-client-apache5"));

    private static final HttpClientProviders INSTANCE = new HttpClientProviders(HttpClientProviders.class.getClassLoader(), BUILT_INS,
            System::getProperty, (level, message) -> System.getLogger("io.restassured").log(level, message));

    private final ClassLoader classLoader;
    private final List<BuiltIn> builtIns;
    private final UnaryOperator<String> systemProperties;
    private final BiConsumer<System.Logger.Level, String> log;
    // In OSGi, a client module can be installed later and found with DynamicImport-Package, so its absence isn't kept
    private final boolean osgi;
    private final Map<Choice, Selection> selections = new ConcurrentHashMap<>();
    // The choices of a class, kept with the class so that its class loader can be unloaded
    private final ClassValue<Selection> classSelections = new ClassValue<>() {
        @Override
        protected Selection computeValue(Class<?> providerClass) {
            return resolve(new Choice(How.CONFIG, providerClass));
        }
    };
    private final Map<String, Optional<HttpClientProvider>> builtInProviders = new ConcurrentHashMap<>();
    private final Set<String> loggedClients = ConcurrentHashMap.newKeySet();
    private volatile ThirdParties thirdParties;

    HttpClientProviders(ClassLoader classLoader, List<BuiltIn> builtIns, UnaryOperator<String> systemProperties,
                        BiConsumer<System.Logger.Level, String> log) {
        this(classLoader, builtIns, systemProperties, log, VersionFiles.isOsgiBundle(classLoader));
    }

    HttpClientProviders(ClassLoader classLoader, List<BuiltIn> builtIns, UnaryOperator<String> systemProperties,
                        BiConsumer<System.Logger.Level, String> log, boolean osgi) {
        this.classLoader = classLoader;
        this.builtIns = builtIns;
        this.systemProperties = systemProperties;
        this.log = log;
        this.osgi = osgi;
    }

    /**
     * @return The client that sends a request with this config. The choice is made once for each way of choosing, except
     * that in OSGi the classpath is searched again while http-client-apache5 is missing. The first time a client is
     * chosen, a message says which one it is.
     * @throws IllegalStateException If the chosen client isn't on the classpath, or its version doesn't match, or no
     *                               client module is on the classpath
     */
    public static Selection select(HttpClientConfig config) {
        return INSTANCE.selectFor(config);
    }

    Selection selectFor(HttpClientConfig config) {
        Choice choice;
        if (config.client() instanceof Class) {
            return classSelections.get((Class<?>) config.client());
        } else if (config.client() != null) {
            choice = new Choice(How.CONFIG, config.client());
        } else {
            String property = systemProperties.apply(SYSTEM_PROPERTY);
            choice = property == null || property.isBlank() ? new Choice(How.CLASSPATH, "") : new Choice(How.SYSTEM_PROPERTY, property.trim());
        }
        Selection selection = selections.get(choice);
        if (selection == null) {
            selection = resolve(choice);
            // The default, while http-client-apache5 may be installed later in OSGi
            if (selection.how() != How.DEFAULT || !osgi) {
                Selection other = selections.putIfAbsent(choice, selection);
                selection = other == null ? selection : other;
            }
        }
        return selection;
    }

    private Selection resolve(Choice choice) {
        Selection selection;
        if (choice.how() == How.CLASSPATH) {
            selection = load(builtInOf(HttpClientType.APACHE5)).map(provider -> new Selection(provider, How.CLASSPATH, null))
                    .or(() -> load(builtInOf(HttpClientType.JDK)).map(provider -> new Selection(provider, How.DEFAULT, null)))
                    .orElseThrow(() -> new IllegalStateException("REST Assured found no HTTP client module. Add io.rest-assured:"
                            + builtInOf(HttpClientType.JDK).artifactId() + " (no other dependencies) or io.rest-assured:"
                            + builtInOf(HttpClientType.APACHE5).artifactId() + " to your test dependencies."));
        } else {
            String chosenBy = chosenBy(choice);
            HttpClientProvider provider;
            if (choice.value() instanceof HttpClientType) {
                provider = loadChosen(chosenBy, builtInOf((HttpClientType) choice.value()));
            } else if (choice.value() instanceof Class) {
                provider = byClass((Class<?>) choice.value(), chosenBy);
            } else {
                provider = byName((String) choice.value(), chosenBy);
            }
            selection = new Selection(provider, choice.how(), chosenBy);
        }
        logOnce(selection);
        return selection;
    }

    private static String chosenBy(Choice choice) {
        Object value = choice.value();
        if (choice.how() == How.SYSTEM_PROPERTY) {
            return "the system property " + SYSTEM_PROPERTY + "=" + value;
        } else if (value instanceof Class) {
            return "httpClientConfig().client(" + ((Class<?>) value).getName() + ".class)";
        }
        return "httpClientConfig().client(" + (value instanceof String ? "\"" + value + "\"" : value) + ")";
    }

    private HttpClientProvider byName(String name, String chosenBy) {
        for (BuiltIn builtIn : builtIns) {
            if (builtIn.type().providerName().equalsIgnoreCase(name)) {
                return loadChosen(chosenBy, builtIn);
            }
        }
        ThirdParties found = thirdParties();
        for (ThirdParty thirdParty : found.providers()) {
            if (thirdParty.name().equalsIgnoreCase(name)) {
                return thirdParty.provider();
            }
        }
        String builtInNames = builtIns.stream().map(builtIn -> builtIn.type().providerName()).collect(Collectors.joining(" and "));
        String otherNames = found.providers().stream().map(ThirdParty::name).collect(Collectors.joining(", "));
        // A problem may be the named client's, whose name isn't known without loading it
        throw new IllegalStateException(startOfSentence(chosenBy) + " names no HTTP client on the classpath. REST Assured's clients are " + builtInNames
                + (otherNames.isEmpty() ? ", and no other client is on the classpath." : ", and the other clients on the classpath are " + otherNames + ".")
                + found.problems().stream().map(problem -> " " + problem).collect(Collectors.joining()));
    }

    private HttpClientProvider byClass(Class<?> providerClass, String chosenBy) {
        for (BuiltIn builtIn : builtIns) {
            if (builtIn.className().equals(providerClass.getName())) {
                return loadChosen(chosenBy, builtIn);
            }
        }
        String problem = osgi ? null : revisionProblem(providerClass.getClassLoader(), providerClass.getName());
        if (problem != null) {
            throw new IllegalStateException(problem);
        }
        return newInstance(providerClass);
    }

    private HttpClientProvider loadChosen(String chosenBy, BuiltIn builtIn) {
        return load(builtIn).orElseThrow(() -> new IllegalStateException(startOfSentence(chosenBy) + " needs io.rest-assured:" + builtIn.artifactId() + " on the classpath."));
    }

    private BuiltIn builtInOf(HttpClientType type) {
        return builtIns.stream().filter(builtIn -> builtIn.type() == type).findFirst().orElseThrow();
    }

    private Optional<HttpClientProvider> load(BuiltIn builtIn) {
        Optional<HttpClientProvider> provider = builtInProviders.get(builtIn.className());
        if (provider != null) {
            return provider;
        }
        // Before the class is loaded, so that a module of another version fails with a message
        ModuleVersionCheck.verify(classLoader, builtIn.artifactId(), VersionFiles.HTTP_SPI_REVISION);
        try {
            provider = Optional.of(newInstance(Class.forName(builtIn.className(), true, classLoader)));
        } catch (ClassNotFoundException e) {
            provider = Optional.empty();
        }
        if (provider.isEmpty() && osgi) {
            return provider;
        }
        Optional<HttpClientProvider> other = builtInProviders.putIfAbsent(builtIn.className(), provider);
        return other == null ? provider : other;
    }

    private static HttpClientProvider newInstance(Class<?> providerClass) {
        try {
            return (HttpClientProvider) providerClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            // Such as the message of a provider whose library is missing or too old
            return SafeExceptionRethrower.safeRethrow(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("REST Assured can't create the HTTP client provider " + providerClass.getName()
                    + ", which needs a public constructor without arguments.", e);
        }
    }

    private void logOnce(Selection selection) {
        HttpClientProvider provider = selection.provider();
        if (!loggedClients.add(provider.getClass().getName())) {
            return;
        }
        // The default needs no message in every test run
        if (selection.how() == How.DEFAULT) {
            log.accept(DEBUG, "REST Assured uses " + provider.displayName() + " from " + provider.artifact() + ".");
            return;
        }
        StringBuilder message = new StringBuilder("REST Assured uses the HTTP client from ").append(provider.artifact());
        if (provider.libraryDescription() != null) {
            message.append(" (").append(provider.libraryDescription()).append(")");
        }
        message.append(selection.how() == How.CLASSPATH ? ", found on the classpath." : ", chosen with " + selection.chosenBy() + ".");
        String notUsed;
        try {
            notUsed = thirdParties().providers().stream().filter(other -> other.provider().getClass() != provider.getClass())
                    .map(other -> other.name() + " (" + other.artifact() + ")").collect(Collectors.joining(", "));
        } catch (RuntimeException | LinkageError e) {
            // Only for the message, which mustn't fail the request
            notUsed = "";
        }
        if (!notUsed.isEmpty()) {
            message.append(" Also found, not used: ").append(notUsed).append(".");
        }
        if (selection.how() == How.CLASSPATH) {
            message.append(" To pin a client, use httpClientConfig().client(..) or the system property " + SYSTEM_PROPERTY + ".");
        }
        log.accept(INFO, message.toString());
    }

    private ThirdParties thirdParties() {
        ThirdParties found = thirdParties;
        if (found == null) {
            synchronized (this) {
                found = thirdParties;
                if (found == null) {
                    found = findThirdParties();
                    thirdParties = found;
                }
            }
        }
        return found;
    }

    private ThirdParties findThirdParties() {
        Set<String> builtInClasses = builtIns.stream().map(BuiltIn::className).collect(Collectors.toSet());
        // Checked before the classes are loaded. In OSGi, REST Assured can't read the resources of other bundles.
        Map<String, String> rejected = new LinkedHashMap<>();
        if (!osgi) {
            for (String className : serviceClassNames()) {
                String problem = builtInClasses.contains(className) ? null : revisionProblem(classLoader, className);
                if (problem != null) {
                    rejected.put(className, problem);
                }
            }
        }
        List<ThirdParty> providers = new ArrayList<>();
        List<String> problems = new ArrayList<>(rejected.values());
        // With the class loader of REST Assured, which SPI Fly needs in OSGi
        Iterator<ServiceLoader.Provider<HttpClientProvider>> found = ServiceLoader.load(HttpClientProvider.class, classLoader).stream().iterator();
        String previousError = null;
        while (true) {
            ThirdParty thirdParty;
            try {
                if (!found.hasNext()) {
                    break;
                }
                ServiceLoader.Provider<HttpClientProvider> next = found.next();
                previousError = null;
                if (builtInClasses.contains(next.type().getName()) || rejected.containsKey(next.type().getName())) {
                    continue;
                }
                HttpClientProvider provider = next.get();
                thirdParty = new ThirdParty(provider, provider.name(), provider.artifact());
            } catch (ServiceConfigurationError | RuntimeException e) {
                // After an error ServiceLoader goes on with the next provider, except when it can't list the services
                // files, which throws the same error again and again
                if (e.toString().equals(previousError)) {
                    break;
                }
                previousError = e.toString();
                problems.add(e instanceof ServiceConfigurationError ? e.getMessage() : "REST Assured can't load an HTTP client provider: " + e + ".");
                continue;
            } catch (LinkageError e) {
                // Such as a provider whose library isn't on the classpath, after which ServiceLoader goes on with the next
                problems.add("REST Assured can't load an HTTP client provider: " + e + ".");
                continue;
            }
            if (builtIns.stream().anyMatch(builtIn -> builtIn.type().providerName().equalsIgnoreCase(thirdParty.name()))) {
                log.accept(WARNING, thirdParty.provider().getClass().getName() + " (" + thirdParty.artifact() + ") names its HTTP client " + thirdParty.name()
                        + ", which is the name of one of REST Assured's clients, so REST Assured ignores it.");
            } else {
                providers.add(thirdParty);
            }
        }
        return new ThirdParties(List.copyOf(providers), List.copyOf(problems));
    }

    private Set<String> serviceClassNames() {
        Set<String> classNames = new LinkedHashSet<>();
        try {
            for (URL url : Collections.list(classLoader.getResources(SERVICES_FILE))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        int comment = line.indexOf('#');
                        String className = (comment < 0 ? line : line.substring(0, comment)).trim();
                        if (!className.isEmpty()) {
                            classNames.add(className);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // ServiceLoader reports the files it can't read
        }
        return classNames;
    }

    private static String revisionProblem(ClassLoader classLoader, String className) {
        String file = PROVIDER_FILES + className + ".properties";
        Properties properties = VersionFiles.load(classLoader, file);
        String revision = properties == null ? null : properties.getProperty(VersionFiles.HTTP_SPI_REVISION);
        if (revision == null) {
            return "The HTTP client provider " + className + " doesn't say which HTTP SPI revision it was built for, so REST Assured can't use it. "
                    + "Its jar needs " + file + " with the property " + VersionFiles.HTTP_SPI_REVISION + ".";
        }
        int builtFor;
        try {
            builtFor = Integer.parseInt(revision.trim());
        } catch (NumberFormatException e) {
            builtFor = -1;
        }
        if (builtFor == HttpClientSpi.REVISION) {
            return null;
        }
        String version = properties.getProperty(VersionFiles.VERSION);
        String artifact = properties.getProperty("artifact", "unknown artifact") + (version == null ? "" : " " + version);
        return className + " (" + artifact + ") was built for REST Assured's HTTP SPI revision " + revision.trim() + ", but this REST Assured has revision "
                + HttpClientSpi.REVISION + ". " + (builtFor < HttpClientSpi.REVISION ? "Use a newer version of it." : "Use a newer version of REST Assured, or an older version of it.");
    }

    // "the system property .." starts a sentence as "The system property ..", and "httpClientConfig().." as it is
    static String startOfSentence(String text) {
        return text.startsWith("the ") ? "The " + text.substring(4) : text;
    }

    /**
     * How a client was chosen
     */
    public enum How {
        /**
         * With {@link HttpClientConfig#client()}
         */
        CONFIG,
        /**
         * With the system property {@code restassured.http.client}
         */
        SYSTEM_PROPERTY,
        /**
         * Found on the classpath, without a choice
         */
        CLASSPATH,
        /**
         * The default module, http-client-jdk, used when nothing else is
         */
        DEFAULT
    }

    /**
     * The client that sends a request.
     *
     * @param provider The provider of the client
     * @param how      How it was chosen
     * @param chosenBy The choice in words, such as {@code httpClientConfig().client(JDK)}, or {@code null} if it wasn't chosen
     */
    public record Selection(HttpClientProvider provider, How how, String chosenBy) {
    }

    record BuiltIn(HttpClientType type, String className, String artifactId) {
    }

    private record Choice(How how, Object value) {
    }

    // The name and artifact are read once, when the provider is found
    private record ThirdParty(HttpClientProvider provider, String name, String artifact) {
    }

    private record ThirdParties(List<ThirdParty> providers, List<String> problems) {
    }
}
