package com.lukk.sky.message.controller;

import com.lukk.sky.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${lukk.helloWorld}") String message) {
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

//    @PostMapping("/send")
//    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, Authentication auth) {
//        message.setSender(auth.getName());
//        messageService.send(message);
//        return ResponseEntity.accepted().build();
//    }

//    @GetMapping("/received")
//    public ResponseEntity<?> getReceivedMessages(Authentication auth) {
//        return ResponseEntity.ok(messageService.getReceivedMessages(auth.getName()));
//    }
//
//    @GetMapping("/sent")
//    public ResponseEntity<?> getSentMessages(Authentication auth) {
//        return ResponseEntity.ok(messageService.getSentMessages(auth.getName()));
//    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMessage(Long id) {
        messageService.remove(id);
        return ResponseEntity.accepted().build();
    }

}
