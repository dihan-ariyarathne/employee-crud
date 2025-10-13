package com.acme.employee.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public Mono<UserInfo> me(@AuthenticationPrincipal(expression = "#this?.name") String uid) {
        if (uid == null) {
            return Mono.just(new UserInfo(null));
        }
        return Mono.just(new UserInfo(uid));
    }

    public record UserInfo(String uid) {}
}

