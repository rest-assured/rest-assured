package com.jayway.restassured.itest.support;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;

public class WithJetty {
    public static String itestPath = "/examples/rest-assured-itest";

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setPort(8080);
        server.addConnector(connector);

        String canonicalPath = new File(".").getCanonicalPath();
        String scalatraPath = "/examples/scalatra-webapp";

        // Security config
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);;
        constraint.setRoles(new String[]{"user","admin","moderator"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/secured/*");


        SecurityHandler sh = new SecurityHandler();
        final String realmPath = scalatraPath + "/etc/realm.properties";
        sh.setUserRealm(new HashUserRealm("MyRealm",isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + realmPath : canonicalPath+realmPath));
        sh.setConstraintMappings(new ConstraintMapping[]{cm});
        // End security config


        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        String webAppPath = "/src/main/webapp";
        final String scalatraWebAppPath = scalatraPath + webAppPath;
        String warPath = isExecutedFromMaven(canonicalPath) ? gotoProjectRoot().getCanonicalPath() + scalatraWebAppPath : canonicalPath+scalatraWebAppPath;
        wac.setWar(warPath);
        wac.setServer(server);

        server.setHandler(wac);
        server.addHandler(sh);
        server.setStopAtShutdown(true);
        server.start();
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
    }

    private static Server server;
}
