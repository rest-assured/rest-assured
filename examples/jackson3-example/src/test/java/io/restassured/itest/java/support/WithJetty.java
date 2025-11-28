/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.itest.java.support;

import io.restassured.RestAssured;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;

import java.io.File;
import java.util.Collections;

import static io.restassured.itest.java.support.WithJetty.JettyOption.RESET_REST_ASSURED_BEFORE_TEST;

@DisplayNameGeneration(ReplaceUnderscores.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class WithJetty {
    public static final String itestPath;

    static {
        String fileSeparator = System.getProperty("file.separator");
        itestPath = fileSeparator + "examples" + fileSeparator + "jackson3-example";
    }

    private final JettyOption jettyOption;

    protected WithJetty() {
        this(RESET_REST_ASSURED_BEFORE_TEST);
    }

    protected WithJetty(JettyOption jettyOption) {
        this.jettyOption = jettyOption;
    }

    @BeforeAll
    public void startJetty() throws Exception {
        startJettyOneWaySSL();
    }

    protected static void startJettyOneWaySSL() throws Exception {
        server = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        http.setPort(8080);
        http.setIdleTimeout(30000);

        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        String file = WithJetty.class.getClassLoader().getResource("jetty_localhost_server.jks").getFile();

        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePassword("test1234");
        sslContextFactory.setKeyStorePath(file);

        String canonicalPath = new File(".").getCanonicalPath();
        String scalatraPath = "/examples/scalatra-webapp";

        // Security config
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);

        constraint.setRoles(new String[]{"user", "admin", "moderator"});
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/secured/*");


        final String realmPath = scalatraPath + "/etc/realm.properties";
        LoginService loginService = new HashLoginService("MyRealm", isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + realmPath : canonicalPath + realmPath);
        server.addBean(loginService);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);

        // End security config

        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        String webAppPath = "/src/main/webapp";
        final String scalatraWebAppPath = scalatraPath + webAppPath;
        String warPath = isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + scalatraWebAppPath : canonicalPath + scalatraWebAppPath;
        wac.setWar(warPath);
        wac.setServer(server);

        security.setHandler(wac);
        server.setHandler(security);
        server.setConnectors(new Connector[]{http});
        server.start();
    }


    @BeforeEach
    public void setUpBeforeTest() {
        if (jettyOption == RESET_REST_ASSURED_BEFORE_TEST) {
            RestAssured.reset();
        }
    }

    private static File gotoProjectRoot() {
        return new File("../../.");
    }

    private static boolean isExecutedFromMaven(String canonicalPath) {
        return canonicalPath.contains(itestPath);
    }

    @AfterAll
    public void stopJetty() throws Exception {
        server.stop();
        server.join();
    }

    private static Server server;

    public enum JettyOption {
        RESET_REST_ASSURED_BEFORE_TEST,
        DONT_RESET_REST_ASSURED_BEFORE_TEST
    }
}
