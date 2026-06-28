package com.yinfeng.interview.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Profile("master")
public class DemoTargetController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "message", "pong");
    }

    @GetMapping("/resource")
    public Map<String, String> get() {
        return Map.of("method", "GET");
    }

    @PostMapping("/resource")
    public ResponseEntity<Map<String, String>> post(@RequestBody(required = false) String body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("method", "POST"));
    }

    @PutMapping("/resource")
    public Map<String, String> put(@RequestBody(required = false) String body) {
        return Map.of("method", "PUT");
    }

    @PatchMapping("/resource")
    public Map<String, String> patch(@RequestBody(required = false) String body) {
        return Map.of("method", "PATCH");
    }

    @DeleteMapping("/resource")
    public ResponseEntity<Void> delete() {
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/resource", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/resource", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok()
                .header("Allow", "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS")
                .build();
    }
}
