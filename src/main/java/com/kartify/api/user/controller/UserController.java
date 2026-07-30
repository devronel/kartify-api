package com.kartify.api.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.security.CustomUserDetails;

@RestController
public class UserController {

    @GetMapping("/secured")
    public String hello(@AuthenticationPrincipal CustomUserDetails user){
        return "Welcome: " + user.getUser().getEmail();
    }

}
