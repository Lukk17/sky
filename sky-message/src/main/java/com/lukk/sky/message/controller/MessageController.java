package com.lukk.sky.message.controller;

import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${lukk.helloWorld}") String message) {
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, @RequestHeader("username") String username) {
        message.setSenderEmail(username);
        messageService.send(message);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedMessages(@RequestHeader("username") String username) {
        return ResponseEntity.ok(messageService.getReceivedMessages(username));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentMessages(@RequestHeader("username") String username) {
        return ResponseEntity.ok(messageService.getSentMessages(username));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMessage(@RequestBody Long id) {
        messageService.remove(id);
        return ResponseEntity.accepted().build();
    }

}
