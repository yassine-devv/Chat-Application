package com.example.chat_app.controller;

import com.example.chat_app.entities.User;
import com.example.chat_app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(value = "/signup", consumes = "application/json")
    public User confirmSignup(@RequestBody User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userService.save(user);
    }
}
