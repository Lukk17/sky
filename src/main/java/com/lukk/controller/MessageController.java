package com.lukk.controller;

import com.lukk.dto.MessageDTO;
import com.lukk.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MessageController {

    final MessageService messageService;

    @PutMapping("/sendMessage")
    public ResponseEntity<?> putRegister(@RequestBody MessageDTO message, Authentication auth) {
        message.setSender(auth.getName());
        messageService.send(message);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/receivedMessages")
    public ResponseEntity<?> getReceivedMessages(Authentication auth) {
        return ResponseEntity.ok(messageService.getReceivedMessages(auth.getName()));
    }

    @GetMapping("/sentMessages")
    public ResponseEntity<?> getSentMessages(Authentication auth) {
        return ResponseEntity.ok(messageService.getSentMessages(auth.getName()));
    }

    @DeleteMapping("/deleteMessage")
    public ResponseEntity<?> deleteMessage(Long id) {
        messageService.remove(id);
        return ResponseEntity.accepted().build();
    }

}
