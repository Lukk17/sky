package com.lukk.sky.message.adapters.api;

import com.google.gson.Gson;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.ports.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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

    @Operation(summary = "Hello World Page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Welcome",
                    content = {@Content(mediaType = "application/json")})
    })
    @GetMapping(value = {"/", "/home"})
    public ResponseEntity<String> hello(@Value("${sky.helloWorld}") String message) {
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @Operation(summary = "Send message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @PostMapping("/send")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody MessageDTO message,
                                         @RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);

        message.setSenderEmail(userEmail);
        message.setCreatedTime(LocalDateTime.now().format(DATE_TIME_FORMAT));
        log.info("Sending message from {} to {}", userEmail, message.getReceiverEmail());

        return ResponseEntity.ok(messageService.send(message));
    }

    @Operation(summary = "Get received messages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages pulled",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @GetMapping("/received")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> getReceivedMessages(@RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);
        log.info("Getting received messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getReceivedMessages(userEmail));
    }

    @Operation(summary = "Get sent messages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages pulled",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @GetMapping("/sent")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> getSentMessages(@RequestHeader Map<String, String> headers) {
        String userEmail = getUserInfoFromHeaders(headers);
        log.info("Getting sent messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getSentMessages(userEmail));
    }

    @Operation(summary = "Delete message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message deleted",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))}),
            @ApiResponse(responseCode = "402", description = "No user Info",
                    content = @Content)
    })
    @DeleteMapping("/delete/{messageId}")
    @CrossOrigin(origins = "${sky.crossOrigin.allowed}")
    public ResponseEntity<?> deleteMessage(@RequestHeader Map<String, String> headers, @PathVariable String messageId) {
        try {
            Gson gson = new Gson();
            String userEmail = getUserInfoFromHeaders(headers);

            log.info("Removing message with ID: {}", messageId);
            messageService.remove(Long.parseLong(messageId), userEmail);

            return ResponseEntity.ok(gson.toJson("Message removed."));

        } catch (MessageException exception) {
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
