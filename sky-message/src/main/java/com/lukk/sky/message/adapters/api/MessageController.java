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

        String userEmail = getUserInfoFromHeaders(headers);

        message.setSenderEmail(userEmail);
        message.setCreatedTime(LocalDateTime.now().format(DATE_TIME_FORMAT));
        log.info("Sending message from {} to {}", userEmail, message.getReceiverEmail());

        return ResponseEntity.ok(messageService.send(message));
    }

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedMessages(@RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);
        log.info("Getting received messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getReceivedMessages(userEmail));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentMessages(@RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);
        log.info("Getting sent messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getSentMessages(userEmail));
    }

    @DeleteMapping("/delete/{messageId}")
    public ResponseEntity<?> deleteMessage(@RequestHeader Map<String, String> headers, @PathVariable String messageId) {
        String userEmail = getUserInfoFromHeaders(headers);

        try {
            log.info("Removing message with ID: {}", messageId);
            messageService.remove(Long.parseLong(messageId), userEmail);

            return ResponseEntity.ok("Message removed.");

        } catch (MessageException exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
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
