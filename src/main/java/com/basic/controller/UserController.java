package com.basic.controller;

import com.basic.entity.User;
import com.basic.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class UserController {

    @Autowired
    IUserService userService;

    @GetMapping("/registration")
    public ResponseEntity getRegister()
    {

        return new ResponseEntity("Give User", HttpStatus.OK);
    }

    @PutMapping("/registration")
    public ResponseEntity putRegister(@RequestBody User newUser)
    {
        userService.saveUser(newUser);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/userList")
    public ResponseEntity userList()
    {
        return ResponseEntity.ok(userService.findAll());
    }

}
