package com.bachelorprojekt_backend.controller;

import com.bachelorprojekt_backend.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    UserService userService;

     public UserController(UserService userService) {
        this.userService = userService;
    }

}
