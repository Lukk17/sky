package com.lukk.sky.message.adapters.api;

import com.lukk.sky.common.swagger.ApiCommonErrorResponses;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.ports.service.MessageService;
import com.lukk.sky.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@Tag(name = "Messages", description = "Messaging — send, retrieve, and delete user-to-user messages.")
@ApiCommonErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(path = "${sky.apiPrefix}", version = "1")
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Send message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class))})
    })
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody MessageDTO message) {
        String userEmail = SecurityUtils.currentUserEmail();

        message.setSenderEmail(userEmail);
        message.setCreatedTime(LocalDateTime.now().format(DATE_TIME_FORMAT));
        log.info("Sending message from {} to {}", userEmail, message.getReceiverEmail());

        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(messageService.send(message));
    }

    @Operation(summary = "Get received messages (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages pulled",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @GetMapping("/messages/received")
    public ResponseEntity<Page<MessageDTO>> getReceivedMessages(
            @PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting received messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getReceivedMessages(userEmail, pageable));
    }

    @Operation(summary = "Get sent messages (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages pulled",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @GetMapping("/messages/sent")
    public ResponseEntity<Page<MessageDTO>> getSentMessages(
            @PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting sent messages for user: {}", userEmail);

        return ResponseEntity.ok(messageService.getSentMessages(userEmail, pageable));
    }

    @Operation(summary = "Delete message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Message deleted",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Message not found",
                    content = @Content)
    })
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        String userEmail = SecurityUtils.currentUserEmail();

        log.info("Removing message with ID: {}", messageId);
        messageService.remove(Long.parseLong(messageId), userEmail);

        return ResponseEntity.noContent().build();
    }
}
