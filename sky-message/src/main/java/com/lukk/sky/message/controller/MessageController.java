package com.lukk.sky.message.controller;

import com.lukk.sky.message.dto.EntityDTOConverter;
import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message) {
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, @RequestHeader("username") String username) {
        message.setSenderEmail(username);
        message.setCreatedTime(LocalDateTime.now());

        return ResponseEntity.ok(messageService.send(message));
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
    public ResponseEntity<?> deleteMessage(@RequestBody Long id, @RequestHeader("username") String username) {
        messageService.remove(id, username);
        return ResponseEntity.accepted().build();
    }

}
