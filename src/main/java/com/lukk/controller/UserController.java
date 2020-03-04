package com.lukk.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.entity.User;
import com.lukk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequiredArgsConstructor
public class UserController {

    final UserService userService;

    @GetMapping("/registration")
    public ResponseEntity<?> getRegister() {

        return new ResponseEntity<>("Give User", HttpStatus.OK);
    }

    @PutMapping("/registration")
    public ResponseEntity<?> putRegister(@RequestBody User newUser) {
        userService.saveUser(newUser);

        return ResponseEntity.accepted().build();
    }

    @RequestMapping(value = "/login", method = {RequestMethod.OPTIONS, RequestMethod.GET})
    public ResponseEntity<?> login() {
        // get logged user details from spring security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        User user = userService.findByUserEmail(userEmail);

        // build Json only from exposed in User entity fields (id and email)
        // instead sending back password and other not required by front-end fields
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        String userDetails = gson.toJson(user);

        return new ResponseEntity<>(userDetails, HttpStatus.OK);
    }

    @GetMapping("/userList")
    public ResponseEntity<?> userList() {

        return ResponseEntity.ok(userService.findAll());
    }

}
