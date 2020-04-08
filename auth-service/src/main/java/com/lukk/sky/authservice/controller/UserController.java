package com.lukk.sky.authservice.controller;

import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class UserController {

    final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO newUser) {

        userService.saveUser(newUser);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/userList")
    public ResponseEntity<List<UserDTO>> userList() {
        return ResponseEntity.ok(userService.findAllAndConvertToDTO());
    }

    @GetMapping("/userDetails")
    public ResponseEntity<UserDTO> userDetails(@RequestHeader("username") String username) {
        return ResponseEntity.ok(userService.findUserDetails(username));
    }
}
