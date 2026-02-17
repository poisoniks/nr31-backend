package org.nr31.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Hello", description = "Hello World Controller")
public class HelloController {

    @GetMapping("/hello")
    @Operation(summary = "Get Hello World message")
    public String hello() {
        return "Hello World";
    }
}
