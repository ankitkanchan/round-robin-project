package com.coda.roundrobin.tests.loadtests;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class LoadTester {
    private static final int TOTAL_REQUESTS = 1000;
    private static final int CONCURRENCY = 1;
    private static final String TARGET_URL = "http://localhost:8090/route";

    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws InterruptedException {
        long startTime;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY)) {

            startTime = System.currentTimeMillis();

            CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

            for (int i = 0; i < TOTAL_REQUESTS; i++) {
                executor.submit(() -> {
                    long start = System.nanoTime();
                    try {
                        sendRequest();
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        long end = System.nanoTime();
                        latencies.add(TimeUnit.NANOSECONDS.toMillis(end - start));
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("------ Load Test Results ------");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total Time: " + duration + " ms");
        System.out.println("Requests/sec: " + (1000.0 * TOTAL_REQUESTS / duration));
        System.out.println("Average latency: " + average(latencies) + " ms");
        System.out.println("95th percentile latency: " + percentile(latencies, 95) + " ms");
    }

    private static void sendRequest() throws IOException {
        HttpURLConnection connection = (HttpURLConnection)  URI.create(TARGET_URL).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String payload = "{\"game\":\"Mobile Legends\",\"gamerID\":\"GYUTDTE\",\"points\":20}";

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes());
        }

        try (InputStream is = connection.getResponseCode() < 400 ?
                connection.getInputStream() : connection.getErrorStream()) {
            new String(is.readAllBytes()); // discard content
        }
    }

    private static double average(List<Long> values) {
        return values.stream().mapToLong(l -> l).average().orElse(0);
    }

    private static long percentile(List<Long> values, int percent) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percent / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(index, 0));
    }
}
