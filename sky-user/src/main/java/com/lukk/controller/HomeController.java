package com.lukk.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HomeController {
    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${lukk.helloWorld}") String message) {
        // message is defined and obtained from application.properties file
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
