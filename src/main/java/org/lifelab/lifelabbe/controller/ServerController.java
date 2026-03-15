package org.lifelab.lifelabbe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {

    @GetMapping("/")
    public String health() {
        System.out.println("=== HIT ROOT ==="); // ✅ 테스트
        return "Lifelab server running";
    }

    @GetMapping("/health")
    public String healthCheck() {
        System.out.println("=== HIT HEALTH ==="); // ✅ 테스트
        return "OK";
    }
}