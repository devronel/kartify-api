package com.kartify.api.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/secured")
    public String hello(){
        return "Hello, this is secured";
    }

}
