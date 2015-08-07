/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java.support;

import com.jayway.restassured.RestAssured;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Collections;

@Ignore("To make Maven happy")
public class WithJetty {
    public static final String itestPath;

    static {
        String fileSeparator = System.getProperty("file.separator");
        itestPath = fileSeparator + "examples" + fileSeparator + "rest-assured-itest-java";
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        String canonicalPath = new File(".").getCanonicalPath();
        String scalatraPath = "/examples/scalatra-webapp";

        // Security config
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);;
        constraint.setRoles(new String[]{"user", "admin", "moderator"});
        constraint.setAuthenticate(true);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec("/secured/*");


        final String realmPath = scalatraPath + "/etc/realm.properties";
        LoginService loginService = new HashLoginService("MyRealm",  isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + realmPath : canonicalPath + realmPath);
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
        String warPath = isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + scalatraWebAppPath : canonicalPath+scalatraWebAppPath;
        wac.setWar(warPath);
        wac.setServer(server);

        security.setHandler(wac);

        // Remove the sending of date header since it makes testing of logging much harder
        for(Connector y : server.getConnectors()) {
            y.getConnectionFactories().stream()
                    .filter(x -> x instanceof HttpConnectionFactory)
                    .map(x -> ((HttpConnectionFactory) x))
                    .map(HttpConnectionFactory::getHttpConfiguration)
                    .forEach(conf -> conf.setSendDateHeader(false));
        }

        server.setHandler(security);
        server.start();
    }

    @Before
    public void setUpBeforeTest() {
        RestAssured.reset();
    }

    private static File gotoProjectRoot() {
        return new File("../../.");
    }

    private static boolean isExecutedFromMaven(String canonicalPath) {
        return canonicalPath.contains(itestPath);
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
        server.join();
    }

    private static Server server;
}
