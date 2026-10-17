package com.nexushr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public Map<String, Object> testBackend() {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "NexusHR Backend is working successfully"
        );

        response.put(
                "application",
                "NexusHR"
        );

        response.put(
                "version",
                "1.0"
        );

        response.put(
                "port",
                8080
        );

        return response;
    }
}