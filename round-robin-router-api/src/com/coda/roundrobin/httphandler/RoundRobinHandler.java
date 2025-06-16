package com.coda.roundrobin.httphandler;

import com.coda.roundrobin.httphandler.circuitbreaker.CircuitBreaker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 * @author akanchan
 * RoundRobinHandler to route requests to /reply endpoints in a roundrobin fashion
 */
public class RoundRobinHandler implements HttpHandler {
    private final List<String> backends;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final Map<String, CircuitBreaker> circuitMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Removed ReentrantLock instance variable

    public RoundRobinHandler(List<String> backends) {
        this.backends = backends;
        if (backends == null || backends.isEmpty()) {
            throw new IllegalArgumentException("Backend URLs list cannot be null or empty.");
        }
        for (String backendUrl : backends) {
            circuitMap.put(backendUrl, new CircuitBreaker());
        }
        scheduler.scheduleAtFixedRate(this::healthCheckAll, 0, 10, TimeUnit.SECONDS);
    }

 /**
            * Handles incoming HTTP requests by forwarding them to a backend server selected
 * using a round-robin strategy.
            *
            * <p>This method performs the following steps:
            * <ol>
 *   <li>Validates that the request method is POST. If not, it sends a 405 Method Not Allowed error.</li>
            *   <li>Checks if any backend servers are configured. If not, it sends a 503 Service Unavailable error.</li>
            *   <li>Reads the request body.</li>
            *   <li>Selects an available backend server using a round-robin approach. It iterates through the
 *       configured backends, respecting the state of their associated {@link CircuitBreaker}.</li>
            *   <li>If no healthy backend is found, it sends a 503 Service Unavailable error.</li>
            *   <li>Forwards the request to the selected backend using the
 *       {@link #sendRequestsWithRetry(String, String, int, long)} method, which includes retry logic.</li>
            *   <li>If the forwarding is successful, it sends a 200 OK response with the backend's reply
            *       and records a success for the backend's circuit breaker.</li>
            *   <li>If forwarding fails (e.g., due to I/O errors or after all retries are exhausted),
 *       it records a failure for the backend's circuit breaker and sends a 502 Bad Gateway error.</li>
            *   <li>Any other unexpected exceptions during handling result in a 500 Internal Server Error.</li>
            * </ol>
            * The {@link HttpExchange} is always closed in a finally block to ensure resources are released.
            *
            * @param exchange the {@link HttpExchange} object representing the incoming request and
 *                 outgoing response.
            * @throws IOException if an I/O error occurs during request handling, such as when reading
 *                     the request body, writing the response body, or if an underlying
 *                     network operation within {@code sendErrorResponse} fails.
 *                     Note that I/O exceptions during backend communication in
 *                     {@code sendRequestsWithRetry} are caught and handled by sending
 *                     an appropriate HTTP error status to the client.
            */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            if (backends.isEmpty()) {
                sendErrorResponse(exchange, 503, "No backends configured.");
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String targetUrl = null;

            // Find an available backend
            int numberOfBackends = backends.size();
            int startIndex = Math.abs(counter.getAndIncrement()); // counter is thread-safe

            for (int i = 0; i < numberOfBackends; i++) {
                int currentIndex = (startIndex + i) % numberOfBackends;
                String candidateBackend = backends.get(currentIndex);
                CircuitBreaker cb = circuitMap.get(candidateBackend);
                // Ensure cb is not null, though constructor should initialize all
                if (cb != null && cb.isAvailable()) {
                    targetUrl = candidateBackend;
                    break;
                }
            }

            if (targetUrl == null) {
                sendErrorResponse(exchange, 503, "No healthy backends available.");
                return;
            }

            try {
                String response = sendRequestsWithRetry(targetUrl, requestBody, 3, 100);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                // Ensure circuitMap has the targetUrl before trying to get its CircuitBreaker
                if (circuitMap.containsKey(targetUrl)) {
                    circuitMap.get(targetUrl).recordSuccess();
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error forwarding request to " + targetUrl + ": " + e.getMessage());
                // Record failure for the specific backend
                if (circuitMap.containsKey(targetUrl)) {
                    circuitMap.get(targetUrl).recordFailure();
                }
                sendErrorResponse(exchange, 502, "Failed to communicate with backend: " + targetUrl + ". Error: " + e.getMessage());
            }
        } catch (Exception e) {
            // Catch any other unexpected errors
            System.err.println("Unexpected error in RoundRobinHandler: " + e.getMessage());
            e.printStackTrace();
            // Ideally, all paths are handled cleanly.
            if (exchange.getResponseCode() == -1) { // Check if response code is not set (headers not sent)
                sendErrorResponse(exchange, 500, "Internal Server Error.");
            }
        } finally {
            exchange.close(); // Ensure the exchange is always closed
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        // Check if headers have already been sent. HttpExchange doesn't offer a direct method,
        // so we rely on getResponseCode() == -1 as an indicator.
        if (exchange.getResponseCode() == -1) {
            byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } else {
            // Headers already sent, perhaps log this unexpected state
            System.err.println("Attempted to send error response, but headers were already sent. Status: " + exchange.getResponseCode());
        }
    }

    /**
     * @param targetUrl
     * @param requestBody
     * @param maxRetries
     * @param baseDelayMs
     * @return
     * @throws IOException
     * @throws InterruptedException
     * Method to attempt hitting the targeturl, incorporates retries in case of failure with exponential backoffs
     * This is to support graceful degradation of any backend, in case on of the backends start to go slowly
     */
    private String sendRequestsWithRetry(String targetUrl, String requestBody, int maxRetries, long baseDelayMs)
            throws IOException, InterruptedException {
        int attempt = 0;
        long delay = baseDelayMs;
        IOException lastException = null;

        while (attempt < maxRetries) {
            try {
                System.out.println("Request routed to: " + targetUrl + " (Attempt " + (attempt + 1) + ")" + " Thread:" + Thread.currentThread().getName());
                System.out.flush(); // Ensure log is written immediately for debugging
                HttpURLConnection conn = (HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // Set reasonable timeouts
                conn.setConnectTimeout(5000); // 5 seconds
                conn.setReadTimeout(10000);   // 10 seconds

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                InputStream responseStream = responseCode < 400 ?
                        conn.getInputStream() : conn.getErrorStream();

                try (InputStream is = responseStream) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                lastException = e;
                System.err.println("Attempt " + (attempt + 1) + " failed for " + targetUrl + ": " + e.getMessage());
                attempt++;
                if (attempt >= maxRetries) {
                    System.err.println("Max retries reached for " + targetUrl);
                    throw lastException; // Rethrow the last encountered exception
                }
                Thread.sleep(delay + new Random().nextInt(50)); // Add jitter
                delay *= 2; // Exponential backoff
            }
        }
        // Should not be reached if logic is correct, as the loop either returns or throws
        throw new IOException("Unreachable backend " + targetUrl + " after " + maxRetries + " retries.", lastException);
    }

    private void healthCheckAll() {
        for (String url : backends) {
            String healthUrl = url;
            // Assuming backend /reply endpoints also serve /health or similar
            // If not, this needs adjustment or configuration
            if (url.endsWith("/reply")) { // Basic attempt to form a health URL
                healthUrl = url.substring(0, url.length() - "/reply".length()) + "/health";
            } else if (!url.endsWith("/health")) {
                healthUrl = url + (url.endsWith("/") ? "health" : "/health");
            }

            try {
                // System.out.println("Health checking: " + healthUrl); // For debugging
                HttpURLConnection conn = (HttpURLConnection) URI.create(healthUrl).toURL().openConnection();
                conn.setRequestMethod("GET"); // Health checks are typically GET
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);

                if (conn.getResponseCode() == 200) {
                    circuitMap.get(url).recordSuccess();
                    // System.out.println("Health check SUCCESS for: " + url + " (via " + healthUrl + ")");
                } else {
                    circuitMap.get(url).recordFailure();
                    // System.err.println("Health check FAILED for: " + url + " (via " + healthUrl + ") - Status: " + conn.getResponseCode());
                }
                conn.disconnect();
            } catch (IOException e) {
                circuitMap.get(url).recordFailure();
                // System.err.println("Health check FAILED for: " + url + " (via " + healthUrl + ") - Error: " + e.getMessage());
            }
        }
    }
}

