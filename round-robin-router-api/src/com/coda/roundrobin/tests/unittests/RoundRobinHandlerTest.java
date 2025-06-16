package com.coda.roundrobin.tests.unittests;

import com.coda.roundrobin.httphandler.RoundRobinHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoundRobinHandlerTest {


    private RoundRobinHandler handler;

    @BeforeEach
    public void setup() {
        handler = new RoundRobinHandler(List.of("http://localhost:8081/reply"));
    }

    @Test
    public void testRejectsNonPostRequest() throws Exception {
        MockHttpExchange exchange = new MockHttpExchange("GET", "");
        handler.handle(exchange);
        assertEquals(405, exchange.getStatusCode(), "Should reject non-POST with 405");
    }

    @Test
    public void testFailsWhenNoHealthyBackend() throws Exception {
        RoundRobinHandler failingHandler = new RoundRobinHandler(List.of("http://unreachable:9999"));
        MockHttpExchange exchange = new MockHttpExchange("POST", "{\"msg\":\"test\"}");

        failingHandler.handle(exchange);
        int status = exchange.getStatusCode();
        assertTrue(status == 502 || status == 503, "Should return 502 or 503 when all backends are unavailable");
    }

}
