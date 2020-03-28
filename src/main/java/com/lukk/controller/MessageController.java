package com.lukk.controller;

import com.lukk.dto.MessageDTO;
import com.lukk.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, Authentication auth) {
        message.setSender(auth.getName());
        messageService.send(message);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedMessages(Authentication auth) {
        return ResponseEntity.ok(messageService.getReceivedMessages(auth.getName()));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentMessages(Authentication auth) {
        return ResponseEntity.ok(messageService.getSentMessages(auth.getName()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMessage(Long id) {
        messageService.remove(id);
        return ResponseEntity.accepted().build();
    }

}
