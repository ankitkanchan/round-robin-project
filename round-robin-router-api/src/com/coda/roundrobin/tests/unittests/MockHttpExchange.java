package com.coda.roundrobin.tests.unittests;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;

public class MockHttpExchange extends HttpExchange {
    private final String method;
    private final String requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private int statusCode=-1;

    public MockHttpExchange(String method, String requestBody) {
        this.method = method;
        this.requestBody = requestBody;
    }

    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
        return URI.create("/");
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public InputStream getRequestBody() {
        return new ByteArrayInputStream(requestBody.getBytes());
    }

    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        this.statusCode = rCode;
    }

    @Override
    public void close() {}

    public int getStatusCode() {
        return statusCode;
    }

    // Stub other methods as needed
    @Override public InetSocketAddress getRemoteAddress() { return null; }

    /**
     * @return
     */
    @Override
    public int getResponseCode() {
        return -1;
    }

    @Override public InetSocketAddress getLocalAddress() { return null; }
    @Override public String getProtocol() { return null; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public void setAttribute(String name, Object value) {}
    @Override public void setStreams(InputStream i, OutputStream o) {}

    /**
     * @return
     */
    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }

    @Override public HttpContext getHttpContext() { return null; }
}
