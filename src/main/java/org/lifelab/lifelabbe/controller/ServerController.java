package org.lifelab.lifelabbe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {

    @GetMapping("/")
    public String health() {
        return "Lifelab server running";
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }
}