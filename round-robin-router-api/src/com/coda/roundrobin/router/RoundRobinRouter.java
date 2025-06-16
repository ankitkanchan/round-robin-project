package com.coda.roundrobin.router;

import com.coda.roundrobin.httphandler.RoundRobinHandler;
import com.coda.roundrobin.utils.ConfigFileReader;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class RoundRobinRouter {

    public static void main(String[] args) throws IOException {
        List<String> backendUrls = ConfigFileReader.loadBackends("config.properties");
        HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
        server.createContext("/route", new RoundRobinHandler(backendUrls));
        // Use cachedThreadPool
        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println("RoundRobinRouter listening on http://localhost:8090/route");
        server.start();
    }

}
