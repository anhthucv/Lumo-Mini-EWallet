package com.chethu.paymentledgerservice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
public class HelloController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "payment-ledger-service",
                "version", "0.0.1"
        );
    }

    @GetMapping("/hello") 
    public Map<String, String> hello(){
        return Map.of(
            "message", "Hello Java Backend"
        );
    }
    @GetMapping("/project-info")
    public Map<String,String> projectInfo(){
        return Map.of(
            "project", "Payment Ledger Service",
            "purpose", "Learn Java Backend",
            "day", "Day 1"
        );
    }
}
