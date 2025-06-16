package com.coda.roundrobin.simpleapi.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleAPIController.class)
public class SimpleAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void replyPostPayload_returnsPostedPayload() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "value1");
        payload.put("key2", 123);

        mockMvc.perform(MockMvcRequestBuilders.post("/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(payload)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(asJsonString(payload)));
    }

    @Test
    void replyHealth_returnsOkStatus() throws Exception {
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "OK");

        mockMvc.perform(MockMvcRequestBuilders.get("/reply/health"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(asJsonString(expectedResponse)));
    }

    // Helper method to convert a Map to a JSON string
    private static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}