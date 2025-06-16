package com.coda.roundrobin.simpleapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SimpleAPIController {

    @PostMapping(value="/reply")
    public ResponseEntity<Map<String,Object>>  replyPostPayload(@RequestBody Map<String,Object> postPayload){
        return (ResponseEntity.ok(postPayload));
    }

    @GetMapping(value="/reply/health")
    public ResponseEntity<Map<String,Object>>  replyHealth(){
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }
}
