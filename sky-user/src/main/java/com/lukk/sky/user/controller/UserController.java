package com.lukk.sky.user.controller;

import com.lukk.sky.common.dto.UserDTO;
import com.lukk.sky.common.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


//CrossOrigin allow CORS from Angular App running at the specified URL.
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
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
