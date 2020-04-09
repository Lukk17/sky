package com.lukk.sky.authservice.controller;

import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class UserController {

    final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO newUser) {
        try {
            userService.registerUser(newUser);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/userList")
    public ResponseEntity<List<UserDTO>> userList() {
        return ResponseEntity.ok(userService.findAllAndConvertToDTO());
    }

    @GetMapping("/userDetails")
    public ResponseEntity<?> userDetails(@RequestHeader("username") String username) {
        try {
            return ResponseEntity.ok(userService.findUserDetails(username));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
