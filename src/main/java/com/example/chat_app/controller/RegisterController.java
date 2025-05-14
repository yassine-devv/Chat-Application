package com.example.chat_app.controller;

import com.example.chat_app.entities.User;
import com.example.chat_app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(value = "/signup", consumes = "application/json")
    public ResponseEntity<String> confirmSignup(@RequestBody User user){
        Optional<User> userFoundedWithUsername = userService.findByUsername(user.getUsername());

        String html = """
                <turbo-stream action='update' target="signup-error">
                    <template>
                        Username non disponibile
                    </template>
                </turbo-stream>
                """;

        if(userFoundedWithUsername.isEmpty()){
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userService.save(user);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/login");

            return new ResponseEntity<String>(headers, HttpStatus.FOUND);
        }

        return ResponseEntity.ok().header("Content-Type", "text/vnd.turbo-stream.html").body(html);
    }
}
