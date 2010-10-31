package com.jayway.restassured.itest.support;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;

public class WithJetty {
    public static String itestPath = "/examples/rest-assured-itest";

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server();
        org.mortbay.jetty.Connector connector = new SelectChannelConnector();
        connector.setPort(8080);
        server.addConnector(connector);

        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        String canonicalPath = new File(".").getCanonicalPath();
        String scalatraPath = "/examples/scalatra-webapp";
        String webAppPath = "/src/main/webapp";
        String path = canonicalPath.contains(itestPath) ? new File("../../.").getCanonicalPath()+scalatraPath+webAppPath : canonicalPath+scalatraPath+webAppPath;
        System.out.println("canonicalPath = "+canonicalPath+", contains = "+canonicalPath.contains(itestPath)+", itestPath = "+itestPath+", path = "+path);
        wac.setWar(path);
        wac.setServer(server);

        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
    }

    private static Server server;
}
