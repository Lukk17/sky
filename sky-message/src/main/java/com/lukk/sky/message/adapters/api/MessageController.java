package com.lukk.sky.message.adapters.api;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.ports.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import static com.lukk.sky.message.config.Constants.DATE_TIME_FORMAT;
import static com.lukk.sky.message.config.Constants.USER_INFO_HEADERS;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}")
public class MessageController {

    private final MessageService messageService;

    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message) {
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, @RequestHeader Map<String, String> headers) {

        String user = getUserInfoFromHeaders(headers);

        message.setSenderEmail(user);
        message.setCreatedTime(LocalDateTime.now().format(DATE_TIME_FORMAT));

        return ResponseEntity.ok(messageService.send(message));
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedMessages(@RequestHeader Map<String, String> headers) {
        String user = getUserInfoFromHeaders(headers);
        log.info("Getting received messages for user: {}", user);

        return ResponseEntity.ok(messageService.getReceivedMessages(user));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentMessages(@RequestHeader Map<String, String> headers) {
        String user = getUserInfoFromHeaders(headers);
        log.info("Getting sent messages for user: {}", user);

        return ResponseEntity.ok(messageService.getSentMessages(user));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMessage(@RequestBody Long id, @RequestHeader Map<String, String> headers) {
        String user = getUserInfoFromHeaders(headers);

        messageService.remove(id, user);
        return ResponseEntity.ok("Message removed.");
    }

    private static String getUserInfoFromHeaders(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> USER_INFO_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new MessageException("No user for messages"));
    }

}
